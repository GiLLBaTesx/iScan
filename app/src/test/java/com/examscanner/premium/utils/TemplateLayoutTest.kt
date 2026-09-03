package com.examscanner.premium.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * JVM unit tests for the up-to-3-columns "fit-then-escalate" layout produced by
 * [TemplatePDFGenerator.resolveLayout].
 *
 * These verify the escalation ladder (add columns / tighten rows -> grow paper -> 1-up ->
 * paginate), the legibility floor on row height, the column ceiling (MAX_COLUMNS = 3), and
 * determinism — all device-free (no PDF/Canvas/Context).
 */
class TemplateLayoutTest {

    // ---------------------------------------------------------------------------------------
    // Common small case: 20 questions, 4 choices -> fits 2-up on Letter, one page.
    // ---------------------------------------------------------------------------------------

    @Test
    fun `20 questions 4 choices fits 2-up on Letter single page`() {
        val layout = TemplatePDFGenerator.resolveLayout(totalQuestions = 20, choicesPerQuestion = 4)

        assertEquals(TemplatePDFGenerator.PaperSize.LETTER, layout.paperSize, "should not escalate paper")
        assertTrue(layout.is2up, "small count fits 2-up")
        assertTrue(layout.columnsPerTemplate <= TemplatePDFGenerator.MAX_COLUMNS, "columns must be <= 3")
        assertEquals(1, layout.pageCount)
        assertTrue(layout.capacityPerTemplate >= 20, "capacity must hold all 20 questions")
        assertTrue(layout.rowHeight >= TemplatePDFGenerator.MIN_ROW_HEIGHT)
    }

    // ---------------------------------------------------------------------------------------
    // 60 questions must FIT (no overlap) on a single 2-up page by escalating: either more
    // columns (>2) or a larger paper size than Letter. Columns still capped at 3.
    // ---------------------------------------------------------------------------------------

    @Test
    fun `60 questions 4 choices fits by escalating columns or paper on one page`() {
        val layout = TemplatePDFGenerator.resolveLayout(totalQuestions = 60, choicesPerQuestion = 4)

        assertTrue(layout.columnsPerTemplate <= TemplatePDFGenerator.MAX_COLUMNS, "columns must be <= 3")
        assertTrue(layout.rowHeight >= TemplatePDFGenerator.MIN_ROW_HEIGHT)
        assertEquals(1, layout.pageCount, "60 questions should fit on one page")
        assertTrue(layout.capacityPerTemplate >= 60, "capacity must hold all 60 questions")

        // It had to escalate beyond the plain 2-col Letter default: more columns or bigger paper.
        val escalated = layout.columnsPerTemplate > 2 ||
            layout.paperSize != TemplatePDFGenerator.PaperSize.LETTER
        assertTrue(escalated, "60 questions should require more columns or larger paper")
    }

    // ---------------------------------------------------------------------------------------
    // Large count: 300 questions, 5 choices -> escalates to 1-up and/or multi-page. Columns
    // capped at 3; total capacity across all pages must hold every question.
    // ---------------------------------------------------------------------------------------

    @Test
    fun `300 questions 5 choices escalates to 1-up and or multi-page with full capacity`() {
        val layout = TemplatePDFGenerator.resolveLayout(totalQuestions = 300, choicesPerQuestion = 5)

        assertTrue(layout.columnsPerTemplate <= TemplatePDFGenerator.MAX_COLUMNS, "columns must be <= 3")
        assertTrue(layout.rowHeight >= TemplatePDFGenerator.MIN_ROW_HEIGHT)
        assertTrue(!layout.is2up || layout.pageCount >= 2, "must go 1-up or paginate for 300 questions")

        val totalCapacity = layout.capacityPerTemplate.toLong() * layout.pageCount
        assertTrue(
            totalCapacity >= 300,
            "total capacity $totalCapacity across ${layout.pageCount} pages < 300 (rows dropped)"
        )
    }

    // ---------------------------------------------------------------------------------------
    // Across many inputs: columns stay within 1..MAX_COLUMNS and the legibility floor holds.
    // ---------------------------------------------------------------------------------------

    @Test
    fun `columns within max and row height respects floor across many inputs`() {
        val questionCounts = listOf(1, 10, 20, 40, 60, 100, 150, 300, 500)
        val choiceCounts = listOf(2, 3, 4, 5, 6)

        for (q in questionCounts) {
            for (c in choiceCounts) {
                val layout = TemplatePDFGenerator.resolveLayout(q, c)
                assertTrue(
                    layout.columnsPerTemplate in 1..TemplatePDFGenerator.MAX_COLUMNS,
                    "columns ${layout.columnsPerTemplate} out of 1..${TemplatePDFGenerator.MAX_COLUMNS} for q=$q c=$c"
                )
                assertTrue(
                    layout.rowHeight >= TemplatePDFGenerator.MIN_ROW_HEIGHT,
                    "rowHeight ${layout.rowHeight} < MIN for q=$q c=$c"
                )
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // Determinism: same inputs -> equal layout.
    // ---------------------------------------------------------------------------------------

    @Test
    fun `resolveLayout is deterministic`() {
        val a = TemplatePDFGenerator.resolveLayout(60, 4)
        val b = TemplatePDFGenerator.resolveLayout(60, 4)
        assertEquals(a, b)

        val c = TemplatePDFGenerator.resolveLayout(300, 5)
        val d = TemplatePDFGenerator.resolveLayout(300, 5)
        assertEquals(c, d)
    }
}
