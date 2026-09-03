package com.examscanner.premium.utils

import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for [ReportGenerator.computeClassStatistics] (Task 7.2, Req 5.4).
 *
 * [ReportGenerator.computeClassStatistics] is a pure function: it takes a list of
 * percentage scores and returns [ReportGenerator.ClassStatistics] with
 * count / mean / median / highest / lowest / population standard deviation. It has
 * no Android, PDF, or repository dependencies, so it runs as a plain JVM unit test
 * (JUnit 5 Jupiter) without a device.
 *
 * The standard deviation is the POPULATION standard deviation (variance divides by N,
 * not N-1) — confirmed by the source, which averages the squared deviations.
 *
 * The [ReportGenerator] instance is only needed to reach the method; the method itself
 * reads none of the constructor-injected collaborators. Since the real constructor
 * requires an Android [android.content.Context] (unavailable off-device), we allocate
 * an uninitialized instance and invoke only the pure [computeClassStatistics] on it.
 * No mocking library is required.
 */
class ReportStatisticsTest {

    // computeClassStatistics reads no instance fields, so an uninitialized instance is
    // sufficient to reach the pure logic without a Context/DB.
    private val generator: ReportGenerator = allocateUninitialized(ReportGenerator::class.java)

    private val tolerance = 1e-3f

    // ---------------------------------------------------------------------------------
    // Example-based tests with hand-computed expected values (Req 5.4)
    // ---------------------------------------------------------------------------------

    // Validates: Requirements 5.4
    @Test
    fun `odd-sized cohort computes hand-verified statistics`() {
        // Cohort: [50, 70, 80, 90, 100]  (n = 5, odd)
        //   mean   = (50+70+80+90+100)/5 = 390/5 = 78.0
        //   median = middle of sorted    = 80.0
        //   high   = 100, low = 50
        //   population variance = mean of squared deviations from 78:
        //     (-28)^2 + (-8)^2 + 2^2 + 12^2 + 22^2 = 784+64+4+144+484 = 1480
        //     variance = 1480 / 5 = 296  -> std dev = sqrt(296) ≈ 17.20465
        val stats = generator.computeClassStatistics(listOf(50f, 70f, 80f, 90f, 100f))

        assertEquals(5, stats.count)
        assertEquals(78.0f, stats.mean, tolerance)
        assertEquals(80.0f, stats.median, tolerance)
        assertEquals(100.0f, stats.highest, tolerance)
        assertEquals(50.0f, stats.lowest, tolerance)
        assertEquals(17.20465f, stats.standardDeviation, 1e-2f)
    }

    // Validates: Requirements 5.4
    @Test
    fun `even-sized cohort averages the two middle values for the median`() {
        // Cohort: [60, 70, 80, 90]  (n = 4, even), intentionally unsorted on input.
        //   mean   = (60+70+80+90)/4 = 300/4 = 75.0
        //   median = average of two middle values of sorted [60,70,80,90] = (70+80)/2 = 75.0
        //   high   = 90, low = 60
        //   population variance: deviations from 75 = -15,-5,5,15 ->
        //     225+25+25+225 = 500; variance = 500/4 = 125 -> std dev = sqrt(125) ≈ 11.18034
        val stats = generator.computeClassStatistics(listOf(90f, 60f, 80f, 70f))

        assertEquals(4, stats.count)
        assertEquals(75.0f, stats.mean, tolerance)
        assertEquals(75.0f, stats.median, tolerance)
        assertEquals(90.0f, stats.highest, tolerance)
        assertEquals(60.0f, stats.lowest, tolerance)
        assertEquals(11.18034f, stats.standardDeviation, 1e-2f)
    }

    // Validates: Requirements 5.4
    @Test
    fun `single-element cohort has zero spread`() {
        val stats = generator.computeClassStatistics(listOf(88f))

        assertEquals(1, stats.count)
        assertEquals(88f, stats.mean, tolerance)
        assertEquals(88f, stats.median, tolerance)
        assertEquals(88f, stats.highest, tolerance)
        assertEquals(88f, stats.lowest, tolerance)
        assertEquals(0f, stats.standardDeviation, tolerance)
    }

    // Validates: Requirements 5.4
    @Test
    fun `identical scores yield zero standard deviation`() {
        val stats = generator.computeClassStatistics(listOf(75f, 75f, 75f, 75f))

        assertEquals(4, stats.count)
        assertEquals(75f, stats.mean, tolerance)
        assertEquals(75f, stats.median, tolerance)
        assertEquals(75f, stats.highest, tolerance)
        assertEquals(75f, stats.lowest, tolerance)
        assertEquals(0f, stats.standardDeviation, tolerance)
    }

    // Validates: Requirements 5.4
    @Test
    fun `empty cohort returns zeroed statistics with count zero`() {
        val stats = generator.computeClassStatistics(emptyList())

        assertEquals(0, stats.count)
        assertEquals(0f, stats.mean, tolerance)
        assertEquals(0f, stats.median, tolerance)
        assertEquals(0f, stats.highest, tolerance)
        assertEquals(0f, stats.lowest, tolerance)
        assertEquals(0f, stats.standardDeviation, tolerance)
    }

    // ---------------------------------------------------------------------------------
    // Property-based invariants (jqwik) — reinforce the example-based tests (Req 5.4)
    // ---------------------------------------------------------------------------------

    // Validates: Requirements 5.4
    //
    // For any non-empty cohort: highest >= lowest, lowest <= mean <= highest,
    // lowest <= median <= highest, count matches size, std dev is non-negative, and
    // highest/lowest equal the max/min of the input.
    @Property(tries = 500)
    fun `statistics are internally consistent for any non-empty cohort`(
        @ForAll("cohorts") percentages: List<Float>
    ) {
        val stats = generator.computeClassStatistics(percentages)

        assertEquals(percentages.size, stats.count)
        assertTrue(stats.highest >= stats.lowest, "highest < lowest for $percentages")
        assertTrue(
            stats.mean >= stats.lowest - tolerance && stats.mean <= stats.highest + tolerance,
            "mean ${stats.mean} outside [${stats.lowest}, ${stats.highest}]"
        )
        assertTrue(
            stats.median >= stats.lowest - tolerance && stats.median <= stats.highest + tolerance,
            "median ${stats.median} outside [${stats.lowest}, ${stats.highest}]"
        )
        assertTrue(stats.standardDeviation >= -tolerance, "negative std dev ${stats.standardDeviation}")
        assertEquals(percentages.max(), stats.highest, tolerance)
        assertEquals(percentages.min(), stats.lowest, tolerance)
    }

    @Provide
    fun cohorts(): Arbitrary<List<Float>> =
        Arbitraries.floats().between(0f, 100f).list().ofMinSize(1).ofMaxSize(60)

    private companion object {
        /**
         * Allocates an instance without running any constructor, using the JDK's
         * `sun.misc.Unsafe`. Safe here because the only method invoked on the returned
         * instance ([ReportGenerator.computeClassStatistics]) is pure and reads no fields.
         */
        @Suppress("UNCHECKED_CAST")
        fun <T> allocateUninitialized(clazz: Class<T>): T {
            val unsafeClass = Class.forName("sun.misc.Unsafe")
            val theUnsafe = unsafeClass.getDeclaredField("theUnsafe").apply { isAccessible = true }
            val unsafe = theUnsafe.get(null)
            val allocate = unsafeClass.getMethod("allocateInstance", Class::class.java)
            return allocate.invoke(unsafe, clazz) as T
        }
    }
}
