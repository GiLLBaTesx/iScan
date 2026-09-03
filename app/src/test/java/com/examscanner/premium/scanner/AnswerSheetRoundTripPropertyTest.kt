package com.examscanner.premium.scanner

import com.examscanner.premium.qrcode.QRCodeParser
import com.examscanner.premium.utils.AnswerSheetPrettyPrinter
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Combinators
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * Property-based tests for the answer-sheet print → fill → scan → process
 * pipeline and its idempotence.
 *
 * These are JVM unit tests (jqwik on the JUnit 5 platform) and run WITHOUT a
 * device. They exercise only the pure, deterministic seams:
 *  - [AnswerSheetPrettyPrinter.buildSheetModel] (pure layout; no PDF I/O)
 *  - [AnswerSheetParser.simulateDetection] (deterministic fill simulation)
 *  - [AnswerSheetParser.resolve] (pure multi-mark resolution)
 *
 * The device path ([AnswerSheetParser.parse]) needs a real bitmap + ML Kit, so
 * the design's `parser.parse(simulateFill(...))` sketch is realized here with the
 * equivalent pure pair `simulateDetection` + `resolve`, which share the exact
 * same resolution logic as `parse`.
 *
 * Maps to design Property 6 (Req 21.7, 23.2) and Property 7 (Req 23.4).
 */
class AnswerSheetRoundTripPropertyTest {

    // No processor / bare QR parser: neither pure seam touches those collaborators.
    private val engine = BubbleDetectionEngine()
    private val parser = AnswerSheetParser(QRCodeParser(), engine)

    // The pretty printer is only used for its pure buildSheetModel, which touches
    // neither the QR generator nor a Context, so both are left null here.
    private val prettyPrinter = AnswerSheetPrettyPrinter()

    // Feature: offline-assessment-transformation, Property 6: Answer sheet print-fill-scan-process round-trip
    // Validates: Requirements 21.7, 23.2
    //
    // For any valid exam config (question counts {5,10,20,50,100}, option sets
    // A-B..A-G) and any set of validly filled answers, generating the sheet model,
    // simulating the shaded bubbles, and parsing produces detected answers equal to
    // the filled answers (blanks resolve to "No Answer").
    @Property(tries = 200)
    fun `print fill scan process round trip preserves filled answers`(
        @ForAll("configWithFills") case: RoundTripCase
    ) {
        val model = prettyPrinter.buildSheetModel(case.config)
        val optionLabels = model.optionLabels

        // Map each question to its intended fill (a real label) or blank (null).
        val filled: Map<Int, String?> = model.questions.associate { q ->
            q.questionNumber to case.fills[q.questionNumber]
        }

        val detection = parser.simulateDetection(filled, optionLabels)
        val result = parser.resolve(detection)

        val detected: Map<Int, String> =
            result.answers.associate { it.questionNumber to it.answer }

        // Every question the model produced must appear in the parse result.
        assertEquals(model.totalQuestions, result.answers.size)

        // Each detected answer equals the filled label, blanks -> NO_ANSWER.
        for (q in model.questions) {
            val expected = case.fills[q.questionNumber] ?: AnswerSheetParser.NO_ANSWER
            assertEquals(
                expected,
                detected[q.questionNumber],
                "question ${q.questionNumber} (labels=$optionLabels)"
            )
        }
    }

    // Feature: offline-assessment-transformation, Property 7: Parse-print-parse round-trip is idempotent
    // Validates: Requirements 23.4
    //
    // For any config and parsed answer set, re-simulating from the first parse's
    // answers and parsing again yields an equivalent answer set (stable fixed point).
    @Property(tries = 200)
    fun `parse print parse round trip is idempotent`(
        @ForAll("configWithFills") case: RoundTripCase
    ) {
        val model = prettyPrinter.buildSheetModel(case.config)
        val optionLabels = model.optionLabels

        val filled: Map<Int, String?> = model.questions.associate { q ->
            q.questionNumber to case.fills[q.questionNumber]
        }

        // First parse.
        val firstParse = parser.resolve(parser.simulateDetection(filled, optionLabels))

        // Feed the first parse's answers back through the pipeline. A resolved
        // NO_ANSWER (a blank question) maps back to a blank fill; INVALID_MULTIPLE
        // cannot arise here because each question was filled with at most one label.
        val reFill: Map<Int, String?> = firstParse.answers.associate { a ->
            val label = if (a.answer == AnswerSheetParser.NO_ANSWER) null else a.answer
            a.questionNumber to label
        }

        // Re-print the same config (pure) and parse again.
        val reprinted = prettyPrinter.buildSheetModel(case.config)
        val secondParse =
            parser.resolve(parser.simulateDetection(reFill, reprinted.optionLabels))

        assertEquals(
            firstParse.answers.map { it.questionNumber to it.answer },
            secondParse.answers.map { it.questionNumber to it.answer }
        )
    }

    /**
     * A single round-trip case: an exam config plus the answers a student filled.
     */
    data class RoundTripCase(
        val config: AnswerSheetPrettyPrinter.SheetConfig,
        // question number -> filled option label, or null for a blank question.
        val fills: Map<Int, String?>
    )

    /**
     * Generates a valid exam configuration (question count in {5,10,20,50,100},
     * option set A-B..A-G) together with a valid set of filled answers: each
     * question is either blank or filled with exactly one of the sheet's labels.
     */
    @Provide
    fun configWithFills(): Arbitrary<RoundTripCase> {
        val questionCounts = Arbitraries.of(5, 10, 20, 50, 100)
        val optionCounts = Arbitraries.integers().between(2, 7) // A-B .. A-G

        return Combinators.combine(questionCounts, optionCounts)
            .`as` { total, optionCount ->
                val labels = (0 until optionCount).map { ('A' + it).toString() }
                total to labels
            }
            .flatMap { (total, labels) ->
                // For each question, pick a label or null (blank). Use "" as a
                // stand-in for blank inside the generator, then map to null.
                val cellChoices = Arbitraries.of(labels + listOf(BLANK_MARKER))
                cellChoices.list().ofSize(total).map { cells ->
                    val fills = HashMap<Int, String?>(total)
                    cells.forEachIndexed { index, cell ->
                        fills[index + 1] = if (cell == BLANK_MARKER) null else cell
                    }
                    val config = AnswerSheetPrettyPrinter.SheetConfig(
                        examName = "Round Trip Exam",
                        totalQuestions = total,
                        optionsCount = labels.size,
                        optionsLabels = labels
                    )
                    RoundTripCase(config, fills)
                }
            }
    }

    companion object {
        /** Internal sentinel used only inside the generator to represent a blank. */
        private const val BLANK_MARKER = "\u0000BLANK"
    }
}
