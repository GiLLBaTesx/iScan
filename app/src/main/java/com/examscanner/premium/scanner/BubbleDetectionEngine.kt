package com.examscanner.premium.scanner

import android.graphics.Bitmap
import android.graphics.Color
import com.examscanner.premium.utils.SecureLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * BubbleDetectionEngine - threshold + contour fill-ratio bubble detection.
 *
 * Covers Requirement 22 (Parser for Scanned Answer Sheets) fill classification
 * (22.4, 22.5, 22.7) and integrates with Requirement 17 (preserve existing
 * scanner) by delegating all image handling to the existing
 * [BubbleSheetProcessor] rather than replacing it. Enforces the 1920×1080
 * image cap from Requirement 19.4 via [BubbleSheetProcessor.capImageResolution].
 *
 * Design reference: design.md §12 (Answer Sheet Parser System).
 *
 * The [BubbleState] enum is defined here (shared type) so that the
 * [AnswerSheetParser] pipeline (task 5.6) can reference
 * `BubbleDetectionEngine.BubbleState` without a circular dependency between the
 * two files. The design's `AnswerSheetParser.BubbleState` maps to this type.
 *
 * [classifyFill] and the fill-ratio helpers are pure and testable so round-trip
 * property tests can simulate detection without a camera or real image I/O.
 *
 * @param processor the existing scanner used for bitmap loading, image capping,
 *   and ML Kit text extraction. Only used by [detectBubbles] (the device path);
 *   the pure methods ([classifyFill], [buildQuestionDetection]) never touch it.
 *   Nullable so JVM unit tests that exercise only the pure round-trip seams can
 *   construct the engine without an Android [android.content.Context]. Production
 *   always injects a real [BubbleSheetProcessor]; [detectBubbles] requires it.
 */
class BubbleDetectionEngine(
    private val processor: BubbleSheetProcessor? = null
) {

    /**
     * Bubble fill classification (Req 22.5). Shared across the parser pipeline.
     * MULTIPLE is a resolution state used by [AnswerSheetParser] when more than
     * one option is SHADED for a question (Req 22.6); it is never produced by
     * [classifyFill] itself.
     */
    enum class BubbleState {
        EMPTY,      // 0-20% filled
        PARTIAL,    // 21-79% filled
        SHADED,     // 80-100% filled
        MULTIPLE    // >1 bubble shaded for a question
    }

    data class DetectionResult(
        val studentId: String?,
        val studentName: String?,
        val questions: List<QuestionDetection>,
        val overallConfidence: Float
    )

    data class QuestionDetection(
        val number: Int,
        val options: List<OptionDetection>,
        val confidence: Float
    )

    data class OptionDetection(
        val label: String,          // "A".."G"
        val fillRatio: Float,       // 0.0 - 1.0
        val state: BubbleState
    )

    /**
     * Classifies bubble fill using the threshold bands from Req 22.5:
     * EMPTY 0-20%, PARTIAL 21-79%, SHADED 80-100%.
     *
     * Pure function; the fill ratio is coerced into [0f, 1f] so out-of-range
     * inputs are handled deterministically.
     */
    fun classifyFill(fillRatio: Float): BubbleState {
        val r = fillRatio.coerceIn(0f, 1f)
        return when {
            r <= EMPTY_THRESHOLD -> BubbleState.EMPTY
            r < SHADED_THRESHOLD -> BubbleState.PARTIAL
            else -> BubbleState.SHADED
        }
    }

    /**
     * Detect bubbles on a scanned answer sheet.
     *
     * Image handling is delegated to [BubbleSheetProcessor]:
     * 1. The image is capped to 1920×1080 (Req 19.4) via
     *    [BubbleSheetProcessor.capImageResolution].
     * 2. Student id / name text extraction reuses
     *    [BubbleSheetProcessor.extractStudentInfo] (ML Kit) — not re-implemented.
     * 3. Fill ratios are computed per option region and classified via
     *    [classifyFill].
     *
     * @param bitmap the scanned sheet.
     * @param grid layout of question/option regions to sample. When omitted an
     *   empty result is returned (the caller supplies the grid derived from the
     *   printed [AnswerSheetPrettyPrinter] layout / template).
     */
    suspend fun detectBubbles(
        bitmap: Bitmap,
        grid: BubbleGrid = BubbleGrid.EMPTY
    ): DetectionResult = withContext(Dispatchers.Default) {
        try {
            // Req 19.4 - enforce the image cap by reusing the existing scanner.
            val capped = BubbleSheetProcessor.capImageResolution(bitmap)

            // Req 17.2 - reuse existing ML Kit text extraction rather than duplicating.
            // The processor is required for the device detection path; it is only
            // optional for the pure round-trip test seams.
            val proc = requireNotNull(processor) {
                "detectBubbles requires a BubbleSheetProcessor; construct BubbleDetectionEngine with one for image detection"
            }
            val extracted = proc.extractStudentInfo(capped)

            val questions = grid.questions.map { q ->
                val options = q.options.map { opt ->
                    val ratio = computeFillRatio(capped, opt)
                    OptionDetection(
                        label = opt.label,
                        fillRatio = ratio,
                        state = classifyFill(ratio)
                    )
                }
                buildQuestionDetection(q.number, options)
            }

            DetectionResult(
                studentId = extracted.studentId.ifEmpty { null },
                studentName = extracted.studentName.ifEmpty { null },
                questions = questions,
                overallConfidence = overallConfidence(questions)
            )
        } catch (e: Exception) {
            SecureLogger.e(TAG, "detectBubbles failed", e)
            DetectionResult(
                studentId = null,
                studentName = null,
                questions = emptyList(),
                overallConfidence = 0f
            )
        }
    }

    /**
     * Build a [QuestionDetection] from already-classified options. Pure and
     * testable; confidence is high when exactly one option is decisively SHADED
     * or all are clearly EMPTY, and lower when PARTIAL/ambiguous states appear.
     */
    fun buildQuestionDetection(
        number: Int,
        options: List<OptionDetection>
    ): QuestionDetection {
        return QuestionDetection(
            number = number,
            options = options,
            confidence = questionConfidence(options)
        )
    }

    /**
     * Compute the fill ratio (fraction of dark pixels) within an option's
     * rectangular region using a luminance threshold. Pure with respect to the
     * bitmap contents; region coordinates outside the bitmap are clamped.
     *
     * Threshold + fill-ratio approach per Req 22.4 (threshold, contour detection).
     */
    fun computeFillRatio(bitmap: Bitmap, region: OptionRegion): Float {
        val left = region.left.coerceIn(0, bitmap.width)
        val top = region.top.coerceIn(0, bitmap.height)
        val right = region.right.coerceIn(0, bitmap.width)
        val bottom = region.bottom.coerceIn(0, bitmap.height)

        val w = right - left
        val h = bottom - top
        if (w <= 0 || h <= 0) return 0f

        var dark = 0
        var total = 0
        var x = left
        while (x < right) {
            var y = top
            while (y < bottom) {
                val pixel = bitmap.getPixel(x, y)
                if (luminance(pixel) < DARK_LUMINANCE_THRESHOLD) {
                    dark++
                }
                total++
                y += SAMPLE_STEP
            }
            x += SAMPLE_STEP
        }
        return if (total == 0) 0f else dark.toFloat() / total.toFloat()
    }

    private fun luminance(pixel: Int): Float {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        // Rec. 601 luma
        return 0.299f * r + 0.587f * g + 0.114f * b
    }

    private fun questionConfidence(options: List<OptionDetection>): Float {
        if (options.isEmpty()) return 0f
        val shaded = options.count { it.state == BubbleState.SHADED }
        val partial = options.count { it.state == BubbleState.PARTIAL }
        return when {
            partial > 0 -> 0.5f            // ambiguous fills reduce confidence
            shaded == 1 -> 0.95f           // one clear answer
            shaded == 0 -> 0.9f            // clearly blank
            else -> 0.6f                   // multiple shaded (resolved upstream)
        }
    }

    private fun overallConfidence(questions: List<QuestionDetection>): Float {
        if (questions.isEmpty()) return 0f
        return questions.map { it.confidence }.average().toFloat()
    }

    /**
     * Rectangular region of a single option bubble in image pixel coordinates.
     */
    data class OptionRegion(
        val label: String,
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    )

    /** A question's set of option regions to sample. */
    data class QuestionRegions(
        val number: Int,
        val options: List<OptionRegion>
    )

    /** Full grid layout of an answer sheet. */
    data class BubbleGrid(
        val questions: List<QuestionRegions>
    ) {
        companion object {
            val EMPTY = BubbleGrid(emptyList())
        }
    }

    companion object {
        private const val TAG = "BubbleDetectionEngine"

        /** Req 22.5 fill-classification thresholds. */
        const val EMPTY_THRESHOLD = 0.20f
        const val SHADED_THRESHOLD = 0.80f

        /** Pixels darker than this luminance (0-255) count as filled. */
        private const val DARK_LUMINANCE_THRESHOLD = 128f

        /** Sample every Nth pixel to keep detection within the time budget. */
        private const val SAMPLE_STEP = 2
    }
}
