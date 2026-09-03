package com.examscanner.premium.utils

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Template PDF Generator - Deterministic "fit, then escalate" layout that keeps templates
 * clean and legible while avoiding overlap.
 *
 * The layout is decided by a pure, testable function ([resolveLayout]) that walks a
 * fit-then-escalate ladder and stops at the FIRST option on which the content fits WITHOUT
 * overlap:
 *   1. In-page fixes first (2-up): fit BOTH templates on one 2-up page by increasing columns
 *      per template from 2 up to 3 (split questions as evenly as possible) and modestly
 *      reducing row height down to MIN_ROW_HEIGHT. Preference: fewest columns first, then
 *      tallest rows first. A 3rd column that won't fit the page width is skipped.
 *   2. Grow paper, still 2-up: iterate Letter -> Legal -> A4 -> A3, re-running the in-page
 *      fit (up to 3 cols, row-height search) on each. First size where both templates fit
 *      wins.
 *   3. 1-up (one template per full page): if even A3 2-up can't hold two templates, switch to
 *      one template per page (no cut line) and iterate Letter -> Legal -> A4 -> A3, picking
 *      the smallest that holds a single template on one page (up to 3 cols).
 *   4. Multi-page (last resort): if a single template still doesn't fit A3 1-up at MIN row
 *      height / 3 columns, the single template FLOWS onto additional pages. pageCount =
 *      ceil(totalQuestions / capacityPerTemplate) on A3, 1-up, 3 cols, MIN_ROW_HEIGHT.
 *
 * Identical inputs always produce an identical layout. No randomness.
 */
object TemplatePDFGenerator {

    // Fixed drawing constants (used against the SELECTED page dimensions).
    private const val MARGIN = 30
    private const val MARGIN_BOTTOM = 30
    private const val CUT_GAP = 10 // gap between the two 2-up halves / cut line
    private const val BUBBLE_SIZE = 16f
    private const val BUBBLE_SPACING = 22f
    private const val COLUMN_SPACING = 25
    private const val QUESTION_LABEL_WIDTH = 35f // space reserved for "12." before the bubbles

    // Row-height range. MIN_ROW_HEIGHT keeps a ~16pt bubble from overlapping vertically:
    // MIN_ROW_HEIGHT >= BUBBLE_SIZE + small gap (16 + 4 = 20).
    const val MIN_ROW_HEIGHT = 20f
    private const val DEFAULT_ROW_HEIGHT = 24f
    private const val ROW_HEIGHT_STEP = 1f

    // Columns per template: from 2 up to 3.
    const val MIN_COLUMNS = 2
    const val MAX_COLUMNS = 3

    /**
     * Paper sizes in escalation order. Widths/heights in PDF points (72 DPI).
     */
    enum class PaperSize(val widthPt: Int, val heightPt: Int) {
        LETTER(612, 792),
        LEGAL(612, 1008),
        A4(595, 842),
        A3(842, 1191)
    }

    /** The order paper sizes are tried when escalating. */
    private val PAPER_LADDER = listOf(PaperSize.LETTER, PaperSize.LEGAL, PaperSize.A4, PaperSize.A3)

    /**
     * Resolved, device-free layout decision. Rendering derives entirely from this.
     */
    data class TemplateLayout(
        val paperSize: PaperSize,
        val columnsPerTemplate: Int,
        val rowHeight: Float,
        val is2up: Boolean,
        val pageCount: Int,
        val capacityPerTemplate: Int
    )

    // -------------------------------------------------------------------------------------
    // PURE LAYOUT CORE (no Canvas / no Context) - unit-testable.
    // -------------------------------------------------------------------------------------

    /**
     * Height consumed by the header block drawn at the top of every template in
     * [drawSingleTemplate] (title, question count, separator, name line, date/score line,
     * separator, instructions). Computed from the same yPosition advances used when drawing
     * so the fit math stays honest.
     */
    internal fun headerBlockHeight(): Float {
        var y = 0f
        y += 20 // title
        y += 20 // "$totalQuestions Questions"
        y += 12 // separator line + gap
        y += 18 // Name field
        y += 20 // Date + Score fields
        y += 15 // separator line + gap
        y += 18 // Instructions line
        return y
    }

    /**
     * Available vertical space for question rows inside a single template on [paper].
     */
    internal fun availableTemplateHeight(paper: PaperSize, is2up: Boolean): Float {
        val templateOuterHeight = if (is2up) {
            (paper.heightPt - CUT_GAP) / 2f
        } else {
            paper.heightPt.toFloat()
        }
        return templateOuterHeight - MARGIN - headerBlockHeight() - MARGIN_BOTTOM
    }

    /**
     * Width required by a single question column: label + bubbles + a small gutter.
     */
    internal fun requiredColumnWidth(choicesPerQuestion: Int): Float {
        val gutter = 10f
        return QUESTION_LABEL_WIDTH + choicesPerQuestion * BUBBLE_SPACING + gutter
    }

    /**
     * Horizontal check: do [columns] columns fit within the page's usable width?
     */
    internal fun columnsFitHorizontally(paper: PaperSize, columns: Int, choicesPerQuestion: Int): Boolean {
        val usable = paper.widthPt - 2 * MARGIN
        val needed = columns * requiredColumnWidth(choicesPerQuestion) +
            (columns - 1) * COLUMN_SPACING
        return needed <= usable
    }

    /**
     * Column counts to try on [paper], from fewest to most, filtered to those that fit
     * horizontally. Deterministic ascending ordering (MIN_COLUMNS..MAX_COLUMNS).
     */
    internal fun columnCandidates(paper: PaperSize, choicesPerQuestion: Int): List<Int> =
        (MIN_COLUMNS..MAX_COLUMNS).filter { columnsFitHorizontally(paper, it, choicesPerQuestion) }

    /**
     * Rows that fit in one column for the given template height / row height.
     */
    internal fun rowsPerColumn(availableHeight: Float, rowHeight: Float): Int =
        floor(availableHeight / rowHeight).toInt().coerceAtLeast(0)

    /**
     * Capacity of a single template (rows x columns) for a concrete configuration.
     */
    internal fun capacityPerTemplate(
        paper: PaperSize,
        columns: Int,
        rowHeight: Float,
        is2up: Boolean
    ): Int {
        val rows = rowsPerColumn(availableTemplateHeight(paper, is2up), rowHeight)
        return rows * columns
    }

    /**
     * The row-height candidates to try for a given configuration, from most legible
     * (DEFAULT) down to the floor. Deterministic ordering.
     */
    internal fun rowHeightCandidates(): List<Float> {
        val list = mutableListOf<Float>()
        var rh = DEFAULT_ROW_HEIGHT
        while (rh >= MIN_ROW_HEIGHT) {
            list.add(rh)
            rh -= ROW_HEIGHT_STEP
        }
        if (list.last() != MIN_ROW_HEIGHT) list.add(MIN_ROW_HEIGHT)
        return list
    }

    /**
     * Try to find a (columns, rowHeight) pairing that holds [totalQuestions] on ONE template
     * of the given [paper] and [is2up] mode. Tries columns ascending (fewest first), then row
     * heights DEFAULT -> MIN (tallest / most legible first). Returns the first fitting pair,
     * or null if none fits even at the floor.
     */
    internal fun findInPageFit(
        paper: PaperSize,
        is2up: Boolean,
        totalQuestions: Int,
        choicesPerQuestion: Int
    ): Pair<Int, Float>? {
        for (cols in columnCandidates(paper, choicesPerQuestion)) {
            for (rh in rowHeightCandidates()) {
                if (capacityPerTemplate(paper, cols, rh, is2up) >= totalQuestions) {
                    return Pair(cols, rh)
                }
            }
        }
        return null
    }

    /**
     * Deterministic fit-then-escalate ladder. See class docs.
     */
    internal fun resolveLayout(totalQuestions: Int, choicesPerQuestion: Int): TemplateLayout {
        val questions = totalQuestions.coerceAtLeast(1)
        val choices = choicesPerQuestion.coerceAtLeast(1)

        // Steps 1 & 2: 2-up. Grow paper, fitting both templates via in-page (cols + rows).
        for (paper in PAPER_LADDER) {
            val fit = findInPageFit(paper, is2up = true, totalQuestions = questions, choicesPerQuestion = choices)
            if (fit != null) {
                val (cols, rh) = fit
                return TemplateLayout(
                    paperSize = paper,
                    columnsPerTemplate = cols,
                    rowHeight = rh,
                    is2up = true,
                    pageCount = 1,
                    capacityPerTemplate = capacityPerTemplate(paper, cols, rh, is2up = true)
                )
            }
        }

        // Step 3: 1-up. Grow paper, one template per full page.
        for (paper in PAPER_LADDER) {
            val fit = findInPageFit(paper, is2up = false, totalQuestions = questions, choicesPerQuestion = choices)
            if (fit != null) {
                val (cols, rh) = fit
                return TemplateLayout(
                    paperSize = paper,
                    columnsPerTemplate = cols,
                    rowHeight = rh,
                    is2up = false,
                    pageCount = 1,
                    capacityPerTemplate = capacityPerTemplate(paper, cols, rh, is2up = false)
                )
            }
        }

        // Step 4: Multi-page, 1-up on A3 at the floor, using the largest fitting column count.
        val paper = PaperSize.A3
        val rh = MIN_ROW_HEIGHT
        val cols = columnCandidates(paper, choices).lastOrNull() ?: MIN_COLUMNS
        val perPage = capacityPerTemplate(paper, cols, rh, is2up = false).coerceAtLeast(1)
        val pageCount = ceil(questions.toDouble() / perPage).toInt().coerceAtLeast(1)
        return TemplateLayout(
            paperSize = paper,
            columnsPerTemplate = cols,
            rowHeight = rh,
            is2up = false,
            pageCount = pageCount,
            capacityPerTemplate = perPage
        )
    }

    /**
     * Distribute [total] questions across [columns] as evenly as possible, filling column by
     * column. Returns the count per column (sums to total).
     */
    internal fun distributeColumns(total: Int, columns: Int): List<Int> {
        val perColumn = ceil(total.toDouble() / columns).toInt()
        val result = mutableListOf<Int>()
        var remaining = total
        repeat(columns) {
            val n = minOf(perColumn, remaining)
            result.add(n)
            remaining -= n
        }
        return result
    }

    // -------------------------------------------------------------------------------------
    // RENDERING
    // -------------------------------------------------------------------------------------

    fun generateTemplate(
        context: Context,
        templateName: String,
        totalQuestions: Int,
        choicesPerQuestion: Int
    ): File {
        val layout = resolveLayout(totalQuestions, choicesPerQuestion)
        val pageWidth = layout.paperSize.widthPt
        val pageHeight = layout.paperSize.heightPt

        val document = PdfDocument()

        // Paint objects
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            isAntiAlias = true
        }
        val bubblePaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }
        val dashedLinePaint = Paint().apply {
            color = Color.GRAY
            strokeWidth = 1.5f
            pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
        }

        if (layout.is2up) {
            // 2-up: two identical templates on ONE page (top + cut line + bottom). The whole
            // exam fits per the fit guarantee, so both halves show questions 1..total.
            val page = startPage(document, pageWidth, pageHeight, 1)
            val canvas = page.canvas
            val templateHeight = (pageHeight - CUT_GAP) / 2f
            val cutPaint = Paint(bodyPaint).apply {
                textSize = 10f
                textAlign = Paint.Align.CENTER
            }

            // TOP template.
            drawSingleTemplate(
                canvas, 0f, 1, templateName,
                totalQuestions, choicesPerQuestion, layout,
                titlePaint, bodyPaint, bubblePaint, linePaint,
                firstQuestion = 1,
                questionsOnPage = totalQuestions
            )

            // Cut line.
            val cutLineY = templateHeight + CUT_GAP / 2f
            canvas.drawLine(0f, cutLineY, pageWidth.toFloat(), cutLineY, dashedLinePaint)
            canvas.drawText("\u2702 CUT HERE \u2702", pageWidth / 2f, cutLineY - 3, cutPaint)

            // BOTTOM template - identical to the top.
            drawSingleTemplate(
                canvas, templateHeight + CUT_GAP, 2, templateName,
                totalQuestions, choicesPerQuestion, layout,
                titlePaint, bodyPaint, bubblePaint, linePaint,
                firstQuestion = 1,
                questionsOnPage = totalQuestions
            )

            document.finishPage(page)
        } else {
            // 1-up: one template per page, flowing questions across pages. Page i shows
            // questions [i*capacity+1 .. (i+1)*capacity]. No cut line.
            for (pageIndex in 0 until layout.pageCount) {
                val page = startPage(document, pageWidth, pageHeight, pageIndex + 1)
                val canvas = page.canvas

                val firstQuestion = pageIndex * layout.capacityPerTemplate + 1
                val remaining = totalQuestions - (firstQuestion - 1)
                val onThisPage = minOf(layout.capacityPerTemplate, remaining).coerceAtLeast(0)

                drawSingleTemplate(
                    canvas, 0f, 1, templateName,
                    totalQuestions, choicesPerQuestion, layout,
                    titlePaint, bodyPaint, bubblePaint, linePaint,
                    firstQuestion = firstQuestion,
                    questionsOnPage = onThisPage
                )

                document.finishPage(page)
            }
        }

        // Save to external storage where FileProvider can access it
        val outputDir = File(context.getExternalFilesDir(null), "Templates")
        if (!outputDir.exists()) outputDir.mkdirs()

        val layoutTag = if (layout.is2up) "2up" else "1up"
        val timestamp = System.currentTimeMillis()
        val outputFile = File(
            outputDir,
            "${templateName.replace(" ", "_")}_${layoutTag}_$timestamp.pdf"
        )

        try {
            val outputStream = FileOutputStream(outputFile)
            document.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            document.close()
            return outputFile
        } catch (e: Exception) {
            document.close()
            throw Exception("Failed to generate template: ${e.message}", e)
        }
    }

    private fun startPage(
        document: PdfDocument,
        pageWidth: Int,
        pageHeight: Int,
        pageNumber: Int
    ): PdfDocument.Page {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        return document.startPage(pageInfo)
    }

    private fun drawSingleTemplate(
        canvas: Canvas,
        startY: Float,
        templateNumber: Int,
        templateName: String,
        totalQuestions: Int,
        choicesPerQuestion: Int,
        layout: TemplateLayout,
        titlePaint: Paint,
        bodyPaint: Paint,
        bubblePaint: Paint,
        linePaint: Paint,
        firstQuestion: Int,
        questionsOnPage: Int = totalQuestions
    ) {
        val pageWidth = layout.paperSize.widthPt
        var yPosition = startY + MARGIN

        // HEADER SECTION
        canvas.drawText("$templateName (#$templateNumber)", MARGIN.toFloat(), yPosition, titlePaint)
        yPosition += 20

        canvas.drawText("$totalQuestions Questions", MARGIN.toFloat(), yPosition, bodyPaint)
        yPosition += 20

        canvas.drawLine(MARGIN.toFloat(), yPosition, (pageWidth - MARGIN).toFloat(), yPosition, linePaint)
        yPosition += 12

        canvas.drawText("Name:", MARGIN.toFloat(), yPosition, bodyPaint)
        canvas.drawLine(
            (MARGIN + 60).toFloat(), yPosition + 3,
            (pageWidth - MARGIN).toFloat(), yPosition + 3, linePaint
        )
        yPosition += 18

        canvas.drawText("Date:", MARGIN.toFloat(), yPosition, bodyPaint)
        canvas.drawLine(
            (MARGIN + 60).toFloat(), yPosition + 3,
            (MARGIN + 180).toFloat(), yPosition + 3, linePaint
        )
        canvas.drawText("Score:", (MARGIN + 200).toFloat(), yPosition, bodyPaint)
        canvas.drawLine(
            (MARGIN + 260).toFloat(), yPosition + 3,
            (pageWidth - MARGIN).toFloat(), yPosition + 3, linePaint
        )
        yPosition += 20

        canvas.drawLine(MARGIN.toFloat(), yPosition, (pageWidth - MARGIN).toFloat(), yPosition, linePaint)
        yPosition += 15

        canvas.drawText(
            "Instructions: Fill bubbles completely. Use #2 pencil.",
            MARGIN.toFloat(), yPosition, bodyPaint
        )
        yPosition += 18

        // QUESTIONS SECTION - N columns
        val columns = layout.columnsPerTemplate
        val distribution = distributeColumns(questionsOnPage, columns)
        val columnWidth = (pageWidth - (2 * MARGIN) - (columns - 1) * COLUMN_SPACING) / columns
        val startYPosition = yPosition

        var questionNum = firstQuestion
        for (col in 0 until columns) {
            val count = distribution[col]
            if (count <= 0) continue
            val startX = MARGIN + col * (columnWidth + COLUMN_SPACING)
            drawQuestionColumn(
                canvas, startX, startYPosition,
                questionNum, questionNum + count - 1,
                choicesPerQuestion, layout.rowHeight, bodyPaint, bubblePaint
            )
            questionNum += count
        }
    }

    private fun drawQuestionColumn(
        canvas: Canvas,
        startX: Int,
        startY: Float,
        fromQuestion: Int,
        toQuestion: Int,
        choicesPerQuestion: Int,
        rowHeight: Float,
        textPaint: Paint,
        bubblePaint: Paint
    ) {
        var yPos = startY

        for (questionNum in fromQuestion..toQuestion) {
            canvas.drawText(
                String.format("%2d.", questionNum),
                startX.toFloat(),
                yPos + 13,
                textPaint
            )

            var bubbleX = startX + QUESTION_LABEL_WIDTH
            repeat(choicesPerQuestion) { index ->
                val letter = ('A' + index).toString()

                canvas.drawCircle(
                    bubbleX + BUBBLE_SIZE / 2,
                    yPos + 8,
                    BUBBLE_SIZE / 2,
                    bubblePaint
                )

                val letterPaint = Paint(textPaint).apply {
                    textSize = 9f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(
                    letter,
                    bubbleX + BUBBLE_SIZE / 2,
                    yPos + 12,
                    letterPaint
                )

                bubbleX += BUBBLE_SPACING
            }

            yPos += rowHeight
        }
    }
}
