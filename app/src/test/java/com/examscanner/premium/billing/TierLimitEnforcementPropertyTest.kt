package com.examscanner.premium.billing

import com.examscanner.premium.billing.SubscriptionManager.Companion.FREE_LIMITS
import com.examscanner.premium.billing.SubscriptionManager.Companion.PREMIUM_LIMITS
import com.examscanner.premium.billing.SubscriptionManager.Companion.isFeatureAllowed
import com.examscanner.premium.billing.SubscriptionManager.Companion.isWithinLimit
import com.examscanner.premium.billing.SubscriptionManager.LimitedOperation
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import net.jqwik.api.constraints.IntRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Property-based tests for [SubscriptionManager]'s pure, billing-free tier-limit logic.
 *
 * These are JVM unit tests (jqwik on the JUnit 5 platform) and run WITHOUT a device,
 * a BillingClient, or a database. They exercise the design correctness property that
 * maps to subscription gating:
 *
 *  - Property 12 (Req 15.1, 15.2, 15.3, 16.4): count-based tier limit enforcement via
 *    [SubscriptionManager.isWithinLimit], plus feature gating via
 *    [SubscriptionManager.isFeatureAllowed].
 *
 * Free tier: 3 subjects / 5 exams per subject / 30 scans per exam.
 * Premium tier: Int.MAX_VALUE for all count-based limits (always allowed for realistic counts).
 */
class TierLimitEnforcementPropertyTest {

    // Feature: offline-assessment-transformation, Property 12: Tier limit enforcement
    // Validates: Requirements 15.1, 15.2, 15.3, 16.4
    //
    // For any count-based operation and any current count, on the FREE tier isWithinLimit
    // returns true iff the count is strictly below that operation's Free limit
    // (subjects < 3, exams < 5, scans < 30). On PREMIUM the limit is Int.MAX_VALUE, so any
    // realistic count is always allowed.
    @Property(tries = 1000)
    fun `count-based limit is enforced iff count is below the tier limit`(
        @ForAll("countBasedOperations") operation: LimitedOperation,
        @ForAll @IntRange(min = 0, max = 100_000) currentCount: Int
    ) {
        // FREE: expected limit per operation type.
        val freeLimit = when (operation) {
            is LimitedOperation.CreateSubject -> FREE_LIMITS.maxSubjects
            is LimitedOperation.CreateExam -> FREE_LIMITS.maxExamsPerSubject
            is LimitedOperation.ScanSheet -> FREE_LIMITS.maxScansPerExam
            else -> error("non-count operation: $operation")
        }

        val freeActual = isWithinLimit(operation, currentCount, FREE_LIMITS)
        assertEquals(
            currentCount < freeLimit,
            freeActual,
            "FREE $operation count=$currentCount limit=$freeLimit"
        )

        // PREMIUM: always allowed for realistic (non-MAX_VALUE) counts.
        val premiumActual = isWithinLimit(operation, currentCount, PREMIUM_LIMITS)
        assertTrue(
            premiumActual,
            "PREMIUM should allow $operation at count=$currentCount"
        )
    }

    // Feature: offline-assessment-transformation, Property 12: Tier limit enforcement (boundary edges)
    // Validates: Requirements 15.1, 15.2, 15.3, 16.4
    //
    // Explicit checks around the exact Free-tier boundaries the strict-less-than hinges on:
    //   subjects 2/3/4, exams 4/5/6, scans 29/30/31.
    @Property(tries = 100)
    fun `free tier boundary counts classify exactly`(
        @ForAll("freeBoundaryCases") case: BoundaryCase
    ) {
        val actual = isWithinLimit(case.operation, case.count, FREE_LIMITS)
        assertEquals(case.expected, actual, "${case.operation} count=${case.count}")
    }

    // Feature: offline-assessment-transformation, Property 12: Feature gating by tier
    // Validates: Requirements 16.4
    //
    // Feature-based operations are always denied on FREE and always allowed on PREMIUM.
    // Count-based operations are never treated as features (isFeatureAllowed == false).
    @Property(tries = 100)
    fun `feature operations are denied on free and allowed on premium`(
        @ForAll("featureOperations") operation: LimitedOperation
    ) {
        assertFalse(isFeatureAllowed(operation, FREE_LIMITS), "FREE should deny $operation")
        assertTrue(isFeatureAllowed(operation, PREMIUM_LIMITS), "PREMIUM should allow $operation")
    }

    @Property(tries = 100)
    fun `count operations are not feature-allowed`(
        @ForAll("countBasedOperations") operation: LimitedOperation
    ) {
        assertFalse(isFeatureAllowed(operation, FREE_LIMITS), "$operation is not a feature (FREE)")
        assertFalse(isFeatureAllowed(operation, PREMIUM_LIMITS), "$operation is not a feature (PREMIUM)")
    }

    // ---------------------------------------------------------------------
    // Generators
    // ---------------------------------------------------------------------

    /** The three count-based operations, with arbitrary ids where applicable. */
    @Provide
    fun countBasedOperations(): Arbitrary<LimitedOperation> {
        val ids: Arbitrary<Long> = Arbitraries.longs().between(1L, 1_000_000L)
        return Arbitraries.oneOf(
            Arbitraries.just(LimitedOperation.CreateSubject),
            ids.map { LimitedOperation.CreateExam(it) },
            ids.map { LimitedOperation.ScanSheet(it) }
        )
    }

    /** The feature-based operations. */
    @Provide
    fun featureOperations(): Arbitrary<LimitedOperation> = Arbitraries.of(
        LimitedOperation.AdvancedAnalytics,
        LimitedOperation.SchoolReport,
        LimitedOperation.CustomTemplate,
        LimitedOperation.CurriculumTracking
    )

    data class BoundaryCase(
        val operation: LimitedOperation,
        val count: Int,
        val expected: Boolean
    )

    /** Boundary counts around each Free-tier limit (2/3/4, 4/5/6, 29/30/31) + 0. */
    @Provide
    fun freeBoundaryCases(): Arbitrary<BoundaryCase> = Arbitraries.of(
        // subjects, limit 3
        BoundaryCase(LimitedOperation.CreateSubject, 0, true),
        BoundaryCase(LimitedOperation.CreateSubject, 2, true),
        BoundaryCase(LimitedOperation.CreateSubject, 3, false),
        BoundaryCase(LimitedOperation.CreateSubject, 4, false),
        // exams per subject, limit 5
        BoundaryCase(LimitedOperation.CreateExam(1L), 0, true),
        BoundaryCase(LimitedOperation.CreateExam(1L), 4, true),
        BoundaryCase(LimitedOperation.CreateExam(1L), 5, false),
        BoundaryCase(LimitedOperation.CreateExam(1L), 6, false),
        // scans per exam, limit 30
        BoundaryCase(LimitedOperation.ScanSheet(1L), 0, true),
        BoundaryCase(LimitedOperation.ScanSheet(1L), 29, true),
        BoundaryCase(LimitedOperation.ScanSheet(1L), 30, false),
        BoundaryCase(LimitedOperation.ScanSheet(1L), 31, false)
    )
}
