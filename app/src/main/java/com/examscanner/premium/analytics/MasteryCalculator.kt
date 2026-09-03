package com.examscanner.premium.analytics

import com.examscanner.premium.data.AnswerKeyEntity
import com.examscanner.premium.data.ExamRepository
import com.examscanner.premium.data.StudentAnswerEntity
import com.examscanner.premium.data.StudentMelcMasteryEntity
import com.examscanner.premium.utils.SecureLogger
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * MasteryCalculator - Computes DepEd MELC mastery levels for a student.
 *
 * This is a net-new domain service (design §8) that reads and writes through
 * the existing [ExamRepository]/`ExamDao`. It aggregates a student's earned and
 * possible points across every exam that maps questions to a MELC, reduces the
 * result to a `(percentage, masteryLevel)` pair, and persists it via
 * [ExamRepository.insertStudentMelcMastery] (REPLACE on conflict).
 *
 * Per the reconciled design, [StudentMelcMasteryEntity] is a lean summary row
 * (no cumulative point columns) - the cumulative earned/possible aggregation is
 * computed here in-memory and only the reduced values are stored.
 */
class MasteryCalculator(
    private val repository: ExamRepository
) {

    /** Earned vs. possible points for one MELC within a single exam. */
    data class MelcScore(val earned: Int, val possible: Int)

    /** Aggregated competency mastery summary for a student profile. */
    data class MasterySummary(
        val totalMelcsAssessed: Int,
        val advanced: Int,
        val proficient: Int,
        val approaching: Int,
        val developing: Int,
        val averagePercentage: Float,
        val strengths: List<StudentMelcMasteryEntity>,
        val weaknesses: List<StudentMelcMasteryEntity>
    )

    /**
     * Recalculates MELC mastery for a student after a new assessment.
     *
     * @param studentId the [com.examscanner.premium.data.StudentEntity] Long
     *   primary key. Its school studentId String is resolved so performance can
     *   be aggregated across every exam record the student has (each scan is a
     *   separate StudentEntity row sharing the same school id). The persisted
     *   mastery rows are keyed on this incoming [studentId].
     */
    suspend fun updateStudentMastery(studentId: Long) = withContext(Dispatchers.Default) {
        try {
            // Resolve the school studentId String to gather all exam records.
            val anchor = repository.getStudentById(studentId)
            val studentRecords = if (anchor != null && anchor.studentId.isNotBlank()) {
                repository.getAllStudentRecords(anchor.studentId)
            } else {
                // Fall back to just the single record (or none) when the school id is absent.
                anchor?.let { listOf(it) } ?: emptyList()
            }

            // Accumulate per-MELC scores across every exam the student has taken.
            val melcScores = mutableMapOf<Long, MutableList<MelcScore>>()

            studentRecords.forEach { record ->
                val examId = record.examId
                if (examId <= 0L) return@forEach

                val answerKeys = repository.getAnswerKeys(examId).first()
                val studentAnswers = repository.getStudentAnswers(record.id)
                // questionNumber -> MelcEntity, inverted below to melcId -> [questionNumbers]
                val questionToMelc = repository.getQuestionMelcMappings(examId)

                val melcToQuestions = mutableMapOf<Long, MutableList<Int>>()
                questionToMelc.forEach { (questionNumber, melc) ->
                    melcToQuestions.getOrPut(melc.id) { mutableListOf() }.add(questionNumber)
                }

                melcToQuestions.forEach { (melcId, questions) ->
                    var earned = 0
                    var possible = 0
                    questions.forEach { questionNumber ->
                        val key = answerKeys.find { it.questionNumber == questionNumber }
                        if (key != null) {
                            possible += key.points
                            if (isAnswerCorrect(studentAnswers, questionNumber, key)) {
                                earned += key.points
                            }
                        }
                    }
                    melcScores.getOrPut(melcId) { mutableListOf() }
                        .add(MelcScore(earned = earned, possible = possible))
                }
            }

            // Reduce each MELC's cumulative scores to a percentage + level and persist.
            val now = System.currentTimeMillis()
            melcScores.forEach { (melcId, scores) ->
                val percentage = aggregatePercentage(scores)

                repository.insertStudentMelcMastery(
                    StudentMelcMasteryEntity(
                        studentId = studentId,
                        melcId = melcId,
                        masteryLevel = determineMasteryLevel(percentage),
                        percentage = percentage,
                        lastUpdated = now
                    )
                )
            }

            AnalyticsTracker.trackFeatureUsage(
                userId = currentUserId(),
                featureName = "student_mastery_updated",
                success = true,
                metadata = mapOf(
                    "student_id" to studentId,
                    "melcs_assessed" to melcScores.size
                )
            )
        } catch (e: Exception) {
            SecureLogger.e(TAG, "updateStudentMastery failed for student $studentId", e)
            AnalyticsTracker.trackError(
                userId = currentUserId(),
                errorType = "mastery_error",
                errorMessage = e.message ?: "Failed to update student mastery",
                stackTrace = e.stackTraceToString()
            )
            throw e
        }
    }

    /**
     * Builds the competency mastery summary shown on a student profile.
     *
     * Strengths are MELCs classified Proficient or Advanced (best first);
     * weaknesses are MELCs classified Developing or Approaching (worst first).
     */
    suspend fun getMasterySummary(studentId: Long): MasterySummary {
        val masteryRecords = repository.getStudentMastery(studentId).first()

        val advanced = masteryRecords.count { it.masteryLevel == LEVEL_ADVANCED }
        val proficient = masteryRecords.count { it.masteryLevel == LEVEL_PROFICIENT }
        val approaching = masteryRecords.count { it.masteryLevel == LEVEL_APPROACHING }
        val developing = masteryRecords.count { it.masteryLevel == LEVEL_DEVELOPING }

        val averagePercentage = if (masteryRecords.isNotEmpty()) {
            masteryRecords.map { it.percentage }.average().toFloat()
        } else {
            0f
        }

        val strengths = masteryRecords
            .filter { it.masteryLevel == LEVEL_PROFICIENT || it.masteryLevel == LEVEL_ADVANCED }
            .sortedByDescending { it.percentage }

        val weaknesses = masteryRecords
            .filter { it.masteryLevel == LEVEL_DEVELOPING || it.masteryLevel == LEVEL_APPROACHING }
            .sortedBy { it.percentage }

        return MasterySummary(
            totalMelcsAssessed = masteryRecords.size,
            advanced = advanced,
            proficient = proficient,
            approaching = approaching,
            developing = developing,
            averagePercentage = averagePercentage,
            strengths = strengths,
            weaknesses = weaknesses
        )
    }

    /**
     * Whether the student's answer to [questionNumber] matches the answer key,
     * accepting any of the key's comma-separated [AnswerKeyEntity.alternativeAnswers].
     */
    private fun isAnswerCorrect(
        studentAnswers: List<StudentAnswerEntity>,
        questionNumber: Int,
        key: AnswerKeyEntity
    ): Boolean {
        val answer = studentAnswers.find { it.questionNumber == questionNumber }?.answer ?: return false
        if (answer == key.correctAnswer) return true
        return key.alternativeAnswers
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .contains(answer)
    }

    private fun currentUserId(): String? =
        FirebaseAuth.getInstance().currentUser?.uid

    companion object {
        private const val TAG = "MasteryCalculator"

        const val LEVEL_DEVELOPING = "Developing"
        const val LEVEL_APPROACHING = "Approaching"
        const val LEVEL_PROFICIENT = "Proficient"
        const val LEVEL_ADVANCED = "Advanced"

        /**
         * Classifies a percentage (0-100) into a DepEd-aligned mastery band:
         * Developing 0-74.99, Approaching 75-79.99, Proficient 80-89.99,
         * Advanced 90-100.
         */
        fun determineMasteryLevel(percentage: Float): String {
            return when {
                percentage >= 90f -> LEVEL_ADVANCED
                percentage >= 80f -> LEVEL_PROFICIENT
                percentage >= 75f -> LEVEL_APPROACHING
                else -> LEVEL_DEVELOPING
            }
        }

        /**
         * Reduces a MELC's cumulative earned/possible scores to a bounded
         * percentage: (Σearned / Σpossible) × 100 when Σpossible > 0, else 0.
         *
         * This is the pure aggregation math extracted from
         * [updateStudentMastery] so it can be unit/property tested without a
         * repository or database. The result is always in the closed range
         * [0, 100] for well-formed inputs (each earned ≤ possible, possible ≥ 0).
         */
        fun aggregatePercentage(scores: List<MelcScore>): Float {
            val totalEarned = scores.sumOf { it.earned }
            val totalPossible = scores.sumOf { it.possible }
            return if (totalPossible > 0) {
                (totalEarned.toFloat() / totalPossible.toFloat()) * 100f
            } else {
                0f
            }
        }
    }
}
