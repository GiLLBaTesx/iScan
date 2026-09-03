package com.examscanner.premium.analytics

import com.examscanner.premium.data.ExamRepository
import com.examscanner.premium.data.MelcCoverageEntity
import com.examscanner.premium.data.MelcEntity
import com.examscanner.premium.utils.SecureLogger
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * PacingEngine - Tracks DepEd curriculum (MELC) coverage and pacing.
 *
 * Net-new domain service (design §9) that integrates with the existing
 * [ExamRepository]/`ExamDao` rather than a separate `MelcRepository`. It:
 *
 *  - Produces a per-quarter [PacingGuide]: each MELC gets a recommended week
 *    (1-10) spread evenly across the quarter, a [CoverageStatus], and its
 *    coverage timestamp.
 *  - Detects behind-schedule MELCs (no coverage recorded while the current
 *    week is already past the MELC's recommended week).
 *  - Persists manual coverage decisions via [markMelcCovered] / [markMelcSkipped]
 *    into the `melc_coverage` table through [ExamRepository.upsertMelcCoverage].
 *  - Rolls the four quarters up into a [QuarterlySummary].
 *
 * Reconciliation with the actual codebase (vs. the design sketch):
 *  - `MelcEntity.gradeLevel` is a **String**, so pacing is keyed on a String
 *    grade level, not an Int.
 *  - There is no per-assessment history table; `melc_coverage` holds at most
 *    one logical coverage row per MELC (the DAO upsert replaces prior rows).
 *    A MELC is therefore treated as "assessed/covered" when a coverage row
 *    exists, "fully assessed" when that row's `coverageType` is "Assessment",
 *    "partially assessed" for a "Manual" coverage, and skipped rows
 *    ([COVERAGE_TYPE_SKIPPED]) are excluded from the assessed count.
 *  - Coverage % = (assessed MELCs / total MELCs) × 100 (Requirement 9.3).
 *
 * The pure schedule/coverage-status/percentage math lives in the companion
 * object so it can be unit tested without a device or database (task 7.4).
 */
class PacingEngine(
    private val repository: ExamRepository
) {

    /** A MELC together with its recommended schedule slot and coverage status. */
    data class MelcWithSchedule(
        val melc: MelcEntity,
        val recommendedWeek: Int,           // Week within the quarter (1-10)
        val status: CoverageStatus,
        val lastCoveredDate: Long?,
        val coverageType: String?           // "Assessment", "Manual", "Skipped" or null
    )

    /** Pacing guide for a single subject in a single quarter. */
    data class PacingGuide(
        val quarter: Int,
        val melcs: List<MelcWithSchedule>,
        val totalMelcs: Int,
        val assessedMelcs: Int,
        val coveragePercentage: Float,
        val behindSchedule: List<MelcEntity>
    )

    /** Per-quarter roll-up used inside [QuarterlySummary]. */
    data class QuarterProgress(
        val quarter: Int,
        val totalMelcs: Int,
        val assessedMelcs: Int,
        val coveragePercentage: Float
    )

    /** Whole-school-year coverage roll-up across all four quarters. */
    data class QuarterlySummary(
        val schoolYear: String,
        val gradeLevel: String,
        val subject: String,
        val quarters: List<QuarterProgress>,
        val overallCoverage: Float
    )

    /** Coverage status for a single MELC in the pacing guide. */
    enum class CoverageStatus {
        NOT_ASSESSED,           // Never covered/assessed, still on schedule
        PARTIALLY_ASSESSED,     // Manually marked covered (non-assessment)
        FULLY_ASSESSED,         // Covered through an assessment
        BEHIND_SCHEDULE         // Past the recommended week with no coverage
    }

    /**
     * Builds the pacing guide for a subject + grade level in a given quarter.
     *
     * @param subject the MELC subject name (matches [MelcEntity.subject]).
     * @param gradeLevel the MELC grade level String (matches [MelcEntity.gradeLevel]).
     * @param quarter the quarter (1-4).
     * @param currentWeek the current week within the quarter (1-10); used for
     *   behind-schedule detection. Callers may derive this from the current
     *   date relative to the quarter start.
     */
    suspend fun getPacingGuide(
        subject: String,
        gradeLevel: String,
        quarter: Int,
        currentWeek: Int
    ): PacingGuide = withContext(Dispatchers.Default) {
        try {
            val melcs = repository.examDao.getMelcsByQuarter(quarter).first()
                .filter { it.subject == subject && it.gradeLevel == gradeLevel }
                .sortedBy { it.code }

            val melcsWithSchedule = melcs.mapIndexed { index, melc ->
                val recommendedWeek = recommendedWeekFor(index, melcs.size)
                val coverage = repository.examDao.getMelcCoverageByMelc(melc.id)
                val status = determineStatus(coverage?.coverageType, recommendedWeek, currentWeek)

                MelcWithSchedule(
                    melc = melc,
                    recommendedWeek = recommendedWeek,
                    status = status,
                    lastCoveredDate = coverage?.coveredAt,
                    coverageType = coverage?.coverageType
                )
            }

            val assessedCount = melcsWithSchedule.count { isAssessed(it.status) }

            PacingGuide(
                quarter = quarter,
                melcs = melcsWithSchedule,
                totalMelcs = melcs.size,
                assessedMelcs = assessedCount,
                coveragePercentage = coveragePercentage(assessedCount, melcs.size),
                behindSchedule = melcsWithSchedule
                    .filter { it.status == CoverageStatus.BEHIND_SCHEDULE }
                    .map { it.melc }
            )
        } catch (e: Exception) {
            SecureLogger.e(TAG, "getPacingGuide failed for $subject / $gradeLevel Q$quarter", e)
            AnalyticsTracker.trackError(
                userId = currentUserId(),
                errorType = "pacing_error",
                errorMessage = e.message ?: "Failed to build pacing guide",
                stackTrace = e.stackTraceToString()
            )
            throw e
        }
    }

    /**
     * Marks a MELC as manually covered (non-assessment coverage), persisting a
     * "Manual" coverage row to `melc_coverage`. Requirement 9.6.
     */
    suspend fun markMelcCovered(melcId: Long, subjectId: Long, notes: String = ""): Long {
        return repository.upsertMelcCoverage(
            melcId = melcId,
            subjectId = subjectId,
            coveredAt = System.currentTimeMillis(),
            notes = notes,
            coverageType = COVERAGE_TYPE_MANUAL
        )
    }

    /**
     * Marks a MELC as intentionally skipped, persisting a "Skipped" coverage row
     * to `melc_coverage`. Skipped MELCs are recorded (so they no longer surface
     * as behind-schedule) but are NOT counted toward the coverage percentage.
     * Requirement 9.6.
     */
    suspend fun markMelcSkipped(melcId: Long, subjectId: Long, notes: String = ""): Long {
        return repository.upsertMelcCoverage(
            melcId = melcId,
            subjectId = subjectId,
            coveredAt = System.currentTimeMillis(),
            notes = notes,
            coverageType = COVERAGE_TYPE_SKIPPED
        )
    }

    /**
     * Generates a whole-school-year quarterly coverage summary for a subject +
     * grade level. Each quarter is evaluated at end-of-quarter (`currentWeek =
     * WEEKS_PER_QUARTER`) so the summary reflects final coverage. Requirement 9.7.
     */
    suspend fun getQuarterlySummary(
        subject: String,
        gradeLevel: String,
        schoolYear: String
    ): QuarterlySummary {
        val quarters = (1..4).map { quarter ->
            val guide = getPacingGuide(subject, gradeLevel, quarter, currentWeek = WEEKS_PER_QUARTER)
            QuarterProgress(
                quarter = quarter,
                totalMelcs = guide.totalMelcs,
                assessedMelcs = guide.assessedMelcs,
                coveragePercentage = guide.coveragePercentage
            )
        }

        val quartersWithMelcs = quarters.filter { it.totalMelcs > 0 }
        val overall = if (quartersWithMelcs.isNotEmpty()) {
            quartersWithMelcs.map { it.coveragePercentage }.average().toFloat()
        } else {
            0f
        }

        return QuarterlySummary(
            schoolYear = schoolYear,
            gradeLevel = gradeLevel,
            subject = subject,
            quarters = quarters,
            overallCoverage = overall
        )
    }

    /**
     * Observe the raw coverage rows for a subject + quarter. Thin pass-through
     * to [ExamRepository.getCoverage] for UI that wants to react to changes.
     */
    fun observeCoverage(subject: String, quarter: Int) =
        repository.getCoverage(subject, quarter)

    private fun currentUserId(): String? =
        FirebaseAuth.getInstance().currentUser?.uid

    companion object {
        private const val TAG = "PacingEngine"

        /** DepEd quarters are ~10 instructional weeks. */
        const val WEEKS_PER_QUARTER = 10

        const val COVERAGE_TYPE_ASSESSMENT = "Assessment"
        const val COVERAGE_TYPE_MANUAL = "Manual"
        const val COVERAGE_TYPE_SKIPPED = "Skipped"

        /**
         * Assigns a recommended week (1..[WEEKS_PER_QUARTER]) to the MELC at
         * [index] out of [total], spreading MELCs evenly across the quarter.
         *
         * Pure function extracted for testability (task 7.4). For an empty or
         * single-MELC set every MELC maps to week 1; otherwise the schedule is
         * a monotonically non-decreasing spread across the quarter.
         */
        fun recommendedWeekFor(index: Int, total: Int): Int {
            if (total <= 0) return 1
            val melcsPerWeek = total.toFloat() / WEEKS_PER_QUARTER
            if (melcsPerWeek <= 0f) return 1
            return ((index / melcsPerWeek).toInt() + 1).coerceIn(1, WEEKS_PER_QUARTER)
        }

        /**
         * Pure coverage-status classification (task 7.4).
         *
         * @param coverageType the stored coverage type for the MELC, or null
         *   when no coverage row exists.
         * @param recommendedWeek the MELC's recommended coverage week.
         * @param currentWeek the current week within the quarter.
         *
         * Rules:
         *  - "Assessment" coverage  -> FULLY_ASSESSED
         *  - "Manual" coverage      -> PARTIALLY_ASSESSED
         *  - "Skipped" coverage     -> NOT_ASSESSED (recorded, not behind, not counted)
         *  - no coverage & past due -> BEHIND_SCHEDULE  (currentWeek > recommendedWeek)
         *  - no coverage & on time  -> NOT_ASSESSED
         */
        fun determineStatus(
            coverageType: String?,
            recommendedWeek: Int,
            currentWeek: Int
        ): CoverageStatus {
            return when (coverageType) {
                COVERAGE_TYPE_ASSESSMENT -> CoverageStatus.FULLY_ASSESSED
                COVERAGE_TYPE_MANUAL -> CoverageStatus.PARTIALLY_ASSESSED
                COVERAGE_TYPE_SKIPPED -> CoverageStatus.NOT_ASSESSED
                else -> if (currentWeek > recommendedWeek) {
                    CoverageStatus.BEHIND_SCHEDULE
                } else {
                    CoverageStatus.NOT_ASSESSED
                }
            }
        }

        /** Whether a status counts toward the assessed/coverage total. */
        fun isAssessed(status: CoverageStatus): Boolean =
            status == CoverageStatus.FULLY_ASSESSED || status == CoverageStatus.PARTIALLY_ASSESSED

        /**
         * Coverage percentage = (assessed / total) × 100, clamped to [0, 100].
         * Returns 0 when there are no MELCs. Pure function for task 7.4.
         */
        fun coveragePercentage(assessed: Int, total: Int): Float {
            if (total <= 0) return 0f
            return (assessed.toFloat() / total.toFloat() * 100f).coerceIn(0f, 100f)
        }
    }
}
