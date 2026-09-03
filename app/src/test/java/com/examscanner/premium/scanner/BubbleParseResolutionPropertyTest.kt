package com.examscanner.premium.scanner

import com.examscanner.premium.qrcode.QRCodeParser
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import net.jqwik.api.constraints.FloatRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Property-based tests for the parser's pure bubble-parse resolution and the
 * pure fill-ratio classification. These are JVM unit tests (jqwik on the JUnit 5
 * platform) and run WITHOUT a device: they only exercise the pure seams
 * [BubbleDetectionEngine.classifyFill], [BubbleDetectionEngine.buildQuestionDetection]
 * and [AnswerSheetParser.resolveQuestion], none of which touch a camera, bitmap,
 * or the ML Kit collaborators.
 *
 * Maps to design Property 8 (Req 22.5, 22.6).
 */
class BubbleParseResolutionPropertyTest {

    // The engine/parser are built with no processor and a bare QR parser because
    // the pure resolution/classification methods never invoke either collaborator.
    private val engine = BubbleDetectionEngine()
    private val parser = AnswerSheetParser(QRCodeParser(), engine)

    private val allLabels = listOf("A", "B", "C", "D", "E", "F", "G")

    // Feature: offline-assessment-transformation, Property 8: Bubble parse resolution
    // Validates: Requirements 22.5, 22.6
    //
    // For any question's detected option set: zero SHADED -> "No Answer",
    // exactly one SHADED -> that option's label, more than one SHADED ->
    // "Invalid - Multiple".
    @Property(tries = 200)
    fun `multi-mark resolution follows the shaded-count rule`(
        @ForAll("questionOptions") options: List<BubbleDetectionEngine.OptionDetection>
    ) {
        val question = engine.buildQuestionDetection(number = 1, options = options)
        val resolved = parser.resolveQuestion(question)

        val shaded = options.filter { it.state == BubbleDetectionEngine.BubbleState.SHADED }

        when (shaded.size) {
            0 -> {
                assertEquals(AnswerSheetParser.NO_ANSWER, resolved.answer)
                assertEquals(BubbleDetectionEngine.BubbleState.EMPTY, resolved.bubbleState)
            }
            1 -> {
                assertEquals(shaded.single().label, resolved.answer)
                assertEquals(BubbleDetectionEngine.BubbleState.SHADED, resolved.bubbleState)
            }
            else -> {
                assertEquals(AnswerSheetParser.INVALID_MULTIPLE, resolved.answer)
                assertEquals(BubbleDetectionEngine.BubbleState.MULTIPLE, resolved.bubbleState)
            }
        }

        // The question number is carried through unchanged.
        assertEquals(1, resolved.questionNumber)
    }

    // Feature: offline-assessment-transformation, Property 8: Bubble parse resolution
    // Validates: Requirements 22.5, 22.6
    //
    // For any fill ratio in 0.0-1.0, classifyFill returns EMPTY (<=0.20),
    // PARTIAL (0.21-0.79), or SHADED (>=0.80).
    @Property(tries = 500)
    fun `fill ratio classification matches the defined bands`(
        @ForAll @FloatRange(min = 0f, max = 1f) fillRatio: Float
    ) {
        val expected = when {
            fillRatio <= BubbleDetectionEngine.EMPTY_THRESHOLD ->
                BubbleDetectionEngine.BubbleState.EMPTY
            fillRatio < BubbleDetectionEngine.SHADED_THRESHOLD ->
                BubbleDetectionEngine.BubbleState.PARTIAL
            else -> BubbleDetectionEngine.BubbleState.SHADED
        }

        val actual = engine.classifyFill(fillRatio)

        assertEquals(expected, actual, "fillRatio=$fillRatio")
        // classifyFill never produces the resolution-only MULTIPLE state.
        assertTrue(
            actual in setOf(
                BubbleDetectionEngine.BubbleState.EMPTY,
                BubbleDetectionEngine.BubbleState.PARTIAL,
                BubbleDetectionEngine.BubbleState.SHADED
            ),
            "unexpected state $actual for fillRatio=$fillRatio"
        )
    }

    // Feature: offline-assessment-transformation, Property 8: Bubble parse resolution
    // Validates: Requirements 22.5, 22.6
    //
    // Out-of-range fill ratios are coerced deterministically: anything <= 0
    // classifies EMPTY and anything >= 1 classifies SHADED.
    @Property(tries = 200)
    fun `fill ratio classification coerces out-of-range inputs`(
        @ForAll @FloatRange(min = -100f, max = 100f) fillRatio: Float
    ) {
        val coerced = fillRatio.coerceIn(0f, 1f)
        val expected = when {
            coerced <= BubbleDetectionEngine.EMPTY_THRESHOLD ->
                BubbleDetectionEngine.BubbleState.EMPTY
            coerced < BubbleDetectionEngine.SHADED_THRESHOLD ->
                BubbleDetectionEngine.BubbleState.PARTIAL
            else -> BubbleDetectionEngine.BubbleState.SHADED
        }
        assertEquals(expected, engine.classifyFill(fillRatio), "fillRatio=$fillRatio")
    }

    // Feature: offline-assessment-transformation, Property 8: band edges classify exactly
    // Validates: Requirements 22.5
    @Property(tries = 100)
    fun `classification band edges are exact`(
        @ForAll("bandEdges") edge: Float
    ) {
        val expected = when {
            edge <= 0.20f -> BubbleDetectionEngine.BubbleState.EMPTY
            edge < 0.80f -> BubbleDetectionEngine.BubbleState.PARTIAL
            else -> BubbleDetectionEngine.BubbleState.SHADED
        }
        assertEquals(expected, engine.classifyFill(edge), "edge=$edge")
    }

    @Provide
    fun bandEdges(): Arbitrary<Float> = Arbitraries.of(
        0f, 0.20f, 0.200001f, 0.21f, 0.79f, 0.799999f, 0.80f, 0.800001f, 1f
    )

    /**
     * Generates a realistic per-question option set: 2..7 options labelled A..,
     * each with an independently generated state. This spans the zero / one /
     * many shaded cases the resolution rule partitions on.
     */
    @Provide
    fun questionOptions(): Arbitrary<List<BubbleDetectionEngine.OptionDetection>> {
        return Arbitraries.integers().between(2, 7).flatMap { count ->
            val labels = allLabels.take(count)
            val states: Arbitrary<BubbleDetectionEngine.BubbleState> = Arbitraries.of(
                BubbleDetectionEngine.BubbleState.EMPTY,
                BubbleDetectionEngine.BubbleState.PARTIAL,
                BubbleDetectionEngine.BubbleState.SHADED
            )
            states.list().ofSize(count).map { stateList ->
                labels.mapIndexed { i, label ->
                    val ratio = when (stateList[i]) {
                        BubbleDetectionEngine.BubbleState.EMPTY -> 0.0f
                        BubbleDetectionEngine.BubbleState.PARTIAL -> 0.5f
                        BubbleDetectionEngine.BubbleState.SHADED -> 1.0f
                        else -> 0.0f
                    }
                    BubbleDetectionEngine.OptionDetection(
                        label = label,
                        fillRatio = ratio,
                        state = stateList[i]
                    )
                }
            }
        }
    }
}
