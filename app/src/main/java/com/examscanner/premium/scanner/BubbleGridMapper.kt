package com.examscanner.premium.scanner

import com.examscanner.premium.utils.AnswerSheetPrettyPrinter

/**
 * BubbleGridMapper - pure point→pixel mapper from a printed answer-sheet layout to a
 * samplable [BubbleDetectionEngine.BubbleGrid].
 *
 * Bridges the two existing halves of the real scan pipeline (design.md §12):
 *  - [AnswerSheetPrettyPrinter.buildSheetModel] emits a pure [AnswerSheetPrettyPrinter.SheetModel]
 *    whose bubbles carry exact geometry (centerX/centerY/radius) in PDF **points** on an
 *    A4 page (595 × 842 pt, top-left origin, y-down — same convention as Android Canvas).
 *  - [BubbleDetectionEngine.detectBubbles] samples fill ratios over pixel-space
 *    [BubbleDetectionEngine.OptionRegion]s.
 *
 * This object converts the former into the latter for a scan of an app-printed sheet, so a
 * reasonably square-on capture can be graded against the exact bubble positions the app drew.
 *
 * The mapping is a simple independent linear scale per axis:
 *   scaleX = imageWidthPx / pageWidthPt, scaleY = imageHeightPx / pageHeightPt
 * applied to each bubble's bounding box (centerX ± radius, centerY ± radius). Regions are
 * clamped to the image bounds and guaranteed at least 1px wide/tall so
 * [BubbleDetectionEngine.computeFillRatio] never sees a degenerate rectangle.
 *
 * PURE / device-free: no Android graphics types appear in the signature (SheetModel/Bubble
 * and BubbleGrid/OptionRegion are plain data classes), no logging, no side effects — so it
 * is fully JVM-unit-testable.
 *
 * KNOWN LIMITATION (single physical page): a scanned bitmap is exactly ONE physical sheet,
 * i.e. ONE page. [AnswerSheetPrettyPrinter.buildSheetModel] resets each page's row layout,
 * so page-2+ questions reuse the same small centerY as page-1 questions and cannot be told
 * apart from coordinates alone. This first real version therefore maps EVERY question in
 * [AnswerSheetPrettyPrinter.SheetModel.questions] as-is and assumes a single-page sheet.
 * Multi-page exams (pageCount > 1) must be scanned one page at a time (future work); mapping
 * a multi-page model against a single-page scan will overlay later pages onto page 1.
 */
object BubbleGridMapper {

    /**
     * Builds a pixel-space [BubbleDetectionEngine.BubbleGrid] from a printed [model].
     *
     * @param model the pure sheet layout from [AnswerSheetPrettyPrinter.buildSheetModel].
     * @param imageWidthPx width of the (capped) scanned bitmap in pixels.
     * @param imageHeightPx height of the (capped) scanned bitmap in pixels.
     * @param pageWidthPt printed page width in points; defaults to [model]'s selected page width.
     * @param pageHeightPt printed page height in points; defaults to [model]'s selected page height.
     * @return a grid whose questions/options mirror the FIRST page of the model (see below),
     *   with each option's region mapped from the bubble's point-space bounding box into pixel
     *   space and clamped to the image bounds. Returns an empty grid when the image or page
     *   dimensions are non-positive.
     *
     * Only the first `model.questionsPerPage` questions are mapped: a scan is a single physical
     * page and later pages reuse the same per-page coordinates.
     */
    fun buildGrid(
        model: AnswerSheetPrettyPrinter.SheetModel,
        imageWidthPx: Int,
        imageHeightPx: Int,
        pageWidthPt: Float = model.pageWidthPt,
        pageHeightPt: Float = model.pageHeightPt
    ): BubbleDetectionEngine.BubbleGrid {
        if (imageWidthPx <= 0 || imageHeightPx <= 0 || pageWidthPt <= 0f || pageHeightPt <= 0f) {
            return BubbleDetectionEngine.BubbleGrid.EMPTY
        }

        val scaleX = imageWidthPx / pageWidthPt
        val scaleY = imageHeightPx / pageHeightPt

        // A scanned bitmap is ONE physical page. Because each page's questions reuse the same
        // per-page (column, row) coordinates, only the FIRST page's questions can be mapped
        // unambiguously against a single-page scan. Mapping later pages would overlay their
        // coordinates on page 1. Multi-page exams must be scanned one page at a time.
        val firstPageQuestions =
            if (model.questionsPerPage > 0) model.questions.take(model.questionsPerPage)
            else model.questions

        val questions = firstPageQuestions.map { question ->
            val options = question.bubbles.map { bubble ->
                mapBubble(bubble, scaleX, scaleY, imageWidthPx, imageHeightPx)
            }
            BubbleDetectionEngine.QuestionRegions(
                number = question.questionNumber,
                options = options
            )
        }

        return BubbleDetectionEngine.BubbleGrid(questions)
    }

    /**
     * Maps a single [AnswerSheetPrettyPrinter.Bubble]'s point-space bounding box into a
     * clamped, non-degenerate pixel-space [BubbleDetectionEngine.OptionRegion].
     */
    private fun mapBubble(
        bubble: AnswerSheetPrettyPrinter.Bubble,
        scaleX: Float,
        scaleY: Float,
        imageWidthPx: Int,
        imageHeightPx: Int
    ): BubbleDetectionEngine.OptionRegion {
        val leftRaw = ((bubble.centerX - bubble.radius) * scaleX).toInt()
        val rightRaw = ((bubble.centerX + bubble.radius) * scaleX).toInt()
        val topRaw = ((bubble.centerY - bubble.radius) * scaleY).toInt()
        val bottomRaw = ((bubble.centerY + bubble.radius) * scaleY).toInt()

        var left = leftRaw.coerceIn(0, imageWidthPx)
        var right = rightRaw.coerceIn(0, imageWidthPx)
        var top = topRaw.coerceIn(0, imageHeightPx)
        var bottom = bottomRaw.coerceIn(0, imageHeightPx)

        // Guard against degenerate 0-size regions: ensure at least 1px extent, staying
        // within the image bounds.
        if (right <= left) {
            if (left < imageWidthPx) right = left + 1 else left = right - 1
        }
        if (bottom <= top) {
            if (top < imageHeightPx) bottom = top + 1 else top = bottom - 1
        }

        return BubbleDetectionEngine.OptionRegion(
            label = bubble.label,
            left = left,
            top = top,
            right = right,
            bottom = bottom
        )
    }
}
