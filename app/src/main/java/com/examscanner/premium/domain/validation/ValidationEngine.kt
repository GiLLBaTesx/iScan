package com.examscanner.premium.domain.validation

import com.examscanner.premium.utils.InputSanitizer
import java.io.File

/**
 * ValidationEngine - Centralized data integrity checks (Requirement 24).
 *
 * Design goals:
 * - Pure, DB-free format checks (empty/length/range/character rules) are exposed as
 *   deterministic functions so they can be exercised by property tests (Property 10)
 *   without a database.
 * - Checks that require a data lookup (student ID uniqueness within a section,
 *   MELC existence / duplicate-mapping) accept the relevant facts as parameters
 *   (existing IDs, existence flags, existing mappings) rather than reaching into a
 *   repository/DAO directly. This keeps the engine testable and side-effect free;
 *   callers supply the looked-up data from their repositories.
 * - Reuses [InputSanitizer] for character-level rules where it already encodes the
 *   app's injection/path-traversal policy.
 *
 * Validates: Requirements 7.2, 8.5, 24.1, 24.2, 24.3, 24.4, 24.6, 24.7
 */
class ValidationEngine {

    /**
     * Typed validation outcome. [Valid] carries no data; [Invalid] carries a
     * specific, user-facing message (Req 24.6).
     */
    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val message: String) : ValidationResult()

        val isValid: Boolean get() = this is Valid
    }

    // region Pure format checks (no DB) -------------------------------------------------

    /**
     * Req 24.1: exam name must be non-empty and reasonably bounded.
     */
    fun validateExamName(name: String): ValidationResult = when {
        name.isBlank() ->
            ValidationResult.Invalid("Exam name cannot be empty")
        name.length > MAX_EXAM_NAME_LENGTH ->
            ValidationResult.Invalid("Exam name too long (max $MAX_EXAM_NAME_LENGTH characters)")
        name.contains(INVALID_NAME_CHARS) ->
            ValidationResult.Invalid("Exam name contains invalid characters")
        else -> ValidationResult.Valid
    }

    /**
     * Req 24.1: question count must be within the supported 1-200 range.
     */
    fun validateQuestionCount(count: Int): ValidationResult = when {
        count < MIN_QUESTION_COUNT ->
            ValidationResult.Invalid("Question count must be at least $MIN_QUESTION_COUNT")
        count > MAX_QUESTION_COUNT ->
            ValidationResult.Invalid("Question count cannot exceed $MAX_QUESTION_COUNT")
        else -> ValidationResult.Valid
    }

    /**
     * Format-only portion of Req 24.2 / Req 8.1: student ID must be non-empty,
     * at most 20 characters, and contain only letters, numbers, and hyphens.
     * Uniqueness is checked separately by [validateStudentId].
     */
    fun validateStudentIdFormat(id: String): ValidationResult = when {
        id.isBlank() ->
            ValidationResult.Invalid("Student ID cannot be empty")
        id.length > MAX_STUDENT_ID_LENGTH ->
            ValidationResult.Invalid("Student ID too long (max $MAX_STUDENT_ID_LENGTH characters)")
        !id.matches(STUDENT_ID_PATTERN) ->
            ValidationResult.Invalid("Student ID can only contain letters, numbers, and hyphens")
        else -> ValidationResult.Valid
    }

    /**
     * Req 7.2: section name is required and capped at 50 characters.
     */
    fun validateSectionName(name: String): ValidationResult = when {
        name.isBlank() ->
            ValidationResult.Invalid("Section name cannot be empty")
        name.length > MAX_SECTION_NAME_LENGTH ->
            ValidationResult.Invalid("Section name too long (max $MAX_SECTION_NAME_LENGTH characters)")
        else -> ValidationResult.Valid
    }

    /**
     * Req 8.5: profile notes are capped at 500 characters per note.
     * An empty note is permitted (the note simply carries no text).
     */
    fun validateNote(note: String): ValidationResult = when {
        note.length > MAX_NOTE_LENGTH ->
            ValidationResult.Invalid("Note too long (max $MAX_NOTE_LENGTH characters)")
        else -> ValidationResult.Valid
    }

    // endregion

    // region Checks requiring looked-up data --------------------------------------------

    /**
     * Req 24.2: student ID must pass format validation AND be unique within its
     * section. Callers supply the IDs already present in the target section
     * (typically from a repository/DAO query); this keeps the engine DB-free.
     *
     * @param id the candidate student ID
     * @param existingIdsInSection the set of student IDs already enrolled in the
     *        same section. The comparison is case-insensitive.
     */
    fun validateStudentId(
        id: String,
        existingIdsInSection: Collection<String> = emptyList(),
    ): ValidationResult {
        val format = validateStudentIdFormat(id)
        if (format is ValidationResult.Invalid) return format

        val duplicate = existingIdsInSection.any { it.equals(id, ignoreCase = true) }
        return if (duplicate) {
            ValidationResult.Invalid("Student ID '$id' already exists in this section")
        } else {
            ValidationResult.Valid
        }
    }

    /**
     * Req 24.3: a MELC mapping must reference a MELC that exists in the database and
     * must not duplicate an existing mapping. Both facts are supplied by the caller.
     *
     * @param melcId the MELC being mapped
     * @param melcExists whether the MELC id was found in the database
     * @param existingMelcMappings MELC ids already mapped to the same target
     *        (e.g. exam/question). A repeat of [melcId] is treated as a duplicate.
     */
    fun validateMelcMapping(
        melcId: Long,
        melcExists: Boolean,
        existingMelcMappings: Collection<Long> = emptyList(),
    ): ValidationResult = when {
        !melcExists ->
            ValidationResult.Invalid("Selected MELC does not exist")
        existingMelcMappings.contains(melcId) ->
            ValidationResult.Invalid("MELC mapping already exists")
        else -> ValidationResult.Valid
    }

    // endregion

    // region File / template upload validation ------------------------------------------

    /**
     * Req 24.4: uploaded template files must exist, be non-empty (not corrupted/empty),
     * be within the 10 MB size limit, and use a supported format.
     */
    fun validateFileUpload(
        file: File,
        maxSize: Long = MAX_FILE_SIZE_BYTES,
    ): ValidationResult = when {
        !file.exists() ->
            ValidationResult.Invalid("File does not exist")
        file.length() == 0L ->
            ValidationResult.Invalid("File is empty or corrupted")
        file.length() > maxSize ->
            ValidationResult.Invalid("File size exceeds ${maxSize / 1024 / 1024}MB limit")
        !isSupportedFileType(file.extension) ->
            ValidationResult.Invalid("Unsupported file format")
        else -> ValidationResult.Valid
    }

    private fun isSupportedFileType(extension: String): Boolean =
        extension.lowercase() in SUPPORTED_FILE_EXTENSIONS

    // endregion

    /**
     * Sanitizes an exam name for safe persistence, delegating to the app-wide
     * [InputSanitizer] policy. Callers should [validateExamName] first for the
     * user-facing error and use this to normalize the stored value.
     */
    fun sanitizeExamName(name: String): String = InputSanitizer.sanitizeExamName(name)

    companion object {
        const val MAX_EXAM_NAME_LENGTH = 200
        const val MIN_QUESTION_COUNT = 1
        const val MAX_QUESTION_COUNT = 200
        const val MAX_STUDENT_ID_LENGTH = 20
        const val MAX_SECTION_NAME_LENGTH = 50
        const val MAX_NOTE_LENGTH = 500
        const val MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024 // 10 MB

        val SUPPORTED_FILE_EXTENSIONS = listOf("pdf", "docx", "jpg", "jpeg", "png")

        private val INVALID_NAME_CHARS = Regex("[<>:\"/\\\\|?*]")
        private val STUDENT_ID_PATTERN = Regex("^[a-zA-Z0-9-]+$")
    }
}
