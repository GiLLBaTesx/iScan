package com.examscanner.premium.viewmodel

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * Unit tests for [DashboardViewModel.intervalFor] — the pure mapping from a
 * dashboard [DateRange] filter to the trend [Interval] granularity (Task 10.5,
 * Req 12.3 / 12.6). These run as plain JVM unit tests (JUnit 5 Jupiter) with no
 * device or database, since [intervalFor] is a pure companion function.
 *
 * Expected mapping (single source of truth in [DashboardViewModel]):
 *  - THIS_WEEK    -> DAILY
 *  - THIS_MONTH   -> WEEKLY
 *  - THIS_QUARTER -> WEEKLY
 *  - THIS_YEAR    -> MONTHLY
 */
class DashboardIntervalMappingTest {

    // Validates: Requirements 12.3, 12.6
    @Test
    fun `THIS_WEEK maps to DAILY`() {
        assertEquals(Interval.DAILY, DashboardViewModel.intervalFor(DateRange.THIS_WEEK))
    }

    // Validates: Requirements 12.3, 12.6
    @Test
    fun `THIS_MONTH maps to WEEKLY`() {
        assertEquals(Interval.WEEKLY, DashboardViewModel.intervalFor(DateRange.THIS_MONTH))
    }

    // Validates: Requirements 12.3, 12.6
    @Test
    fun `THIS_QUARTER maps to WEEKLY`() {
        assertEquals(Interval.WEEKLY, DashboardViewModel.intervalFor(DateRange.THIS_QUARTER))
    }

    // Validates: Requirements 12.3, 12.6
    @Test
    fun `THIS_YEAR maps to MONTHLY`() {
        assertEquals(Interval.MONTHLY, DashboardViewModel.intervalFor(DateRange.THIS_YEAR))
    }

    // Validates: Requirements 12.3, 12.6
    @Test
    fun `every DateRange maps to a non-null Interval`() {
        DateRange.values().forEach { range ->
            assertNotNull(
                DashboardViewModel.intervalFor(range),
                "intervalFor($range) must return a non-null Interval"
            )
        }
    }
}
