package com.examscanner.premium.analytics

import com.examscanner.premium.data.AnswerKeyEntity
import com.examscanner.premium.data.ExamRepository
import com.examscanner.premium.data.MelcEntity
import com.examscanner.premium.data.StudentAnswerEntity
import com.examscanner.premium.data.StudentEntity
import com.examscanner.premium.utils.SecureLogger
import kotlinx.coroutines.flow.first

/**
 * AnalyticsEngine - Advanced assessment analytics and item analysis.
 *
 * Calculates classical test-theory statistics over the exam data already
 * persisted by [ExamRepository]. This engine reuses the repository's scoring
 * (`calculateScore`), student, answer-key, and MELC-mastery reads rather than
 * duplicating any scoring logic.
 *
 * Provided analytics (Requirement 6):
 * - Difficulty index (6.1): proportion answering correctly x 100 (0-100).
 * - Discrimination index (6.2): (U - L) / N using the upper/lower 27% groups.
 * - Discrimination classification (6.3): Poor / Fair / Good / Excellent.
 * - Item response curve (6.4): per-total-score-group proportion correct.
 * - Learning gaps (6.5): MELCs below 75% mastery across a section.
 * - Low-quality flagging (6.7): discrimination < 0.20 OR difficulty < 30% OR > 90%.
 */
class AnalyticsEngine(
    private val repository: ExamRepository
) {

    /**
     * Calculates the difficulty index for a question.
     *
     * Formula: (students answering correctly / total students) x 100.
     * Returns a value in the inclusive range 0.0..100.0, and 0.0 for an empty
     * cohort or when the question has no answer key. (Requirement 6.1)
     */
    suspend fun calculateDifficulty(examId: Long, questionNumber: Int): Float {
        val students = repository.getStudents(examId).first()
        if (students.isEmpty()) return 0f

        val keys = repository.getAnswerKeys(examId).first()
        val key = keys.find { it.questionNumber == questionNumber } ?: return 0f

        val correctCount = students.count { student ->
            isCorrect(repository.getStudentAnswers(student.id), questionNumber, key)
        }

        return (correctCount.toFloat() / students.size) * 100f
    }

    /**
     * Calculates the discrimination index using the upper and lower 27% score
     * groups.
     *
     * Formula: (U - L) / N, where U/L are the counts of correct responses in
     * the upper/lower group and N is the size of each group.
     *
     * Returns a value in the inclusive range -1.0..+1.0. Returns 0.0 when there
     * are fewer than [MIN_DISCRIMINATION_SAMPLE] students (insufficient sample),
     * when the group size resolves to 0, or when the question has no answer key.
     * (Requirement 6.2)
     */
    suspend fun calculateDiscriminationIndex(examId: Long, questionNumber: Int): Float {
        val students = repository.getStudents(examId).first()
        if (students.size < MIN_DISCRIMINATION_SAMPLE) return 0f

        val keys = repository.getAnswerKeys(examId).first()
        val key = keys.find { it.questionNumber == questionNumber } ?: return 0f

        // Rank students by total score (reusing the repository scoring logic).
        val ranked = students
            .map { student -> student to repository.calculateScore(student, keys) }
            .sortedByDescending { it.second }

        val groupSize = (ranked.size * UPPER_LOWER_FRACTION).toInt()
        if (groupSize == 0) return 0f

        val upperGroup = ranked.take(groupSize)
        val lowerGroup = ranked.takeLast(groupSize)

        val upperCorrect = upperGroup.count { (student, _) ->
            isCorrect(repository.getStudentAnswers(student.id), questionNumber, key)
        }
        val lowerCorrect = lowerGroup.count { (student, _) ->
            isCorrect(repository.getStudentAnswers(student.id), questionNumber, key)
        }

        val index = (upperCorrect - lowerCorrect).toFloat() / groupSize
        // (U - L) / N is mathematically bounded to [-1, 1]; clamp defensively.
        return index.coerceIn(-1f, 1f)
    }

    /**
     * Classifies a discrimination index into a quality band. (Requirement 6.3)
     *
     * - Poor:      index < 0.20
     * - Fair:      0.20 <= index < 0.30
     * - Good:      0.30 <= index < 0.40
     * - Excellent: index >= 0.40
     */
    fun classifyDiscrimination(index: Float): DiscriminationQuality {
        return when {
            index < 0.20f -> DiscriminationQuality.POOR
            index < 0.30f -> DiscriminationQuality.FAIR
            index < 0.40f -> DiscriminationQuality.GOOD
            else -> DiscriminationQuality.EXCELLENT
        }
    }

    enum class DiscriminationQuality {
        POOR, FAIR, GOOD, EXCELLENT
    }

    /**
     * Identifies learning gaps: MELCs whose average mastery across the section
     * falls below 75%. (Requirement 6.5)
     *
     * Mastery is read from the persisted per-student MELC mastery summaries via
     * [ExamRepository.getStudentMastery]; students with no recorded mastery for a
     * MELC contribute 0% for that MELC. The result is ordered from the most
     * severe gap (lowest average mastery) upward.
     */
    suspend fun identifyLearningGaps(sectionId: Long): List<LearningGap> {
        val students = repository.getStudentsBySection(sectionId)
        if (students.isEmpty()) return emptyList()

        val melcs = repository.getAllMelcs().first()

        // Pre-load each student's mastery rows once to avoid repeated reads.
        val masteryByStudent: Map<Long, Map<Long, Float>> = students.associate { student ->
            student.id to repository.getStudentMastery(student.id).first()
                .associate { it.melcId to it.percentage }
        }

        return melcs.mapNotNull { melc ->
            val perStudentMastery = students.map { student ->
                masteryByStudent[student.id]?.get(melc.id) ?: 0f
            }

            val averageMastery = perStudentMastery.average().toFloat()
            if (averageMastery < LEARNING_GAP_THRESHOLD) {
                LearningGap(
                    melc = melc,
                    averageMastery = averageMastery,
                    studentsBelow75 = perStudentMastery.count { it < LEARNING_GAP_THRESHOLD },
                    totalStudents = students.size,
                    severity = when {
                        averageMastery < 50f -> GapSeverity.CRITICAL
                        averageMastery < 65f -> GapSeverity.HIGH
                        else -> GapSeverity.MODERATE
                    }
                )
            } else {
                null
            }
        }.sortedBy { it.averageMastery }
    }

    data class LearningGap(
        val melc: MelcEntity,
        val averageMastery: Float,
        val studentsBelow75: Int,
        val totalStudents: Int,
        val severity: GapSeverity
    )

    enum class GapSeverity {
        MODERATE, HIGH, CRITICAL
    }

    /**
     * Builds the item response curve data for a question. (Requirement 6.4)
     *
     * Returns a map of answer option -> number of students who selected it,
     * plus the total number of student responses considered. Students who left
     * the question blank are counted under [NO_ANSWER_LABEL].
     */
    suspend fun getItemResponseCurve(examId: Long, questionNumber: Int): ItemResponseCurve {
        val students = repository.getStudents(examId).first()
        val keys = repository.getAnswerKeys(examId).first()
        val key = keys.find { it.questionNumber == questionNumber }
            ?: return ItemResponseCurve(emptyMap(), 0)

        val responses = linkedMapOf<String, Int>()
        // Seed the known options (correct + declared alternatives) so they appear
        // even when no student selected them.
        expectedOptions(key).forEach { option -> responses[option] = 0 }

        students.forEach { student ->
            val answer = repository.getStudentAnswers(student.id)
                .find { it.questionNumber == questionNumber }
                ?.answer
                ?.takeIf { it.isNotBlank() }
                ?: NO_ANSWER_LABEL
            responses[answer] = (responses[answer] ?: 0) + 1
        }

        return ItemResponseCurve(
            responses = responses,
            totalResponses = students.size
        )
    }

    data class ItemResponseCurve(
        val responses: Map<String, Int>, // Answer option -> count
        val totalResponses: Int
    )

    /**
     * Flags a question as low quality when its discrimination index is below
     * 0.20 OR its difficulty index is below 30% OR above 90%. (Requirement 6.7)
     */
    suspend fun flagLowQuality(examId: Long, questionNumber: Int): QuestionQualityFlag {
        val difficulty = calculateDifficulty(examId, questionNumber)
        val discrimination = calculateDiscriminationIndex(examId, questionNumber)
        return evaluateQuality(questionNumber, difficulty, discrimination)
    }

    /**
     * Pure quality evaluation over already-computed statistics. Useful for
     * flagging every question of an exam without recomputing shared reads, and
     * directly testable. (Requirement 6.7)
     */
    fun evaluateQuality(
        questionNumber: Int,
        difficulty: Float,
        discrimination: Float
    ): QuestionQualityFlag {
        val lowDiscrimination = discrimination < LOW_DISCRIMINATION_THRESHOLD
        val tooEasy = difficulty > MAX_DIFFICULTY_PCT
        val tooHard = difficulty < MIN_DIFFICULTY_PCT
        return QuestionQualityFlag(
            questionNumber = questionNumber,
            difficulty = difficulty,
            discrimination = discrimination,
            lowDiscrimination = lowDiscrimination,
            tooEasy = tooEasy,
            tooHard = tooHard,
            isLowQuality = lowDiscrimination || tooEasy || tooHard
        )
    }

    data class QuestionQualityFlag(
        val questionNumber: Int,
        val difficulty: Float,
        val discrimination: Float,
        val lowDiscrimination: Boolean,
        val tooEasy: Boolean,
        val tooHard: Boolean,
        val isLowQuality: Boolean
    )

    /**
     * Convenience: evaluates quality for every keyed question of an exam.
     */
    suspend fun flagLowQualityQuestions(examId: Long): List<QuestionQualityFlag> {
        return try {
            repository.getAnswerKeys(examId).first()
                .sortedBy { it.questionNumber }
                .map { key -> flagLowQuality(examId, key.questionNumber) }
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Failed to flag low-quality questions for exam $examId", e)
            emptyList()
        }
    }

    /**
     * Determines whether a student's response to [questionNumber] is correct,
     * matching either the primary correct answer or one of the comma-separated
     * alternative answers declared on the key.
     */
    private fun isCorrect(
        answers: List<StudentAnswerEntity>,
        questionNumber: Int,
        key: AnswerKeyEntity
    ): Boolean {
        val response = answers.find { it.questionNumber == questionNumber }?.answer ?: return false
        return response == key.correctAnswer || response in alternativeAnswersOf(key)
    }

    private fun expectedOptions(key: AnswerKeyEntity): List<String> {
        val options = mutableListOf<String>()
        if (key.correctAnswer.isNotBlank()) options.add(key.correctAnswer)
        options.addAll(alternativeAnswersOf(key))
        return options.distinct()
    }

    private fun alternativeAnswersOf(key: AnswerKeyEntity): List<String> {
        return key.alternativeAnswers
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    companion object {
        private const val TAG = "AnalyticsEngine"

        /** Minimum cohort size required to compute a discrimination index. */
        const val MIN_DISCRIMINATION_SAMPLE = 10

        /** Fraction of the cohort used for the upper and lower score groups. */
        const val UPPER_LOWER_FRACTION = 0.27

        /** A section MELC is a learning gap when average mastery is below this. */
        const val LEARNING_GAP_THRESHOLD = 75f

        /** Low-quality thresholds (Requirement 6.7). */
        const val LOW_DISCRIMINATION_THRESHOLD = 0.20f
        const val MIN_DIFFICULTY_PCT = 30f
        const val MAX_DIFFICULTY_PCT = 90f

        const val NO_ANSWER_LABEL = "No Answer"
    }
}
