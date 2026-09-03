package com.examscanner.premium.scanner

import android.graphics.Bitmap
import com.examscanner.premium.data.AnswerKeyEntity
import com.examscanner.premium.qrcode.QRCodeGenerator
import com.examscanner.premium.qrcode.QRCodeParser
import com.examscanner.premium.utils.SecureLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AnswerSheetParser - orchestrates the full scan-to-answers pipeline.
 *
 * Covers Requirement 22 (Parser for Scanned Answer Sheets) and integrates with
 * Requirement 17 (Preserve Existing Scanner). Design reference: design.md §12
 * (Answer Sheet Parser System).
 *
 * Pipeline (Req 22.1-22.7):
 *  1. QR-first detection via [QRCodeParser.parseQRCode] (Req 4.4, 22.1, 22.2).
 *  2. Bubble detection via [BubbleDetectionEngine.detectBubbles] as the always-run
 *     source of answers, and the fallback when QR fails (Req 4.7, 17.3, 22.3).
 *  3. Multi-mark resolution per question (Req 22.6): zero SHADED -> "No Answer",
 *     exactly one SHADED -> that option's label, more than one SHADED ->
 *     "Invalid - Multiple".
 *  4. Confidence scores carried through from the detector (Req 22.7).
 *  5. A structured [ParseResult] (Req 4.5, 22.7).
 *
 * This service references the shared [BubbleDetectionEngine.BubbleState] enum
 * rather than redefining it (the design's `AnswerSheetParser.BubbleState` maps to
 * that shared type). It does not redefine detection/QR types.
 *
 * Auto-grading (Req 17.3) is preserved via [grade], which compares the resolved
 * answers against the existing [AnswerKeyEntity] rows (the same comparison used by
 * `ExamRepository.calculateScore`), honoring `alternativeAnswers` and `points`.
 *
 * A deterministic [simulateDetection] path lets the round-trip property tests
 * (5.8/5.9) run without a device, camera, or real image I/O.
 *
 * @param qrParser decodes embedded [QRCodeGenerator.ExamMetadata].
 * @param bubbleDetector classifies option fill and produces per-question detections.
 */
class AnswerSheetParser(
    private val qrParser: QRCodeParser,
    private val bubbleDetector: BubbleDetectionEngine
) {

    /** Resolved answer sentinel used when no option is shaded (Req 22.6). */
    val noAnswerLabel: String get() = NO_ANSWER

    /** Resolved answer sentinel used when more than one option is shaded (Req 22.6). */
    val invalidMultipleLabel: String get() = INVALID_MULTIPLE

    /**
     * Structured output of the parse pipeline (Req 22.7).
     *
     * @param examMetadata decoded QR metadata, or null when no QR code was found
     *   or it could not be decoded (bubble-only fallback path, Req 4.7).
     * @param studentId student id extracted from the sheet, if any.
     * @param studentName student name extracted from the sheet, if any.
     * @param answers resolved per-question answers with confidence.
     * @param overallConfidence mean detector confidence across questions (0-1).
     * @param usedQrCode true when QR metadata was decoded and used to identify the exam.
     */
    data class ParseResult(
        val examMetadata: QRCodeGenerator.ExamMetadata?,
        val studentId: String?,
        val studentName: String?,
        val answers: List<DetectedAnswer>,
        val overallConfidence: Float,
        val usedQrCode: Boolean
    )

    /**
     * A single resolved answer.
     *
     * @param questionNumber 1-based question number.
     * @param answer resolved label: "A".."G", [NO_ANSWER], or [INVALID_MULTIPLE].
     * @param bubbleState the aggregate state for the question:
     *   SHADED (single answer), EMPTY (no answer), or MULTIPLE (invalid).
     * @param confidence detector confidence for the question (0-1).
     */
    data class DetectedAnswer(
        val questionNumber: Int,
        val answer: String,
        val bubbleState: BubbleDetectionEngine.BubbleState,
        val confidence: Float
    )

    /**
     * Run the full parse pipeline over a scanned [bitmap].
     *
     * QR detection and bubble detection are both attempted. Bubble detection is
     * always the source of the answers; QR only supplies exam identification, so
     * a QR failure degrades gracefully to bubble-only processing (Req 4.7, 17.3).
     *
     * @param grid the option-region layout to sample (derived from the printed
     *   [AnswerSheetPrettyPrinter] sheet / template). Defaults to
     *   [BubbleDetectionEngine.BubbleGrid.EMPTY] which yields no answers.
     */
    suspend fun parse(
        bitmap: Bitmap,
        grid: BubbleDetectionEngine.BubbleGrid = BubbleDetectionEngine.BubbleGrid.EMPTY
    ): ParseResult = withContext(Dispatchers.Default) {
        // Req 22.1/22.2 - QR-first. Never throws; null means bubble-only fallback.
        val metadata = runCatching { qrParser.parseQRCode(bitmap) }
            .getOrElse {
                SecureLogger.e(TAG, "QR parse failed; falling back to bubble-only", it)
                null
            }

        // Req 22.3/22.4 - bubble detection (image handling delegated to the processor).
        val detection = bubbleDetector.detectBubbles(bitmap, grid)

        resolve(detection, metadata)
    }

    /**
     * Resolve a [BubbleDetectionEngine.DetectionResult] into a [ParseResult].
     *
     * Pure with respect to its inputs (no image I/O), so both the device path
     * ([parse]) and the deterministic simulated path ([simulateDetection]) share
     * the exact same resolution logic, keeping round-trip tests faithful.
     */
    fun resolve(
        detection: BubbleDetectionEngine.DetectionResult,
        metadata: QRCodeGenerator.ExamMetadata? = null
    ): ParseResult {
        val answers = detection.questions.map { q -> resolveQuestion(q) }
        return ParseResult(
            examMetadata = metadata,
            studentId = detection.studentId,
            studentName = detection.studentName,
            answers = answers,
            overallConfidence = detection.overallConfidence,
            usedQrCode = metadata != null
        )
    }

    /**
     * Multi-mark resolution for a single question (Req 22.6):
     *  - zero SHADED options -> [NO_ANSWER] (bubbleState EMPTY)
     *  - exactly one SHADED option -> that option's label (bubbleState SHADED)
     *  - more than one SHADED option -> [INVALID_MULTIPLE] (bubbleState MULTIPLE)
     */
    fun resolveQuestion(q: BubbleDetectionEngine.QuestionDetection): DetectedAnswer {
        val shaded = q.options.filter { it.state == BubbleDetectionEngine.BubbleState.SHADED }
        val (answer, state) = when {
            shaded.isEmpty() -> NO_ANSWER to BubbleDetectionEngine.BubbleState.EMPTY
            shaded.size == 1 -> shaded.first().label to BubbleDetectionEngine.BubbleState.SHADED
            else -> INVALID_MULTIPLE to BubbleDetectionEngine.BubbleState.MULTIPLE
        }
        return DetectedAnswer(
            questionNumber = q.number,
            answer = answer,
            bubbleState = state,
            confidence = q.confidence
        )
    }

    /**
     * Auto-grade a [ParseResult] against the exam's answer key (Req 17.3).
     *
     * Mirrors `ExamRepository.calculateScore`: a question is correct when the
     * resolved label equals the key's `correctAnswer` (or one of the
     * comma-separated `alternativeAnswers`), awarding the key's `points`. The
     * returned percentage is `earnedPoints * 100 / totalPoints`, or 0 when the
     * key has no points. [NO_ANSWER] / [INVALID_MULTIPLE] answers never match a key.
     *
     * @return a [GradeResult] with earned/total points and the integer percentage.
     */
    fun grade(result: ParseResult, answerKeys: List<AnswerKeyEntity>): GradeResult {
        var earned = 0
        var correctCount = 0
        val answersByQuestion = result.answers.associateBy { it.questionNumber }

        answerKeys.forEach { key ->
            val detected = answersByQuestion[key.questionNumber]?.answer ?: return@forEach
            if (detected == NO_ANSWER || detected == INVALID_MULTIPLE) return@forEach
            if (matchesKey(detected, key)) {
                earned += key.points
                correctCount++
            }
        }

        val total = answerKeys.sumOf { it.points }
        val percentage = if (total > 0) (earned * 100) / total else 0
        return GradeResult(
            earnedPoints = earned,
            totalPoints = total,
            correctCount = correctCount,
            gradedQuestions = answerKeys.size,
            percentage = percentage
        )
    }

    private fun matchesKey(answer: String, key: AnswerKeyEntity): Boolean {
        if (answer.equals(key.correctAnswer, ignoreCase = true)) return true
        return key.alternativeAnswers
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .any { it.equals(answer, ignoreCase = true) }
    }

    /**
     * Outcome of auto-grading (Req 17.3).
     */
    data class GradeResult(
        val earnedPoints: Int,
        val totalPoints: Int,
        val correctCount: Int,
        val gradedQuestions: Int,
        val percentage: Int
    )

    /**
     * Deterministic detector used by round-trip property tests (5.8/5.9) so they
     * can run without a device, camera, or real image I/O.
     *
     * Given the intended [filledAnswers] (question number -> the option a student
     * filled, or null for a blank question) and the full [optionLabels] for each
     * question, this produces a [BubbleDetectionEngine.DetectionResult] whose
     * option fill ratios classify deterministically: the intended option is fully
     * SHADED (1.0) and the rest EMPTY (0.0). Feeding the result through [resolve]
     * reproduces the intended labels exactly, giving a faithful
     * print -> fill -> scan -> process round trip.
     *
     * @param filledAnswers per-question intended fill; a question mapped to a
     *   label not present in [optionLabels] (or to a list of labels) can be used
     *   by callers to simulate multi-marks by supplying [multiMarks].
     * @param optionLabels the option labels present on the sheet, e.g. A..D.
     * @param multiMarks optional per-question set of labels to shade simultaneously
     *   (simulates an "Invalid - Multiple" case). Overrides [filledAnswers] for
     *   any question it contains.
     * @param studentId optional simulated extracted student id.
     * @param studentName optional simulated extracted student name.
     */
    fun simulateDetection(
        filledAnswers: Map<Int, String?>,
        optionLabels: List<String>,
        multiMarks: Map<Int, Set<String>> = emptyMap(),
        studentId: String? = null,
        studentName: String? = null
    ): BubbleDetectionEngine.DetectionResult {
        val questionNumbers = (filledAnswers.keys + multiMarks.keys).sorted()
        val questions = questionNumbers.map { number ->
            val shadedLabels: Set<String> = when {
                multiMarks.containsKey(number) -> multiMarks.getValue(number)
                else -> filledAnswers[number]?.let { setOf(it) } ?: emptySet()
            }
            val options = optionLabels.map { label ->
                val ratio = if (label in shadedLabels) SIM_SHADED else SIM_EMPTY
                BubbleDetectionEngine.OptionDetection(
                    label = label,
                    fillRatio = ratio,
                    state = bubbleDetector.classifyFill(ratio)
                )
            }
            bubbleDetector.buildQuestionDetection(number, options)
        }
        return BubbleDetectionEngine.DetectionResult(
            studentId = studentId,
            studentName = studentName,
            questions = questions,
            overallConfidence = if (questions.isEmpty()) 0f
                else questions.map { it.confidence }.average().toFloat()
        )
    }

    companion object {
        private const val TAG = "AnswerSheetParser"

        /** Resolved label when a question has no shaded option (Req 22.6). */
        const val NO_ANSWER = "No Answer"

        /** Resolved label when a question has more than one shaded option (Req 22.6). */
        const val INVALID_MULTIPLE = "Invalid - Multiple"

        /** Fill ratios used by the deterministic simulated detector. */
        private const val SIM_SHADED = 1.0f
        private const val SIM_EMPTY = 0.0f
    }
}
