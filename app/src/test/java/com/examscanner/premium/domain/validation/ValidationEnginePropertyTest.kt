package com.examscanner.premium.domain.validation

import com.examscanner.premium.domain.validation.ValidationEngine.Companion.MAX_EXAM_NAME_LENGTH
import com.examscanner.premium.domain.validation.ValidationEngine.Companion.MAX_NOTE_LENGTH
import com.examscanner.premium.domain.validation.ValidationEngine.Companion.MAX_QUESTION_COUNT
import com.examscanner.premium.domain.validation.ValidationEngine.Companion.MAX_SECTION_NAME_LENGTH
import com.examscanner.premium.domain.validation.ValidationEngine.Companion.MAX_STUDENT_ID_LENGTH
import com.examscanner.premium.domain.validation.ValidationEngine.Companion.MIN_QUESTION_COUNT
import com.examscanner.premium.domain.validation.ValidationEngine.ValidationResult
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Assume
import net.jqwik.api.Combinators
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import net.jqwik.api.constraints.IntRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Property-based tests for [ValidationEngine]'s pure, DB-free input validation.
 *
 * These are JVM unit tests (jqwik on the JUnit 5 platform) and run WITHOUT a
 * device or database. They close the spec's previously-incomplete optional task
 * 4.8 by implementing design Property 10 ("input validation partitions inputs
 * correctly", Req 7.2 / 8.5 / 24.1 / 24.2 / 24.3): every input drawn from a
 * validator's INVALID partition yields [ValidationResult.Invalid], and every
 * input drawn from its VALID partition yields [ValidationResult.Valid].
 *
 * Generators draw from disjoint partitions so a "valid" value can never
 * accidentally cross into the invalid space (e.g. valid exam names use a safe
 * alphabet that excludes the 9 reserved characters and are never blank).
 */
class ValidationEnginePropertyTest {

    private val v = ValidationEngine()

    // The 9 characters ValidationEngine rejects in exam names: < > : " / \ | ? *
    private val invalidNameChars = listOf('<', '>', ':', '"', '/', '\\', '|', '?', '*')

    // region validateQuestionCount ------------------------------------------------------

    // Feature: offline-assessment-transformation, Property 10: input validation partitions inputs correctly
    // Validates: Requirements 24.1
    //
    // VALID partition: counts in [MIN_QUESTION_COUNT, MAX_QUESTION_COUNT] -> Valid.
    @Property(tries = 300)
    fun `question count within range is valid`(
        @ForAll @IntRange(min = MIN_QUESTION_COUNT, max = MAX_QUESTION_COUNT) count: Int
    ) {
        assertTrue(v.validateQuestionCount(count).isValid, "count=$count should be valid")
    }

    // Feature: offline-assessment-transformation, Property 10: input validation partitions inputs correctly
    // Validates: Requirements 24.1
    //
    // INVALID partition: counts below MIN or above MAX -> Invalid.
    @Property(tries = 300)
    fun `question count out of range is invalid`(@ForAll count: Int) {
        Assume.that(count < MIN_QUESTION_COUNT || count > MAX_QUESTION_COUNT)
        assertFalse(v.validateQuestionCount(count).isValid, "count=$count should be invalid")
    }

    // endregion

    // region validateExamName -----------------------------------------------------------

    // Feature: offline-assessment-transformation, Property 10: input validation partitions inputs correctly
    // Validates: Requirements 24.1
    //
    // VALID partition: non-blank names of length 1..MAX over a safe alphabet that
    // excludes the 9 reserved characters -> Valid.
    @Property(tries = 300)
    fun `exam name from safe alphabet within length is valid`(
        @ForAll("validExamNames") name: String
    ) {
        assertTrue(v.validateExamName(name).isValid, "name='$name' should be valid")
    }

    // Feature: offline-assessment-transformation, Property 10: input validation partitions inputs correctly
    // Validates: Requirements 24.1
    //
    // INVALID partition: blank, too long, or containing a reserved char -> Invalid.
    @Property(tries = 300)
    fun `exam name that is blank too long or has reserved char is invalid`(
        @ForAll("invalidExamNames") name: String
    ) {
        assertFalse(v.validateExamName(name).isValid, "name='$name' should be invalid")
    }

    // endregion

    // region validateStudentIdFormat ----------------------------------------------------

    // Feature: offline-assessment-transformation, Property 10: input validation partitions inputs correctly
    // Validates: Requirements 24.2
    //
    // VALID partition: non-empty [A-Za-z0-9-] ids of length 1..MAX -> Valid.
    @Property(tries = 300)
    fun `student id format with legal chars and length is valid`(
        @ForAll("validStudentIds") id: String
    ) {
        assertTrue(v.validateStudentIdFormat(id).isValid, "id='$id' should be valid format")
    }

    // Feature: offline-assessment-transformation, Property 10: input validation partitions inputs correctly
    // Validates: Requirements 24.2
    //
    // INVALID partition: blank, too long, or containing an out-of-set char -> Invalid.
    @Property(tries = 300)
    fun `student id format that is blank too long or illegal is invalid`(
        @ForAll("invalidStudentIds") id: String
    ) {
        assertFalse(v.validateStudentIdFormat(id).isValid, "id='$id' should be invalid format")
    }

    // endregion

    // region validateSectionName --------------------------------------------------------

    // Feature: offline-assessment-transformation, Property 10: input validation partitions inputs correctly
    // Validates: Requirements 7.2
    //
    // VALID partition: non-blank names of length 1..MAX_SECTION_NAME_LENGTH -> Valid.
    @Property(tries = 300)
    fun `section name non blank within length is valid`(
        @ForAll("validSectionNames") name: String
    ) {
        assertTrue(v.validateSectionName(name).isValid, "name='$name' should be valid")
    }

    // Feature: offline-assessment-transformation, Property 10: input validation partitions inputs correctly
    // Validates: Requirements 7.2
    //
    // INVALID partition: blank or longer than MAX_SECTION_NAME_LENGTH -> Invalid.
    @Property(tries = 300)
    fun `section name blank or too long is invalid`(
        @ForAll("invalidSectionNames") name: String
    ) {
        assertFalse(v.validateSectionName(name).isValid, "name='$name' should be invalid")
    }

    // endregion

    // region validateNote ---------------------------------------------------------------

    // Feature: offline-assessment-transformation, Property 10: input validation partitions inputs correctly
    // Validates: Requirements 8.5
    //
    // VALID partition: notes of length 0..MAX_NOTE_LENGTH (empty allowed) -> Valid.
    @Property(tries = 300)
    fun `note within length limit is valid`(
        @ForAll("validNotes") note: String
    ) {
        assertTrue(v.validateNote(note).isValid, "note length=${note.length} should be valid")
    }

    // Feature: offline-assessment-transformation, Property 10: input validation partitions inputs correctly
    // Validates: Requirements 8.5
    //
    // INVALID partition: notes longer than MAX_NOTE_LENGTH -> Invalid.
    @Property(tries = 300)
    fun `note over length limit is invalid`(
        @ForAll("tooLongNotes") note: String
    ) {
        assertFalse(v.validateNote(note).isValid, "note length=${note.length} should be invalid")
    }

    // endregion

    // region validateStudentId (uniqueness) ---------------------------------------------

    // Feature: offline-assessment-transformation, Property 10: input validation partitions inputs correctly
    // Validates: Requirements 24.2
    //
    // Uniqueness partitioning: a format-valid id that case-insensitively collides
    // with the existing section ids -> Invalid; a format-valid id absent from the
    // set -> Valid; a format-invalid id -> Invalid regardless of the set.
    @Property(tries = 300)
    fun `student id uniqueness partitions on duplicate and format`(
        @ForAll("validStudentIds") id: String,
        @ForAll("validStudentIds") other: String
    ) {
        // Duplicate (case-insensitive variant of the same id) -> Invalid.
        val dupeResult = v.validateStudentId(id, listOf(id.lowercase(), "ZZ-OTHER"))
        assertFalse(dupeResult.isValid, "duplicate id='$id' should be invalid")
        assertTrue(dupeResult is ValidationResult.Invalid)

        // A different (non-colliding) format-valid id -> Valid.
        Assume.that(!other.equals(id, ignoreCase = true))
        assertTrue(
            v.validateStudentId(other, listOf(id)).isValid,
            "non-duplicate id='$other' should be valid"
        )

        // Format-invalid id (contains illegal char) -> Invalid regardless of set.
        assertFalse(
            v.validateStudentId("bad id!", listOf(id)).isValid,
            "format-invalid id should be invalid"
        )
    }

    // endregion

    // region validateMelcMapping --------------------------------------------------------

    // Feature: offline-assessment-transformation, Property 10: input validation partitions inputs correctly
    // Validates: Requirements 24.3
    //
    // MELC mapping partitioning: non-existent MELC -> Invalid; existing MELC that
    // is already mapped -> Invalid; existing MELC not yet mapped -> Valid.
    @Property(tries = 300)
    fun `melc mapping partitions on existence and duplication`(
        @ForAll melcId: Long,
        @ForAll("otherMelcIds") existing: List<Long>
    ) {
        // Does not exist -> Invalid regardless of mappings.
        assertFalse(
            v.validateMelcMapping(melcId, melcExists = false, existingMelcMappings = existing).isValid,
            "non-existent melc should be invalid"
        )

        // Exists but already mapped -> Invalid.
        assertFalse(
            v.validateMelcMapping(melcId, melcExists = true, existingMelcMappings = existing + melcId).isValid,
            "duplicate melc mapping should be invalid"
        )

        // Exists and not duplicated -> Valid.
        val notDuplicated = existing.filter { it != melcId }
        assertTrue(
            v.validateMelcMapping(melcId, melcExists = true, existingMelcMappings = notDuplicated).isValid,
            "existing non-duplicate melc should be valid"
        )
    }

    // endregion

    // region Explicit boundary edges ----------------------------------------------------

    @Test
    fun `question count boundary edges`() {
        assertFalse(v.validateQuestionCount(0).isValid)
        assertTrue(v.validateQuestionCount(MIN_QUESTION_COUNT).isValid)        // 1
        assertTrue(v.validateQuestionCount(MAX_QUESTION_COUNT).isValid)        // 200
        assertFalse(v.validateQuestionCount(MAX_QUESTION_COUNT + 1).isValid)   // 201
    }

    @Test
    fun `exam name length boundary edges`() {
        assertTrue(v.validateExamName("A".repeat(MAX_EXAM_NAME_LENGTH)).isValid)        // 200
        assertFalse(v.validateExamName("A".repeat(MAX_EXAM_NAME_LENGTH + 1)).isValid)   // 201
    }

    @Test
    fun `student id length boundary edges`() {
        assertTrue(v.validateStudentIdFormat("a".repeat(MAX_STUDENT_ID_LENGTH)).isValid)       // 20
        assertFalse(v.validateStudentIdFormat("a".repeat(MAX_STUDENT_ID_LENGTH + 1)).isValid)  // 21
    }

    @Test
    fun `section name length boundary edges`() {
        assertTrue(v.validateSectionName("S".repeat(MAX_SECTION_NAME_LENGTH)).isValid)       // 50
        assertFalse(v.validateSectionName("S".repeat(MAX_SECTION_NAME_LENGTH + 1)).isValid)  // 51
    }

    @Test
    fun `note length boundary edges`() {
        assertTrue(v.validateNote("n".repeat(MAX_NOTE_LENGTH)).isValid)        // 500
        assertTrue(v.validateNote("").isValid)                                // empty allowed
        assertFalse(v.validateNote("n".repeat(MAX_NOTE_LENGTH + 1)).isValid)  // 501
    }

    // endregion

    // region Generators -----------------------------------------------------------------

    /** Non-blank names of length 1..MAX over a safe alphabet (no reserved chars, no blank). */
    @Provide
    fun validExamNames(): Arbitrary<String> =
        Arbitraries.strings()
            .withChars(*"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 -_.".toCharArray())
            .ofMinLength(1)
            .ofMaxLength(MAX_EXAM_NAME_LENGTH)
            // A space-only string is blank; keep at least one non-space char.
            .filter { it.isNotBlank() }

    /** Invalid names: blank, too long, or containing a reserved character. */
    @Provide
    fun invalidExamNames(): Arbitrary<String> {
        val blank = Arbitraries.strings().withChars(' ', '\t', '\n').ofMinLength(0).ofMaxLength(5)
        val tooLong = Arbitraries.strings()
            .withChars(*"ABCDEabcde".toCharArray())
            .ofMinLength(MAX_EXAM_NAME_LENGTH + 1)
            .ofMaxLength(MAX_EXAM_NAME_LENGTH + 20)
        val withReserved = Combinators.combine(
            Arbitraries.strings().withChars(*"ABCabc123".toCharArray()).ofMinLength(0).ofMaxLength(10),
            Arbitraries.of(*invalidNameChars.toTypedArray())
        ).`as` { prefix, bad -> prefix + bad }
        return Arbitraries.oneOf(blank, tooLong, withReserved)
    }

    /** Non-empty ids of length 1..MAX over [A-Za-z0-9-]. */
    @Provide
    fun validStudentIds(): Arbitrary<String> =
        Arbitraries.strings()
            .withChars(*"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-".toCharArray())
            .ofMinLength(1)
            .ofMaxLength(MAX_STUDENT_ID_LENGTH)

    /** Invalid ids: blank, too long, or containing a char outside [A-Za-z0-9-]. */
    @Provide
    fun invalidStudentIds(): Arbitrary<String> {
        val blank = Arbitraries.strings().withChars(' ', '\t').ofMinLength(0).ofMaxLength(3)
        val tooLong = Arbitraries.strings()
            .withChars(*"ABCabc123-".toCharArray())
            .ofMinLength(MAX_STUDENT_ID_LENGTH + 1)
            .ofMaxLength(MAX_STUDENT_ID_LENGTH + 15)
        // Illegal chars: anything not in the legal set. Use spaces/punctuation/unicode.
        val illegal = Combinators.combine(
            Arbitraries.strings().withChars(*"ABCabc123".toCharArray()).ofMinLength(0).ofMaxLength(8),
            Arbitraries.of(' ', '_', '.', '/', '@', '#', '!', '%')
        ).`as` { prefix, bad -> prefix + bad }
        return Arbitraries.oneOf(blank, tooLong, illegal)
    }

    /** Non-blank names of length 1..MAX_SECTION_NAME_LENGTH. */
    @Provide
    fun validSectionNames(): Arbitrary<String> =
        Arbitraries.strings()
            .withChars(*"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 -".toCharArray())
            .ofMinLength(1)
            .ofMaxLength(MAX_SECTION_NAME_LENGTH)
            .filter { it.isNotBlank() }

    /** Invalid section names: blank or longer than MAX_SECTION_NAME_LENGTH. */
    @Provide
    fun invalidSectionNames(): Arbitrary<String> {
        val blank = Arbitraries.strings().withChars(' ', '\t', '\n').ofMinLength(0).ofMaxLength(5)
        val tooLong = Arbitraries.strings()
            .withChars(*"ABCabc123".toCharArray())
            .ofMinLength(MAX_SECTION_NAME_LENGTH + 1)
            .ofMaxLength(MAX_SECTION_NAME_LENGTH + 20)
        return Arbitraries.oneOf(blank, tooLong)
    }

    /** Notes of length 0..MAX_NOTE_LENGTH (empty allowed). */
    @Provide
    fun validNotes(): Arbitrary<String> =
        Arbitraries.strings()
            .withChars(*"ABCabc123 .,-".toCharArray())
            .ofMinLength(0)
            .ofMaxLength(MAX_NOTE_LENGTH)

    /** Notes longer than MAX_NOTE_LENGTH. */
    @Provide
    fun tooLongNotes(): Arbitrary<String> =
        Arbitraries.strings()
            .withChars(*"ABCabc123 ".toCharArray())
            .ofMinLength(MAX_NOTE_LENGTH + 1)
            .ofMaxLength(MAX_NOTE_LENGTH + 50)

    /** Small lists of MELC ids used to seed existing-mapping checks. */
    @Provide
    fun otherMelcIds(): Arbitrary<List<Long>> =
        Arbitraries.longs().between(1L, 1_000L).list().ofMinSize(0).ofMaxSize(10)

    // endregion
}
