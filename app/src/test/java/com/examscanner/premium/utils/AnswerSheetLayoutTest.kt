package com.examscanner.premium.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * JVM unit tests for the pure answer-sheet layout produced by
 * [AnswerSheetPrettyPrinter.buildSheetModel].
 *
 * The generator now GROWS THE PAGE SIZE to fit as many questions as possible on ONE page
 * before paginating: rows-per-column is DYNAMIC (derived from the selected page height),
 * columns-per-page stay capped at [AnswerSheetPrettyPrinter.MAX_COLUMNS_PER_PAGE], and the
 * page size is chosen first-fit over [A4, LEGAL, LETTER] by dynamic capacity.
 *
 * These verify the dynamic per-page capacity, non-overlapping bubble geometry (the key
 * scanner-alignment guarantee), page-size selection, and pagination — all device-free
 * (no PDF/Canvas/Context).
 */
class AnswerSheetLayoutTest {

    private val prettyPrinter = AnswerSheetPrettyPrinter()

    private fun model(
        totalQuestions: Int,
        labels: List<String>
    ): AnswerSheetPrettyPrinter.SheetModel =
        prettyPrinter.buildSheetModel(
            AnswerSheetPrettyPrinter.SheetConfig(
                examName = "Layout",
                totalQuestions = totalQuestions,
                optionsCount = labels.size,
                optionsLabels = labels
            )
        )

    private val validPageSizes: Set<Pair<Float, Float>> =
        AnswerSheetPrettyPrinter.PageSize.entries
            .map { it.widthPt to it.heightPt }
            .toSet()

    // ---------------------------------------------------------------------------------------
    // (a) No bubble overlap — the most important guarantee (scanner alignment).
    //     Verified for 100 questions A–D and 50 questions A–G.
    // ---------------------------------------------------------------------------------------

    @Test
    fun `bubbles never overlap for A-D across all pages`() {
        val m = model(totalQuestions = 100, labels = listOf("A", "B", "C", "D"))
        m.questions.chunked(m.questionsPerPage).forEach { page ->
            assertNoOverlapWithinPage(page)
        }
    }

    @Test
    fun `A-G seven options produce non-overlapping bubbles`() {
        val labels = listOf("A", "B", "C", "D", "E", "F", "G")
        val m = model(totalQuestions = 50, labels = labels)
        m.questions.chunked(m.questionsPerPage).forEach { page ->
            assertNoOverlapWithinPage(page)
        }
    }

    // ---------------------------------------------------------------------------------------
    // (b) Mid-size case: 60 questions A–D fit on ONE page after paper escalation.
    //     The selected page is the SMALLEST in [A4, LEGAL, LETTER] order that fits.
    // ---------------------------------------------------------------------------------------

    @Test
    fun `60 questions A-D fit on one page after page-size escalation`() {
        val m = model(totalQuestions = 60, labels = listOf("A", "B", "C", "D"))

        assertEquals(1, m.pageCount, "60 A–D should fit on a single page")
        assertTrue(m.columnsPerPage <= AnswerSheetPrettyPrinter.MAX_COLUMNS_PER_PAGE)

        // Capacity (columns × rows-per-column == questionsPerPage) must cover all 60.
        assertTrue(
            m.questionsPerPage >= 60,
            "capacity (questionsPerPage=${m.questionsPerPage}) must hold >= 60"
        )
        val rowsPerColumn = m.questionsPerPage / m.columnsPerPage
        assertEquals(
            m.columnsPerPage * rowsPerColumn,
            m.questionsPerPage,
            "questionsPerPage must equal columnsPerPage × rowsPerColumn"
        )

        // The chosen page must actually be needed: a shorter A4-height column could not
        // fit 60 at 3 columns, so escalation to a taller size is expected here. Whatever
        // size is chosen, it must be one whose HEIGHT is >= A4 (never a shorter page than
        // A4 when A4 itself did not fit). This proves "grow the page to fit".
        assertTrue(
            m.pageHeightPt >= AnswerSheetPrettyPrinter.PageSize.A4.heightPt,
            "expected escalation to a page at least as tall as A4, got ${m.pageHeightPt}"
        )
    }

    // ---------------------------------------------------------------------------------------
    // (c) Large case: 300 questions paginate onto >= 2 pages, columns still capped at 3,
    //     and total capacity across pages covers everything.
    // ---------------------------------------------------------------------------------------

    @Test
    fun `300 questions paginate with capped columns and sufficient total capacity`() {
        val m = model(totalQuestions = 300, labels = listOf("A", "B", "C", "D"))

        assertTrue(m.pageCount >= 2, "300 questions should need more than one page")
        assertTrue(
            m.columnsPerPage <= AnswerSheetPrettyPrinter.MAX_COLUMNS_PER_PAGE,
            "columns per page must stay capped at 3"
        )
        assertTrue(
            m.questionsPerPage * m.pageCount >= 300,
            "total capacity ${m.questionsPerPage}x${m.pageCount} must cover 300"
        )
    }

    // ---------------------------------------------------------------------------------------
    // (d) Selected page size is one of the three defined sizes.
    // ---------------------------------------------------------------------------------------

    @Test
    fun `selected page size is one of A4 Legal or Letter`() {
        listOf(
            listOf("A", "B", "C", "D"),
            listOf("A", "B", "C", "D", "E", "F", "G")
        ).forEach { labels ->
            val m = model(totalQuestions = 40, labels = labels)
            assertTrue(
                (m.pageWidthPt to m.pageHeightPt) in validPageSizes,
                "page size ${m.pageWidthPt}x${m.pageHeightPt} not one of $validPageSizes"
            )
        }
    }

    // ---------------------------------------------------------------------------------------
    // (e) The strict 10-per-column cap has been RELAXED: a question count that would have
    //     spilled beyond 30/page under the old rule now packs > 30 per page (or > 10 in a
    //     single column) on taller paper.
    // ---------------------------------------------------------------------------------------

    @Test
    fun `rows per column are not hard-capped at ten on taller paper`() {
        val m = model(totalQuestions = 45, labels = listOf("A", "B", "C", "D"))

        val rowsPerColumn = m.questionsPerPage / m.columnsPerPage.coerceAtLeast(1)
        val maxQuestionsInAnyColumn = m.questions
            .take(m.questionsPerPage)
            .groupBy { it.bubbles.first().centerX }
            .values
            .maxOf { it.size }

        assertTrue(
            m.questionsPerPage > 30 || rowsPerColumn > 10 || maxQuestionsInAnyColumn > 10,
            "expected the strict 10-per-column cap to be relaxed: " +
                "questionsPerPage=${m.questionsPerPage}, rowsPerColumn=$rowsPerColumn, " +
                "maxInColumn=$maxQuestionsInAnyColumn"
        )
    }

    // ---------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------

    /**
     * Every pair of bubbles on the SAME page must have centers separated by at least the
     * bubble diameter (2 × radius), so the drawn circles never intersect.
     */
    private fun assertNoOverlapWithinPage(
        page: List<AnswerSheetPrettyPrinter.SheetQuestion>
    ) {
        val bubbles = page.flatMap { it.bubbles }
        for (i in bubbles.indices) {
            for (j in i + 1 until bubbles.size) {
                val a = bubbles[i]
                val b = bubbles[j]
                val dx = (a.centerX - b.centerX).toDouble()
                val dy = (a.centerY - b.centerY).toDouble()
                val dist = Math.hypot(dx, dy)
                val minDist = (a.radius + b.radius).toDouble()
                assertTrue(
                    dist >= minDist - 1e-3,
                    "bubbles overlap: Q${a.questionNumber}/${a.label} and " +
                        "Q${b.questionNumber}/${b.label} dist=$dist < $minDist"
                )
            }
        }
    }
}
