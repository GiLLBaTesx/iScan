package com.examscanner.premium.scanner

import com.examscanner.premium.utils.AnswerSheetPrettyPrinter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * JVM unit tests for [BubbleGridMapper] — the pure point→pixel mapper that turns a printed
 * [AnswerSheetPrettyPrinter.SheetModel] into a samplable [BubbleDetectionEngine.BubbleGrid].
 *
 * These run WITHOUT a device or Android bitmap: the mapper and the pretty printer's
 * buildSheetModel are both pure. 1080×1527 px is used as a stand-in scan resolution because
 * its aspect ratio ≈ A4 (595 × 842 pt).
 */
class BubbleGridMapperTest {

    private val prettyPrinter = AnswerSheetPrettyPrinter()

    private val imageWidth = 1080
    private val imageHeight = 1527

    private fun buildModel(): AnswerSheetPrettyPrinter.SheetModel =
        prettyPrinter.buildSheetModel(
            AnswerSheetPrettyPrinter.SheetConfig(
                examName = "T",
                totalQuestions = 10,
                optionsCount = 4,
                optionsLabels = listOf("A", "B", "C", "D"),
            )
        )

    @Test
    fun `grid mirrors model questions and option counts`() {
        val model = buildModel()
        val grid = BubbleGridMapper.buildGrid(
            model,
            imageWidthPx = imageWidth,
            imageHeightPx = imageHeight,
            pageWidthPt = 595f,
            pageHeightPt = 842f,
        )

        assertEquals(model.questions.size, grid.questions.size)
        grid.questions.forEach { q ->
            assertEquals(4, q.options.size, "question ${q.number} should have 4 options")
        }
    }

    @Test
    fun `every region is within image bounds and non-degenerate`() {
        val model = buildModel()
        val grid = BubbleGridMapper.buildGrid(model, imageWidth, imageHeight, 595f, 842f)

        grid.questions.forEach { q ->
            q.options.forEach { r ->
                assertTrue(r.left in 0..imageWidth, "left in bounds: $r")
                assertTrue(r.right in 0..imageWidth, "right in bounds: $r")
                assertTrue(r.top in 0..imageHeight, "top in bounds: $r")
                assertTrue(r.bottom in 0..imageHeight, "bottom in bounds: $r")
                assertTrue(r.right > r.left, "right > left: $r")
                assertTrue(r.bottom > r.top, "bottom > top: $r")
            }
        }
    }

    @Test
    fun `option regions are monotonic left to right within a question`() {
        val model = buildModel()
        val grid = BubbleGridMapper.buildGrid(model, imageWidth, imageHeight, 595f, 842f)

        grid.questions.forEach { q ->
            val byLabel = q.options.associateBy { it.label }
            val a = byLabel.getValue("A")
            val b = byLabel.getValue("B")
            val c = byLabel.getValue("C")
            val d = byLabel.getValue("D")
            assertTrue(b.left > a.left, "B right of A in question ${q.number}")
            assertTrue(c.left > b.left, "C right of B in question ${q.number}")
            assertTrue(d.left > c.left, "D right of C in question ${q.number}")
        }
    }

    @Test
    fun `non-positive image dimensions yield an empty grid`() {
        val model = buildModel()
        assertEquals(0, BubbleGridMapper.buildGrid(model, 0, imageHeight).questions.size)
        assertEquals(0, BubbleGridMapper.buildGrid(model, imageWidth, 0).questions.size)
    }
}
