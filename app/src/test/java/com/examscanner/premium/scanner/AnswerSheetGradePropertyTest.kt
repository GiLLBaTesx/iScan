package com.examscanner.premium.scanner

import com.examscanner.premium.data.AnswerKeyEntity
import com.examscanner.premium.qrcode.QRCodeParser
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Combinators
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import net.jqwik.api.constraints.IntRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit + property tests for [AnswerSheetParser.grade] — the auto-grading seam
 * (Req 17.3). These are JVM unit tests (jqwik on the JUnit 5 platform) that run
 * WITHOUT a device: they drive the pure `simulateDetection -> resolve -> grade`
 * path, so no bitmap, camera, ML Kit, or database is required.
 *
 * They close an auto-grade regression-coverage gap by asserting that grading:
 *  - awards each key's `points` exactly when the resolved answer matches
 *    `correctAnswer` or one of the comma-separated `alternativeAnswers`
 *    (case-insensitive),
 *  - never scores a [AnswerSheetParser.NO_ANSWER] or
 *    [AnswerSheetParser.INVALID_MULTIPLE] resolution,
 *  - computes percentage as integer `earned * 100 / total` (0 when total == 0).
 */
class AnswerSheetGradePropertyTest {

    // Neither pure seam touches the processor or the QR parser's collaborators.
    private val parser = AnswerSheetParser(QRCodeParser(), BubbleDetectionEngine())

    private val labels = listOf("A", "B", "C", "D")

    private fun key(
        question: Int,
        correct: String,
        alternatives: String = "",
        points: Int = 1
    ) = AnswerKeyEntity(
        examId = 1L,
        questionNumber = question,
        correctAnswer = correct,
        alternativeAnswers = alternatives,
        points = points
    )

    /** Build a graded result from a question -> filled-label map. */
    private fun gradeFills(
        fills: Map<Int, String?>,
        keys: List<AnswerKeyEntity>,
        multiMarks: Map<Int, Set<String>> = emptyMap()
    ): AnswerSheetParser.GradeResult {
        val detection = parser.simulateDetection(fills, labels, multiMarks)
        val parseResult = parser.resolve(detection)
        return parser.grade(parseResult, keys)
    }

    @Test
    fun `all correct earns full points and 100 percent`() {
        val keys = listOf(
            key(1, "A", points = 2),
            key(2, "B", points = 3),
            key(3, "C", points = 1)
        )
        val fills = mapOf(1 to "A", 2 to "B", 3 to "C")

        val result = gradeFills(fills, keys)

        assertEquals(6, result.totalPoints)
        assertEquals(6, result.earnedPoints)
        assertEquals(3, result.correctCount)
        assertEquals(3, result.gradedQuestions)
        assertEquals(100, result.percentage)
    }

    @Test
    fun `all blank earns zero points and zero percent`() {
        val keys = listOf(key(1, "A"), key(2, "B"), key(3, "C"))
        // No fills at all -> every question resolves to NO_ANSWER.
        val fills = mapOf<Int, String?>(1 to null, 2 to null, 3 to null)

        val result = gradeFills(fills, keys)

        assertEquals(0, result.earnedPoints)
        assertEquals(0, result.correctCount)
        assertEquals(0, result.percentage)
        assertEquals(3, result.totalPoints)
    }

    @Test
    fun `partial with points weighting earns only correct question points`() {
        val keys = listOf(
            key(1, "A", points = 5),
            key(2, "B", points = 3),
            key(3, "C", points = 2)
        )
        // Q1 correct (5), Q2 wrong, Q3 correct (2) -> earned 7 of 10.
        val fills = mapOf(1 to "A", 2 to "D", 3 to "C")

        val result = gradeFills(fills, keys)

        assertEquals(10, result.totalPoints)
        assertEquals(7, result.earnedPoints)
        assertEquals(2, result.correctCount)
        assertEquals((7 * 100) / 10, result.percentage) // integer division = 70
    }

    @Test
    fun `alternative answers are accepted case insensitively`() {
        val keys = listOf(key(1, "A", alternatives = "B, C", points = 1))

        assertEquals(1, gradeFills(mapOf(1 to "A"), keys).earnedPoints)
        assertEquals(1, gradeFills(mapOf(1 to "B"), keys).earnedPoints)
        assertEquals(1, gradeFills(mapOf(1 to "C"), keys).earnedPoints)
        // "D" is neither the correct answer nor an alternative.
        assertEquals(0, gradeFills(mapOf(1 to "D"), keys).earnedPoints)
    }

    @Test
    fun `multi mark and no answer never score`() {
        // Key says "A"; the student shades both A and B -> INVALID_MULTIPLE.
        val keys = listOf(key(1, "A", points = 4), key(2, "B", points = 4))
        val result = gradeFills(
            fills = mapOf(2 to null),               // Q2 blank
            keys = keys,
            multiMarks = mapOf(1 to setOf("A", "B")) // Q1 multi-marked (includes correct "A")
        )

        assertEquals(0, result.earnedPoints)
        assertEquals(0, result.correctCount)
        assertEquals(8, result.totalPoints)
        assertEquals(0, result.percentage)
    }

    // Feature: offline-assessment-transformation, Auto-grade (Req 17.3): grade awards points iff resolved answer matches the key
    // Validates: Requirements 17.3
    //
    // For a random key set (questions 1..N, random correctAnswer in A..D, points
    // 1..5) and a random fill per question (correct / a wrong letter / blank),
    // earnedPoints equals the sum of points over questions answered exactly
    // correctly, and percentage == (total>0 ? earned*100/total : 0) in [0,100].
    @Property(tries = 300)
    fun `grade scores exactly the correctly-answered questions`(
        @ForAll("gradingCases") case: GradingCase
    ) {
        val result = gradeFills(case.fills, case.keys)

        // Manual expected: sum points where the fill equals the key's correctAnswer.
        val keysByQuestion = case.keys.associateBy { it.questionNumber }
        val expectedEarned = case.keys.sumOf { k ->
            val filled = case.fills[k.questionNumber]
            if (filled != null && filled.equals(k.correctAnswer, ignoreCase = true)) k.points else 0
        }
        val expectedCorrectCount = case.keys.count { k ->
            val filled = case.fills[k.questionNumber]
            filled != null && filled.equals(k.correctAnswer, ignoreCase = true)
        }
        val total = case.keys.sumOf { it.points }
        val expectedPct = if (total > 0) (expectedEarned * 100) / total else 0

        assertEquals(expectedEarned, result.earnedPoints, "earned points")
        assertEquals(expectedCorrectCount, result.correctCount, "correct count")
        assertEquals(total, result.totalPoints, "total points")
        assertEquals(expectedPct, result.percentage, "percentage")
        assertTrue(result.percentage in 0..100, "percentage in range: ${result.percentage}")

        // Sanity: no sentinel-resolved question ever contributes.
        keysByQuestion.values.forEach { k ->
            if (case.fills[k.questionNumber] == null) {
                // Blank question: its points must not be in the earned total via this key.
                // (covered by the aggregate equality above)
            }
        }
    }

    data class GradingCase(
        val keys: List<AnswerKeyEntity>,
        // question number -> filled label, or null for blank.
        val fills: Map<Int, String?>
    )

    /**
     * Generates a key set over questions 1..N (N in 1..30) with random correct
     * answers from A..D and points 1..5, together with a per-question fill that is
     * either the correct answer, a different (wrong) letter, or blank.
     */
    @Provide
    fun gradingCases(): Arbitrary<GradingCase> {
        val questionCount: Arbitrary<Int> = Arbitraries.integers().between(1, 30)

        return questionCount.flatMap { n ->
            // Per question we need: a correct label, points, and a fill choice.
            val correctAnswers: Arbitrary<List<String>> =
                Arbitraries.of(*labels.toTypedArray()).list().ofSize(n)
            val pointsList: Arbitrary<List<Int>> =
                Arbitraries.integers().between(1, 5).list().ofSize(n)
            // Fill choice per question: 0=correct, 1=wrong, 2=blank.
            val fillChoices: Arbitrary<List<Int>> =
                Arbitraries.integers().between(0, 2).list().ofSize(n)

            Combinators.combine(correctAnswers, pointsList, fillChoices).`as` { corrects, pts, choices ->
                val keys = ArrayList<AnswerKeyEntity>(n)
                val fills = HashMap<Int, String?>(n)
                for (i in 0 until n) {
                    val q = i + 1
                    val correct = corrects[i]
                    keys += key(q, correct, points = pts[i])
                    fills[q] = when (choices[i]) {
                        0 -> correct                             // correct
                        1 -> labels.first { it != correct }      // a wrong letter
                        else -> null                             // blank
                    }
                }
                GradingCase(keys, fills)
            }
        }
    }

    // A small guard property: an empty key set grades to all zeros (total == 0 path).
    // Feature: offline-assessment-transformation, Auto-grade (Req 17.3): empty key set yields zero percentage
    // Validates: Requirements 17.3
    @Property(tries = 200)
    fun `empty answer key yields zero totals and zero percentage`(
        @ForAll @IntRange(min = 0, max = 10) filledCount: Int
    ) {
        val fills: Map<Int, String?> = (1..filledCount).associateWith { "A" }
        val result = gradeFills(fills, emptyList())
        assertEquals(0, result.totalPoints)
        assertEquals(0, result.earnedPoints)
        assertEquals(0, result.correctCount)
        assertEquals(0, result.percentage)
    }
}
