package com.examscanner.premium.onboarding

import com.examscanner.premium.data.ExamRepository
import com.examscanner.premium.data.OnboardingPreferences
import com.examscanner.premium.utils.SecureLogger
import kotlinx.coroutines.flow.first

/**
 * OnboardingManager - Coordinates the first-launch tutorial and sample-data seeding.
 *
 * This is a thin coordinator that REUSES the existing onboarding infrastructure:
 * - [OnboardingPreferences] (DataStore) owns the "has seen onboarding" flag and the
 *   completed / reset transitions.
 * - `ui/screens/OnboardingScreen.kt` owns the tutorial UI (4-page pager with a Skip
 *   button). This manager does NOT re-implement any UI.
 * - [ExamRepository] owns all data access used for optional sample-data seeding.
 *
 * Responsibilities:
 * - Decide whether the welcome tutorial should show on launch (Req 28.1).
 * - Mark the tutorial complete / skipped, delegating to the existing preferences
 *   (Req 28.6 skippable).
 * - Allow re-showing the tutorial from Settings -> Help -> "Show Tutorial Again"
 *   (Req 28.7) via [replayTutorial].
 * - Optionally seed sample data - 1 subject folder, 2 exams with answer keys, and
 *   5 students with results - using existing repository methods, guarded so it never
 *   double-seeds (Req 28.5).
 *
 * The tutorial content itself (subjects, exams, MELC mapping, scanning, reports -
 * Req 28.2) lives in `onboardingPages` inside `OnboardingScreen.kt`.
 *
 * Requirements: 28.1, 28.2, 28.3, 28.5, 28.6, 28.7
 */
class OnboardingManager(
    private val onboardingPreferences: OnboardingPreferences,
    private val repository: ExamRepository
) {

    companion object {
        private const val TAG = "OnboardingManager"

        /** Name of the seeded sample subject folder. Used as the idempotency guard. */
        const val SAMPLE_SUBJECT_NAME = "Sample Subject (Demo)"

        private const val SAMPLE_EXAM_ONE_NAME = "Sample Quiz 1 (Demo)"
        private const val SAMPLE_EXAM_TWO_NAME = "Sample Quiz 2 (Demo)"

        /** Number of questions on each seeded sample exam. */
        private const val SAMPLE_QUESTION_COUNT = 5

        /** Number of sample students seeded per exam. */
        private const val SAMPLE_STUDENT_COUNT = 5
    }

    /**
     * Whether the first-launch welcome tutorial should be shown (Req 28.1).
     * Reads the existing [OnboardingPreferences] flag; true only until the user
     * completes or skips the tutorial.
     */
    suspend fun shouldShowTutorial(): Boolean {
        return !onboardingPreferences.hasSeenOnboarding.first()
    }

    /**
     * Mark the tutorial as completed (user tapped "Get Started"). Delegates to the
     * existing [OnboardingPreferences.setOnboardingCompleted].
     */
    suspend fun markTutorialComplete() {
        onboardingPreferences.setOnboardingCompleted()
        SecureLogger.d(TAG, "Tutorial marked complete")
    }

    /**
     * Mark the tutorial as skipped (Req 28.6). Skipping and completing have the same
     * effect on the "seen" flag - the tutorial won't auto-show again - so this
     * delegates to the same preference transition.
     */
    suspend fun markTutorialSkipped() {
        onboardingPreferences.setOnboardingCompleted()
        SecureLogger.d(TAG, "Tutorial marked skipped")
    }

    /**
     * Reset the "seen onboarding" flag so the tutorial can be shown again (Req 28.7).
     * The Settings -> Help -> "Show Tutorial Again" action calls this, then navigates
     * to the existing [com.examscanner.premium.ui.screens.OnboardingScreen].
     */
    suspend fun replayTutorial() {
        onboardingPreferences.resetOnboarding()
        SecureLogger.d(TAG, "Tutorial reset for replay")
    }

    /**
     * Whether the app currently allows re-showing the tutorial. Always true - the
     * replay path (Req 28.7) is unconditionally available from Settings.
     */
    fun allowsReshowing(): Boolean = true

    /**
     * Idempotently seed sample data (Req 28.5): 1 subject folder, 2 sample exams
     * (each with an answer key), and 5 sample students with recorded results per exam.
     *
     * Uses only existing [ExamRepository] methods. Guarded so it never double-seeds:
     * seeding is skipped when a subject folder with [SAMPLE_SUBJECT_NAME] already
     * exists (which also covers the "no subjects yet" first-run case, since the guard
     * simply finds nothing to skip on).
     *
     * @return true if sample data was seeded, false if it already existed (no-op).
     */
    suspend fun seedSampleData(): Boolean {
        return try {
            val existingFolders = repository.getAllSubjectFolders().first()
            if (existingFolders.any { it.name == SAMPLE_SUBJECT_NAME }) {
                SecureLogger.d(TAG, "Sample data already present; skipping seed")
                return false
            }

            val folderId = repository.createSubjectFolder(SAMPLE_SUBJECT_NAME)

            seedSampleExam(SAMPLE_EXAM_ONE_NAME, folderId)
            seedSampleExam(SAMPLE_EXAM_TWO_NAME, folderId)

            SecureLogger.d(TAG, "Sample data seeded (1 subject, 2 exams, students per exam)")
            true
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Failed to seed sample data", e)
            false
        }
    }

    /**
     * Create a single sample exam under [folderId] with a fixed answer key and
     * [SAMPLE_STUDENT_COUNT] students whose recorded answers vary so the demo shows a
     * realistic spread of scores.
     */
    private suspend fun seedSampleExam(examName: String, folderId: Long) {
        val examId = repository.createExam(
            name = examName,
            totalQuestions = SAMPLE_QUESTION_COUNT,
            folderId = folderId
        )

        // Answer key: questions 1..5 with a fixed correct choice.
        val answerKey = SAMPLE_ANSWER_KEY
        repository.saveAnswerKey(examId, answerKey)

        // Seed students, each answering a different number of questions correctly so
        // the sample reports show variation rather than identical scores.
        for (index in 1..SAMPLE_STUDENT_COUNT) {
            val correctCount = index // student 1 gets 1 correct, ... student 5 all 5
            val answers = answerKey.mapIndexed { qIndex, (questionNum, correct) ->
                val answer = if (qIndex < correctCount) correct else wrongChoiceFor(correct)
                questionNum to answer
            }
            repository.saveStudentResults(
                examId = examId,
                studentId = "S%02d".format(index),
                name = "Sample Student $index",
                answers = answers
            )
        }
    }

    /** Return a plausible wrong choice (A-D) that differs from [correct]. */
    private fun wrongChoiceFor(correct: String): String {
        return CHOICES.firstOrNull { it != correct } ?: "A"
    }
}

private val CHOICES = listOf("A", "B", "C", "D")

/** Fixed 5-question answer key used for both seeded sample exams. */
private val SAMPLE_ANSWER_KEY: List<Pair<Int, String>> = listOf(
    1 to "A",
    2 to "B",
    3 to "C",
    4 to "D",
    5 to "A"
)
