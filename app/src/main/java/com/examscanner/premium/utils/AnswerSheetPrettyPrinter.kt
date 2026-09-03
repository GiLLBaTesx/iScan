package com.examscanner.premium.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.examscanner.premium.data.ExamEntity
import com.examscanner.premium.data.TemplateEntity
import com.examscanner.premium.qrcode.QRCodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * AnswerSheetPrettyPrinter - Generates A4 printable answer-sheet PDFs.
 *
 * Features:
 * - QR code rendered into a fixed header slot (top-right by default, respecting
 *   [TemplateEntity.qrCodePosition] when a custom template is supplied)
 * - Student info fields (name / ID / section / date)
 * - Instruction text ("Shade circles completely. Use black or blue pen.")
 * - A bubble grid with options A-G (driven by [SheetConfig.optionsLabels]); bubbles are
 *   at least 8mm in diameter with at least 5mm of spacing between them
 * - Incremental page rendering: rows that overflow the current page start a new page
 *
 * PDF generation uses Android's built-in [android.graphics.pdf.PdfDocument] + [Canvas],
 * matching the existing [TemplatePDFGenerator] conventions. iText7 is intentionally NOT
 * used because it is not a project dependency.
 *
 * Testability seam: [buildSheetModel] is a **pure function** that returns a [SheetModel]
 * describing every question, its option labels, and the exact bubble coordinates/labels —
 * WITHOUT performing any PDF I/O. The round-trip property tests (5.8/5.9) use this to
 * simulate fill + scan deterministically without a device or PDF.
 *
 * Because that pure seam touches neither the [qrGenerator] nor the [context], both
 * constructor collaborators are nullable so JVM unit tests can build an instance and
 * call [buildSheetModel] without an Android [Context]. Production always injects both;
 * [generateAnswerSheet] (the PDF path) requires them.
 *
 * Requirements: 3.5, 3.6, 4.2, 4.6, 19.5, 21.1, 21.2, 21.3, 21.4, 21.5, 21.6.
 */
class AnswerSheetPrettyPrinter(
    private val qrGenerator: QRCodeGenerator? = null,
    private val context: Context? = null
) {

    /**
     * A printable page size in PDF points at 72dpi (top-left origin, y-down).
     *
     * Selection order for [buildSheetModel] is [A4] → [LEGAL] → [LETTER]: the first size on
     * which the exam's desired column layout fits horizontally without bubble overlap wins.
     * A4 and Legal share the taller/narrower A4 width story vs. the wider US sizes, so higher
     * option counts (A–F/A–G) that would force columns below the desired count on A4 escalate
     * to the wider 612pt sizes.
     */
    enum class PageSize(val widthPt: Float, val heightPt: Float) {
        A4(595f, 842f),
        LEGAL(612f, 1008f),
        LETTER(612f, 792f);
    }

    /**
     * Render-time configuration for a single answer sheet. This is a DTO, not a schema change.
     *
     * @param examName human-readable exam name shown in the header.
     * @param totalQuestions number of questions to render.
     * @param optionsCount number of answer options per question (A..G supported: 2..7).
     * @param optionsLabels the labels to draw for each option; length should equal [optionsCount].
     * @param includeNameField whether to render the student NAME field.
     * @param includeIdField whether to render the STUDENT ID field.
     * @param includeDateField whether to render the DATE field.
     * @param includeClassField whether to render the SECTION field.
     * @param bubbleSize bubble diameter in millimetres (minimum enforced at 8mm).
     * @param bubbleSpacing gap between adjacent bubbles in millimetres (minimum enforced at 5mm).
     * @param columns DESIRED (max) number of question columns placed side-by-side. Treated as an
     *   upper-bound hint: the effective column count is derived from how many required-width
     *   columns fit on the selected page, capped at [MAX_COLUMNS_PER_PAGE] and by this value.
     */
    data class SheetConfig(
        val examName: String,
        val totalQuestions: Int,
        val optionsCount: Int = 4,
        val optionsLabels: List<String> = listOf("A", "B", "C", "D"),
        val includeNameField: Boolean = true,
        val includeIdField: Boolean = true,
        val includeDateField: Boolean = true,
        val includeClassField: Boolean = true,
        val bubbleSize: Float = 8f,   // mm
        val bubbleSpacing: Float = 5f, // mm
        val columns: Int = MAX_COLUMNS_PER_PAGE
    )

    /**
     * A single bubble in the pure sheet model.
     *
     * Coordinates are the bubble **center** in PDF points, using a top-left origin
     * (matching Android [Canvas]: y increases downward). [radius] is also in points.
     */
    data class Bubble(
        val questionNumber: Int,
        val label: String,
        val centerX: Float,
        val centerY: Float,
        val radius: Float
    )

    /**
     * A single question row in the pure sheet model.
     */
    data class SheetQuestion(
        val questionNumber: Int,
        val optionLabels: List<String>,
        val bubbles: List<Bubble>
    )

    /**
     * Pure, device-free description of a rendered answer sheet.
     *
     * Produced by [buildSheetModel]. Contains everything a simulator needs to fill and
     * "scan" the sheet deterministically: the question count, option labels, and per-bubble
     * coordinates/labels. Contains NO Android/PDF resources.
     */
    data class SheetModel(
        val examName: String,
        val totalQuestions: Int,
        val optionLabels: List<String>,
        val questions: List<SheetQuestion>,
        val pageCount: Int,
        val bubbleRadius: Float,
        /** Selected printed page width in points (one of [PageSize.widthPt]). */
        val pageWidthPt: Float,
        /** Selected printed page height in points (one of [PageSize.heightPt]). */
        val pageHeightPt: Float,
        /** Effective columns per page (1..[MAX_COLUMNS_PER_PAGE]). */
        val columnsPerPage: Int,
        /** Questions rendered per page = [columnsPerPage] × dynamic rows-per-column. */
        val questionsPerPage: Int
    ) {
        /** Convenience lookup: all bubbles flattened across questions. */
        val bubbles: List<Bubble> get() = questions.flatMap { it.bubbles }

        /** Returns the bubble for [questionNumber]/[label], or null if absent. */
        fun bubbleFor(questionNumber: Int, label: String): Bubble? =
            questions.firstOrNull { it.questionNumber == questionNumber }
                ?.bubbles?.firstOrNull { it.label == label }
    }

    /**
     * Builds a pure [SheetModel] from [config] WITHOUT any PDF I/O.
     *
     * This is the key testability seam. It computes the exact layout (page breaks, column
     * placement, bubble centers/radii) using the same geometry the PDF renderer uses, so a
     * simulator can reproduce fills deterministically.
     *
     * When [template] is provided, its [TemplateEntity.numberOfChoices] is used to derive the
     * effective option count/labels if [config] was not explicitly customized to match.
     *
     * @return a fully-populated [SheetModel]. Never performs file or bitmap operations.
     */
    fun buildSheetModel(config: SheetConfig, template: TemplateEntity? = null): SheetModel {
        val effective = resolveConfig(config, template)
        val labels = effective.optionsLabels

        val bubbleRadiusPt = (effective.bubbleSize / 2f) * MM_TO_PT
        val spacingPt = effective.bubbleSpacing * MM_TO_PT
        // Center-to-center distance between adjacent bubbles within a question. Pitch is the
        // bubble DIAMETER + spacing, so adjacent option bubbles never overlap.
        val bubblePitch = bubbleRadiusPt * 2f + spacingPt

        // Width one question column must occupy: the "NN." label + all option bubbles laid
        // out at `bubblePitch`, plus a small inter-column gutter so neighbouring columns'
        // bubbles can never touch.
        val requiredColumnWidth = requiredColumnWidth(labels.size, bubbleRadiusPt, bubblePitch)

        // Desired columns is the caller's hint, capped at the hard maximum.
        val desiredColumns = effective.columns.coerceIn(1, MAX_COLUMNS_PER_PAGE)

        // Row height guarantees a clear vertical gap between question rows: at least
        // 1.6× the bubble diameter and never below the historical 34pt. This is the
        // non-overlap floor and is NEVER reduced to fit more rows.
        val rowHeight = rowHeight(bubbleRadiusPt)

        // The content start Y is the same fixed header/instruction block on every page.
        val contentTopY = firstQuestionTopY(effective)

        // Deterministically pick a page size that fits ALL questions on ONE page if any size
        // can (first-fit over A4 → LEGAL → LETTER by dynamic capacity); otherwise fall back
        // to the maximum-capacity size and paginate. Capacity per page grows with page HEIGHT
        // because rows-per-column is derived from available vertical space (no strict 10 cap).
        val selection = selectPageSize(
            requiredColumnWidth = requiredColumnWidth,
            desiredColumns = desiredColumns,
            rowHeight = rowHeight,
            contentTopY = contentTopY,
            totalQuestions = effective.totalQuestions
        )
        val pageSize = selection.pageSize
        val columnsPerPage = selection.columns
        val rowsPerColumn = selection.rowsPerColumn

        val usableWidth = pageSize.widthPt - 2 * MARGIN
        // Distribute the usable width evenly across the columns we actually place.
        val columnWidth = usableWidth / columnsPerPage

        val questionsPerPage = columnsPerPage * rowsPerColumn

        val questions = ArrayList<SheetQuestion>(effective.totalQuestions)

        var q = 1
        while (q <= effective.totalQuestions) {
            // Zero-based index of the question within its page.
            val indexInPage = (q - 1) % questionsPerPage
            // Fill column by column, `rowsPerColumn` rows per column (dynamic, height-derived).
            val col = indexInPage / rowsPerColumn
            val rowInColumn = indexInPage % rowsPerColumn

            val rowTopY = contentTopY + rowInColumn * rowHeight
            val columnStartX = MARGIN + col * columnWidth
            // Question number occupies the left of the column; bubbles start after it.
            val firstBubbleCenterX = columnStartX + QUESTION_LABEL_WIDTH + bubbleRadiusPt
            val bubbleCenterY = rowTopY + rowHeight / 2f

            val bubbles = labels.mapIndexed { i, label ->
                Bubble(
                    questionNumber = q,
                    label = label,
                    centerX = firstBubbleCenterX + i * bubblePitch,
                    centerY = bubbleCenterY,
                    radius = bubbleRadiusPt
                )
            }
            questions.add(SheetQuestion(q, labels, bubbles))
            q++
        }

        val pageCount = if (effective.totalQuestions <= 0) {
            1
        } else {
            (effective.totalQuestions + questionsPerPage - 1) / questionsPerPage
        }

        return SheetModel(
            examName = effective.examName,
            totalQuestions = effective.totalQuestions,
            optionLabels = labels,
            questions = questions,
            pageCount = pageCount,
            bubbleRadius = bubbleRadiusPt,
            pageWidthPt = pageSize.widthPt,
            pageHeightPt = pageSize.heightPt,
            columnsPerPage = columnsPerPage,
            questionsPerPage = questionsPerPage
        )
    }

    /** Result of deterministic page-size selection. */
    private data class PageSelection(
        val pageSize: PageSize,
        val columns: Int,
        val rowsPerColumn: Int
    ) {
        /** Questions this page can hold = columns × dynamic rows-per-column. */
        val capacity: Int get() = columns * rowsPerColumn
    }

    /**
     * Number of columns of [requiredColumnWidth] that fit horizontally on [size], capped at
     * [desiredColumns] and [MAX_COLUMNS_PER_PAGE], and always at least 1.
     */
    private fun columnsFor(size: PageSize, requiredColumnWidth: Float, desiredColumns: Int): Int {
        val usableWidth = size.widthPt - 2 * MARGIN
        return (usableWidth / requiredColumnWidth).toInt()
            .coerceIn(1, MAX_COLUMNS_PER_PAGE)
            .coerceAtMost(desiredColumns)
    }

    /**
     * DYNAMIC rows-per-column for [pageHeightPt]: how many [rowHeight]-tall question rows fit
     * in the available vertical space of a column, where availableColumnHeight =
     * pageHeight − bottom MARGIN − [contentTopY] (the fixed header/fields/instructions block).
     * Always at least 1. Grows with page height, so taller paper (Legal) holds more rows.
     */
    private fun rowsPerColumn(pageHeightPt: Float, rowHeight: Float, contentTopY: Float): Int {
        val availableColumnHeight = pageHeightPt - MARGIN - contentTopY
        return (availableColumnHeight / rowHeight).toInt().coerceAtLeast(1)
    }

    /**
     * Deterministically selects a [PageSize], effective column count, and dynamic
     * rows-per-column to hold all questions on as FEW pages as possible.
     *
     * Walks [PageSize] entries in declaration order (A4 → LEGAL → LETTER). For each size the
     * effective columns is how many [requiredColumnWidth]-wide columns fit horizontally
     * (capped at [desiredColumns]/[MAX_COLUMNS_PER_PAGE]) and rows-per-column is derived from
     * the page height. Capacity = columns × rowsPerColumn.
     *
     * Returns the FIRST size whose capacity ≥ [totalQuestions] (fits everything on ONE page).
     * If no size fits, returns the size with the MAXIMUM capacity (ties broken by declaration
     * order) so callers paginate onto the fewest additional pages. Pure and stateless:
     * identical inputs always produce the identical selection.
     */
    private fun selectPageSize(
        requiredColumnWidth: Float,
        desiredColumns: Int,
        rowHeight: Float,
        contentTopY: Float,
        totalQuestions: Int
    ): PageSelection {
        var best: PageSelection? = null
        for (size in PageSize.entries) {
            val columns = columnsFor(size, requiredColumnWidth, desiredColumns)
            val rows = rowsPerColumn(size.heightPt, rowHeight, contentTopY)
            val selection = PageSelection(size, columns, rows)
            if (selection.capacity >= totalQuestions) {
                return selection
            }
            val current = best
            if (current == null || selection.capacity > current.capacity) {
                best = selection
            }
        }
        return best ?: PageSelection(PageSize.A4, 1, 1)
    }

    /** Points a single question column needs: label + option bubbles + inter-column gutter. */
    private fun requiredColumnWidth(optionCount: Int, bubbleRadiusPt: Float, bubblePitch: Float): Float {
        // First bubble center sits QUESTION_LABEL_WIDTH + radius from the column's left edge;
        // last bubble center is (optionCount-1) pitches further; add radius for its right edge.
        val bubblesWidth = QUESTION_LABEL_WIDTH + bubbleRadiusPt * 2f + (optionCount - 1) * bubblePitch
        return bubblesWidth + COLUMN_GUTTER
    }

    /** Row height enforcing a clear vertical gap: max(34pt, 1.6 × bubble diameter). */
    private fun rowHeight(bubbleRadiusPt: Float): Float =
        maxOf(ROW_HEIGHT, bubbleRadiusPt * 2f * ROW_HEIGHT_DIAMETER_FACTOR)

    /**
     * Generates a printable answer sheet PDF and returns the absolute file path.
     *
     * Renders the QR code into the fixed header slot, student-info fields, instructions, and
     * the bubble grid computed by [buildSheetModel]. Page breaks from the model drive
     * incremental page rendering.
     *
     * @param exam owning exam (its id is used in the file name and footer).
     * @param metadata metadata encoded into the header QR code.
     * @param config render-time configuration.
     * @param template optional rich template for custom layouts (header text, QR position).
     * @return absolute path to the generated PDF.
     */
    suspend fun generateAnswerSheet(
        exam: ExamEntity,
        metadata: QRCodeGenerator.ExamMetadata,
        config: SheetConfig,
        template: TemplateEntity? = null
    ): String = withContext(Dispatchers.IO) {
        // The PDF path requires both collaborators; they are only optional for the
        // pure buildSheetModel round-trip test seam.
        val qrGen = requireNotNull(qrGenerator) {
            "generateAnswerSheet requires a QRCodeGenerator; construct AnswerSheetPrettyPrinter with one"
        }
        val ctx = requireNotNull(context) {
            "generateAnswerSheet requires a Context; construct AnswerSheetPrettyPrinter with one"
        }
        val effective = resolveConfig(config, template)
        val model = buildSheetModel(effective, template)

        // Render with the SAME page dimensions the model selected.
        val pageWidth = model.pageWidthPt
        val pageHeight = model.pageHeightPt

        val document = PdfDocument()
        try {
            val qrBitmap = qrGen.generateQRCode(metadata, QR_BITMAP_PX)

            // Group model questions by page using the SAME dynamic rule buildSheetModel used:
            // columnsPerPage × dynamic rows-per-column = model.questionsPerPage.
            val chunks = model.questions.chunked(model.questionsPerPage.coerceAtLeast(1))
            val totalPages = chunks.size.coerceAtLeast(1)

            val paints = Paints()

            for ((pageIndex, pageQuestions) in chunks.ifEmpty { listOf(emptyList()) }.withIndex()) {
                val pageInfo = PdfDocument.PageInfo
                    .Builder(pageWidth.toInt(), pageHeight.toInt(), pageIndex + 1)
                    .create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                drawHeader(canvas, effective, template, qrBitmap, paints, pageWidth)
                drawStudentFields(canvas, effective, paints, pageWidth)
                drawInstructions(canvas, effective, paints)
                drawBubbles(canvas, pageQuestions, paints)
                drawFooter(canvas, exam.id, pageIndex + 1, totalPages, paints, pageHeight)

                document.finishPage(page)
            }

            val outputDir = File(ctx.filesDir, "answer_sheets")
            if (!outputDir.exists()) outputDir.mkdirs()
            val outputFile = File(
                outputDir,
                "sheet_${exam.id}_${System.currentTimeMillis()}.pdf"
            )
            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
                out.flush()
            }
            outputFile.absolutePath
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Failed to generate answer sheet for exam ${exam.id}", e)
            throw e
        } finally {
            document.close()
        }
    }

    // ---------------------------------------------------------------------------------------
    // Layout helpers (shared by buildSheetModel and generateAnswerSheet so geometry matches)
    // ---------------------------------------------------------------------------------------

    /**
     * Resolves an effective config, enforcing minimum bubble geometry and deriving option
     * labels from a supplied [template] when the config leaves them at the default.
     */
    private fun resolveConfig(config: SheetConfig, template: TemplateEntity?): SheetConfig {
        // Enforce minimums: bubbles must be >= 8mm diameter with >= 5mm spacing.
        val size = maxOf(config.bubbleSize, MIN_BUBBLE_DIAMETER_MM)
        val spacing = maxOf(config.bubbleSpacing, MIN_BUBBLE_SPACING_MM)

        // If a template dictates a choice count that differs from the default 4, honor it
        // (unless the caller explicitly customized the labels to a different length).
        val templateChoices = template?.numberOfChoices
        val optionsCount: Int
        val labels: List<String>
        if (templateChoices != null &&
            config.optionsCount == 4 &&
            config.optionsLabels.size == 4
        ) {
            optionsCount = templateChoices.coerceIn(MIN_OPTIONS, MAX_OPTIONS)
            labels = defaultLabels(optionsCount)
        } else {
            optionsCount = config.optionsCount.coerceIn(MIN_OPTIONS, MAX_OPTIONS)
            labels = if (config.optionsLabels.size == optionsCount) {
                config.optionsLabels
            } else {
                defaultLabels(optionsCount)
            }
        }

        return config.copy(
            bubbleSize = size,
            bubbleSpacing = spacing,
            optionsCount = optionsCount,
            optionsLabels = labels
        )
    }

    /** Generates default option labels A, B, C, ... for [count] options (A-G). */
    private fun defaultLabels(count: Int): List<String> =
        (0 until count).map { ('A' + it).toString() }

    /** Y (points) of the top of the first question row — same fixed block on every page. */
    private fun firstQuestionTopY(config: SheetConfig): Float {
        var y = MARGIN + HEADER_HEIGHT
        // Student info fields block.
        var fieldLines = 0
        if (config.includeNameField) fieldLines++
        if (config.includeIdField) fieldLines++
        if (config.includeClassField) fieldLines++
        if (config.includeDateField) fieldLines++
        y += fieldLines * FIELD_LINE_HEIGHT
        y += INSTRUCTION_HEIGHT
        return y
    }

    // ---------------------------------------------------------------------------------------
    // Drawing helpers
    // ---------------------------------------------------------------------------------------

    private class Paints {
        val title = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val header = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val body = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            isAntiAlias = true
        }
        val line = Paint().apply {
            color = Color.DKGRAY
            strokeWidth = 1f
        }
        val bubble = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            isAntiAlias = true
        }
        val bubbleLabel = Paint().apply {
            color = Color.BLACK
            textSize = 7f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val footer = Paint().apply {
            color = Color.GRAY
            textSize = 8f
            isAntiAlias = true
        }
    }

    private fun drawHeader(
        canvas: Canvas,
        config: SheetConfig,
        template: TemplateEntity?,
        qrBitmap: Bitmap,
        paints: Paints,
        pageWidth: Float
    ) {
        val headerText = template?.headerText?.takeIf { it.isNotBlank() } ?: config.examName
        canvas.drawText(headerText, MARGIN, MARGIN + 18f, paints.title)
        canvas.drawText("${config.totalQuestions} Questions", MARGIN, MARGIN + 36f, paints.body)

        // QR code in the fixed header slot. Default TOP_RIGHT; honor template position.
        val qrDest = qrHeaderSlot(template?.qrCodePosition, pageWidth)
        canvas.drawBitmap(qrBitmap, null, qrDest, null)
    }

    /** Returns the destination rect (points) for the QR code in the header. */
    private fun qrHeaderSlot(position: String?, pageWidth: Float): Rect {
        val top = MARGIN.toInt()
        val bottom = (MARGIN + QR_SLOT_SIZE).toInt()
        return when (position) {
            "TOP_LEFT" -> Rect(MARGIN.toInt(), top, (MARGIN + QR_SLOT_SIZE).toInt(), bottom)
            "TOP_CENTER" -> {
                val left = ((pageWidth - QR_SLOT_SIZE) / 2f).toInt()
                Rect(left, top, (left + QR_SLOT_SIZE).toInt(), bottom)
            }
            else -> {
                // TOP_RIGHT (default)
                val right = (pageWidth - MARGIN).toInt()
                Rect((right - QR_SLOT_SIZE).toInt(), top, right, bottom)
            }
        }
    }

    private fun drawStudentFields(canvas: Canvas, config: SheetConfig, paints: Paints, pageWidth: Float) {
        var y = MARGIN + HEADER_HEIGHT
        val labelX = MARGIN
        val lineStartX = MARGIN + 90f
        val lineEndX = pageWidth - MARGIN

        fun field(label: String) {
            canvas.drawText(label, labelX, y, paints.header)
            canvas.drawLine(lineStartX, y + 2f, lineEndX, y + 2f, paints.line)
            y += FIELD_LINE_HEIGHT
        }

        if (config.includeNameField) field("NAME:")
        if (config.includeIdField) field("STUDENT ID:")
        if (config.includeClassField) field("SECTION:")
        if (config.includeDateField) field("DATE:")
    }

    /**
     * Instructions are drawn just below the student-info fields block, on the same baseline
     * the model uses to reserve [INSTRUCTION_HEIGHT] before the first question row (see
     * [firstQuestionTopY]).
     */
    private fun drawInstructions(canvas: Canvas, config: SheetConfig, paints: Paints) {
        var fieldLines = 0
        if (config.includeNameField) fieldLines++
        if (config.includeIdField) fieldLines++
        if (config.includeClassField) fieldLines++
        if (config.includeDateField) fieldLines++
        val fieldsBottom = MARGIN + HEADER_HEIGHT + fieldLines * FIELD_LINE_HEIGHT
        canvas.drawText(INSTRUCTION_TEXT, MARGIN, fieldsBottom + 14f, paints.body)
    }

    private fun drawBubbles(
        canvas: Canvas,
        pageQuestions: List<SheetQuestion>,
        paints: Paints
    ) {
        for (question in pageQuestions) {
            val first = question.bubbles.firstOrNull() ?: continue
            // Question number to the left of the first bubble.
            val labelX = first.centerX - first.radius - QUESTION_LABEL_WIDTH
            canvas.drawText(
                String.format("%2d.", question.questionNumber),
                labelX.coerceAtLeast(MARGIN),
                first.centerY + 4f,
                paints.body
            )
            for (b in question.bubbles) {
                canvas.drawCircle(b.centerX, b.centerY, b.radius, paints.bubble)
                canvas.drawText(b.label, b.centerX, b.centerY + 3f, paints.bubbleLabel)
            }
        }
    }

    private fun drawFooter(
        canvas: Canvas,
        examId: Long,
        page: Int,
        totalPages: Int,
        paints: Paints,
        pageHeight: Float
    ) {
        canvas.drawText(
            "Exam #$examId  •  Page $page of $totalPages",
            MARGIN,
            pageHeight - MARGIN / 2f,
            paints.footer
        )
    }

    companion object {
        private const val TAG = "AnswerSheetPrettyPrinter"

        private const val MARGIN = 36f // ~0.5in

        // Layout rule: rows-per-column is DYNAMIC (derived from page height so taller paper
        // holds more rows), with a hard cap on how many columns are placed per page.
        const val MAX_COLUMNS_PER_PAGE = 3

        // Small horizontal gutter (points) kept between adjacent columns so no two columns'
        // bubbles can ever touch.
        private const val COLUMN_GUTTER = 12f

        // Vertical spacing safety: each question row is at least this multiple of the bubble
        // DIAMETER tall, guaranteeing a clear gap between vertically-adjacent rows.
        private const val ROW_HEIGHT_DIAMETER_FACTOR = 1.6f

        // 1 mm = 72/25.4 points.
        private const val MM_TO_PT = 72f / 25.4f

        // Enforced minimums per requirement 21 (bubbles >= 8mm, spacing >= 5mm).
        private const val MIN_BUBBLE_DIAMETER_MM = 8f
        private const val MIN_BUBBLE_SPACING_MM = 5f

        // Supported option range A..G.
        private const val MIN_OPTIONS = 2
        private const val MAX_OPTIONS = 7

        // Fixed header block: title + question count + QR slot.
        private const val HEADER_HEIGHT = 60f
        private const val QR_SLOT_SIZE = 90f // points (~1.25in), fixed header slot
        private const val QR_BITMAP_PX = 200

        private const val FIELD_LINE_HEIGHT = 22f
        private const val INSTRUCTION_HEIGHT = 24f

        // Row height sized so an 8mm bubble (~22.7pt) fits with vertical breathing room.
        private const val ROW_HEIGHT = 34f
        private const val QUESTION_LABEL_WIDTH = 26f // space reserved for the "NN." label

        private const val INSTRUCTION_TEXT =
            "Shade circles completely. Use black or blue pen."
    }
}
