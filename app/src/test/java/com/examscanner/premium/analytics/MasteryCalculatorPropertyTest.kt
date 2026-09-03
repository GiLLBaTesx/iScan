package com.examscanner.premium.analytics

import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import net.jqwik.api.constraints.FloatRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Property-based tests for [MasteryCalculator]'s pure, DB-free logic.
 *
 * These are JVM unit tests (jqwik on the JUnit 5 platform) and run WITHOUT a
 * device. They exercise the two design correctness properties that map to the
 * mastery service:
 *
 *  - Property 1 (Req 2.5): band classification via [MasteryCalculator.determineMasteryLevel]
 *  - Property 2 (Req 2.4): bounded aggregation via [MasteryCalculator.aggregatePercentage]
 */
class MasteryCalculatorPropertyTest {

    // Feature: offline-assessment-transformation, Property 1: Mastery level classification matches defined bands
    // Validates: Requirements 2.5
    //
    // For any percentage 0-100, determineMasteryLevel returns:
    //   Developing   for 0.0   .. 74.99
    //   Approaching  for 75.0  .. 79.99
    //   Proficient   for 80.0  .. 89.99
    //   Advanced     for 90.0  .. 100.0
    @Property(tries = 1000)
    fun `mastery level classification matches defined bands`(
        @ForAll @FloatRange(min = 0f, max = 100f) percentage: Float
    ) {
        val expected = when {
            percentage >= 90f -> MasteryCalculator.LEVEL_ADVANCED
            percentage >= 80f -> MasteryCalculator.LEVEL_PROFICIENT
            percentage >= 75f -> MasteryCalculator.LEVEL_APPROACHING
            else -> MasteryCalculator.LEVEL_DEVELOPING
        }

        val actual = MasteryCalculator.determineMasteryLevel(percentage)

        assertEquals(expected, actual, "percentage=$percentage")
        // The result is always one of the four defined bands.
        assertTrue(
            actual in setOf(
                MasteryCalculator.LEVEL_DEVELOPING,
                MasteryCalculator.LEVEL_APPROACHING,
                MasteryCalculator.LEVEL_PROFICIENT,
                MasteryCalculator.LEVEL_ADVANCED
            ),
            "unexpected band '$actual' for percentage=$percentage"
        )
    }

    // Feature: offline-assessment-transformation, Property 1: band boundaries are inclusive at the lower edge
    // Validates: Requirements 2.5
    //
    // Explicit checks of the exact band edges the four inequalities hinge on.
    @Property(tries = 100)
    fun `band boundary edges classify exactly`(
        @ForAll("bandEdges") edge: Float
    ) {
        val level = MasteryCalculator.determineMasteryLevel(edge)
        val expected = when {
            edge >= 90f -> MasteryCalculator.LEVEL_ADVANCED
            edge >= 80f -> MasteryCalculator.LEVEL_PROFICIENT
            edge >= 75f -> MasteryCalculator.LEVEL_APPROACHING
            else -> MasteryCalculator.LEVEL_DEVELOPING
        }
        assertEquals(expected, level, "edge=$edge")
    }

    @Provide
    fun bandEdges(): Arbitrary<Float> = Arbitraries.of(
        0f, 74.99f, 74.999f, 75f, 79.99f, 79.999f, 80f, 89.99f, 89.999f, 90f, 100f
    )

    // Feature: offline-assessment-transformation, Property 2: Mastery percentage is a bounded aggregation
    // Validates: Requirements 2.4
    //
    // For any set of (earned, possible) pairs with 0 <= earned <= possible and
    // possible >= 0, aggregatePercentage(pairs) equals (Sum(earned)/Sum(possible))*100
    // when Sum(possible) > 0 (else 0), and is always within [0, 100].
    @Property(tries = 1000)
    fun `mastery percentage is a bounded aggregation`(
        @ForAll("scorePairs") scores: List<MasteryCalculator.MelcScore>
    ) {
        val totalEarned = scores.sumOf { it.earned }
        val totalPossible = scores.sumOf { it.possible }

        val actual = MasteryCalculator.aggregatePercentage(scores)

        // Oracle: replicate the exact reduction the formula specifies.
        val expected = if (totalPossible > 0) {
            (totalEarned.toFloat() / totalPossible.toFloat()) * 100f
        } else {
            0f
        }

        assertEquals(expected, actual, "earned=$totalEarned possible=$totalPossible")

        // Boundedness: always within the closed range [0, 100].
        assertTrue(
            actual in 0f..100f,
            "aggregate $actual out of bounds for earned=$totalEarned possible=$totalPossible"
        )
    }

    // Feature: offline-assessment-transformation, Property 2: empty and all-zero-possible aggregate to 0
    // Validates: Requirements 2.4
    @Property(tries = 100)
    fun `aggregation with no possible points is zero`(
        @ForAll("zeroPossiblePairs") scores: List<MasteryCalculator.MelcScore>
    ) {
        assertEquals(0f, MasteryCalculator.aggregatePercentage(scores))
    }

    /**
     * Generates lists of well-formed [MasteryCalculator.MelcScore] pairs where
     * `possible >= 0` and `0 <= earned <= possible` (the domain invariant a
     * real answer key produces: you cannot earn more than the points on offer).
     */
    @Provide
    fun scorePairs(): Arbitrary<List<MasteryCalculator.MelcScore>> {
        val pair: Arbitrary<MasteryCalculator.MelcScore> =
            Arbitraries.integers().between(0, 1000).flatMap { possible ->
                Arbitraries.integers().between(0, possible).map { earned ->
                    MasteryCalculator.MelcScore(earned = earned, possible = possible)
                }
            }
        return pair.list().ofMinSize(0).ofMaxSize(50)
    }

    /** Pairs whose possible points are all zero (division guard must return 0). */
    @Provide
    fun zeroPossiblePairs(): Arbitrary<List<MasteryCalculator.MelcScore>> =
        Arbitraries.just(MasteryCalculator.MelcScore(earned = 0, possible = 0))
            .list().ofMinSize(0).ofMaxSize(10)
}
