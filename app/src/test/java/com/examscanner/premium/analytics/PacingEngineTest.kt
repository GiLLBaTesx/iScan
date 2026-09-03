package com.examscanner.premium.analytics

import com.examscanner.premium.analytics.PacingEngine.CoverageStatus
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import net.jqwik.api.constraints.IntRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [PacingEngine]'s pure companion functions (Task 7.4,
 * Req 9.2 / 9.3 / 9.5). These run as plain JVM unit tests (JUnit 5 Jupiter,
 * jqwik for the property parts) with no device or database.
 *
 * Covered pure functions:
 *  - [PacingEngine.determineStatus]      coverage-status classification (Req 9.2, 9.5)
 *  - [PacingEngine.recommendedWeekFor]   even schedule spread across the quarter (Req 9.2)
 *  - [PacingEngine.isAssessed]           which statuses count toward coverage
 *  - [PacingEngine.coveragePercentage]   coverage % math, clamped to [0,100] (Req 9.3)
 */
class PacingEngineTest {

    // ---------------------------------------------------------------------------------
    // determineStatus — coverage-status classification (Req 9.2, 9.5)
    // ---------------------------------------------------------------------------------

    // Validates: Requirements 9.2, 9.5
    @Test
    fun `assessment coverage maps to FULLY_ASSESSED`() {
        val status = PacingEngine.determineStatus(
            coverageType = PacingEngine.COVERAGE_TYPE_ASSESSMENT,
            recommendedWeek = 3,
            currentWeek = 8
        )
        assertEquals(CoverageStatus.FULLY_ASSESSED, status)
    }

    // Validates: Requirements 9.2, 9.5
    @Test
    fun `manual coverage maps to PARTIALLY_ASSESSED`() {
        val status = PacingEngine.determineStatus(
            coverageType = PacingEngine.COVERAGE_TYPE_MANUAL,
            recommendedWeek = 3,
            currentWeek = 8
        )
        assertEquals(CoverageStatus.PARTIALLY_ASSESSED, status)
    }

    // Validates: Requirements 9.2, 9.5
    @Test
    fun `skipped coverage maps to NOT_ASSESSED even when past due`() {
        // Skipped MELCs are recorded so they no longer surface as behind-schedule,
        // even though currentWeek is well past the recommended week.
        val status = PacingEngine.determineStatus(
            coverageType = PacingEngine.COVERAGE_TYPE_SKIPPED,
            recommendedWeek = 2,
            currentWeek = 10
        )
        assertEquals(CoverageStatus.NOT_ASSESSED, status)
    }

    // Validates: Requirements 9.5
    @Test
    fun `no coverage past the recommended week is BEHIND_SCHEDULE`() {
        val status = PacingEngine.determineStatus(
            coverageType = null,
            recommendedWeek = 4,
            currentWeek = 5
        )
        assertEquals(CoverageStatus.BEHIND_SCHEDULE, status)
    }

    // Validates: Requirements 9.5
    @Test
    fun `no coverage on the recommended week is NOT_ASSESSED (on time)`() {
        val status = PacingEngine.determineStatus(
            coverageType = null,
            recommendedWeek = 4,
            currentWeek = 4
        )
        assertEquals(CoverageStatus.NOT_ASSESSED, status)
    }

    // Validates: Requirements 9.5
    @Test
    fun `no coverage before the recommended week is NOT_ASSESSED`() {
        val status = PacingEngine.determineStatus(
            coverageType = null,
            recommendedWeek = 6,
            currentWeek = 2
        )
        assertEquals(CoverageStatus.NOT_ASSESSED, status)
    }

    // Validates: Requirements 9.5
    @Test
    fun `unknown coverage type falls through to schedule-based classification`() {
        // An unrecognized type behaves like "no coverage": behind schedule when past due.
        assertEquals(
            CoverageStatus.BEHIND_SCHEDULE,
            PacingEngine.determineStatus("Something", recommendedWeek = 2, currentWeek = 5)
        )
        assertEquals(
            CoverageStatus.NOT_ASSESSED,
            PacingEngine.determineStatus("Something", recommendedWeek = 5, currentWeek = 2)
        )
    }

    // ---------------------------------------------------------------------------------
    // recommendedWeekFor — even schedule spread (Req 9.2)
    // ---------------------------------------------------------------------------------

    // Validates: Requirements 9.2
    @Test
    fun `empty or single MELC set maps to week one`() {
        assertEquals(1, PacingEngine.recommendedWeekFor(index = 0, total = 0))
        assertEquals(1, PacingEngine.recommendedWeekFor(index = 0, total = 1))
    }

    // Validates: Requirements 9.2
    @Test
    fun `twenty MELCs spread two per week across ten weeks`() {
        // 20 MELCs / 10 weeks = 2 MELCs per week; index 0,1 -> week 1, index 2,3 -> week 2, ...
        val total = 20
        assertEquals(1, PacingEngine.recommendedWeekFor(0, total))
        assertEquals(1, PacingEngine.recommendedWeekFor(1, total))
        assertEquals(2, PacingEngine.recommendedWeekFor(2, total))
        assertEquals(5, PacingEngine.recommendedWeekFor(9, total))
        assertEquals(10, PacingEngine.recommendedWeekFor(19, total))
    }

    // Validates: Requirements 9.2
    @Test
    fun `first MELC is week one and last MELC is within the quarter`() {
        val total = 37
        assertEquals(1, PacingEngine.recommendedWeekFor(0, total))
        val lastWeek = PacingEngine.recommendedWeekFor(total - 1, total)
        assertTrue(
            lastWeek in 1..PacingEngine.WEEKS_PER_QUARTER,
            "last recommended week $lastWeek not within 1..${PacingEngine.WEEKS_PER_QUARTER}"
        )
    }

    // Validates: Requirements 9.2
    //
    // For any cohort size, every recommended week is within 1..WEEKS_PER_QUARTER and
    // the schedule is monotonically non-decreasing across ascending indices.
    @Property(tries = 500)
    fun `schedule is bounded and monotonic across the quarter`(
        @ForAll @IntRange(min = 1, max = 200) total: Int
    ) {
        var previous = 0
        for (index in 0 until total) {
            val week = PacingEngine.recommendedWeekFor(index, total)
            assertTrue(
                week in 1..PacingEngine.WEEKS_PER_QUARTER,
                "week $week out of range for index=$index total=$total"
            )
            assertTrue(
                week >= previous,
                "schedule not monotonic: week $week < previous $previous at index=$index total=$total"
            )
            previous = week
        }
    }

    // ---------------------------------------------------------------------------------
    // isAssessed — which statuses count toward coverage
    // ---------------------------------------------------------------------------------

    // Validates: Requirements 9.3
    @Test
    fun `only fully and partially assessed statuses count as assessed`() {
        assertTrue(PacingEngine.isAssessed(CoverageStatus.FULLY_ASSESSED))
        assertTrue(PacingEngine.isAssessed(CoverageStatus.PARTIALLY_ASSESSED))
        assertFalse(PacingEngine.isAssessed(CoverageStatus.NOT_ASSESSED))
        assertFalse(PacingEngine.isAssessed(CoverageStatus.BEHIND_SCHEDULE))
    }

    // ---------------------------------------------------------------------------------
    // coveragePercentage — assessed/total*100, clamped (Req 9.3)
    // ---------------------------------------------------------------------------------

    // Validates: Requirements 9.3
    @Test
    fun `coverage percentage computes assessed over total times one hundred`() {
        assertEquals(50f, PacingEngine.coveragePercentage(assessed = 10, total = 20), 1e-3f)
        assertEquals(100f, PacingEngine.coveragePercentage(assessed = 7, total = 7), 1e-3f)
        assertEquals(0f, PacingEngine.coveragePercentage(assessed = 0, total = 8), 1e-3f)
        assertEquals(25f, PacingEngine.coveragePercentage(assessed = 2, total = 8), 1e-3f)
    }

    // Validates: Requirements 9.3
    @Test
    fun `coverage percentage is zero when there are no MELCs`() {
        assertEquals(0f, PacingEngine.coveragePercentage(assessed = 0, total = 0), 1e-3f)
        // Defensive: even a spurious positive assessed count with zero total returns 0.
        assertEquals(0f, PacingEngine.coveragePercentage(assessed = 5, total = 0), 1e-3f)
    }

    // Validates: Requirements 9.3
    //
    // For any 0 <= assessed <= total (total >= 1), coverage% equals assessed/total*100
    // and is always within [0, 100].
    @Property(tries = 1000)
    fun `coverage percentage is bounded and matches the formula`(
        @ForAll("assessedTotal") pair: Pair<Int, Int>
    ) {
        val (assessed, total) = pair
        val actual = PacingEngine.coveragePercentage(assessed, total)
        val expected = assessed.toFloat() / total.toFloat() * 100f

        assertEquals(expected, actual, 1e-3f, "assessed=$assessed total=$total")
        assertTrue(actual in 0f..100f, "coverage $actual out of bounds for $assessed/$total")
    }

    @Provide
    fun assessedTotal(): Arbitrary<Pair<Int, Int>> =
        Arbitraries.integers().between(1, 500).flatMap { total ->
            Arbitraries.integers().between(0, total).map { assessed -> assessed to total }
        }
}
