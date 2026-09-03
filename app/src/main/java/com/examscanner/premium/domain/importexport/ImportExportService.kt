package com.examscanner.premium.domain.importexport

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.examscanner.premium.data.AnswerKeyEntity
import com.examscanner.premium.data.ExamRepository
import com.examscanner.premium.data.StudentEntity
import com.examscanner.premium.domain.validation.ValidationEngine
import com.examscanner.premium.utils.SecureLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ImportExportService - CSV/Excel import & export for exam results, class rosters,
 * and MELC mastery (Requirement 11), reusing the app's existing CSV parsing/writing
 * helpers where they exist and delegating XLSX to Apache POI.
 *
 * Design goals (design.md §13):
 * - Exports go to the device Downloads folder with descriptive, timestamped names
 *   (Req 11.7), e.g. `exam_results_2024_01_15_143000.csv`.
 * - The same column structures apply to both CSV and XLSX (Req 11.6). Column shapes
 *   are fixed by Requirement 11.1-11.3:
 *     - Exam results: student_id, name, exam_name, score, percentage, date (Req 11.1)
 *     - Class roster:  student_id, name, grade_level, section (Req 11.2)
 *     - MELC mastery:  student_id, name, melc_code, mastery_level, percentage (Req 11.3)
 * - Roster import accepts headers student_id, name, grade_level (Req 11.4) and validates
 *   every row through [ValidationEngine] (Req 11.5, Req 24.5), reporting per-row errors
 *   without aborting the whole import.
 *
 * Read/write are kept symmetric (same column meanings, same header names) so a
 * write-then-read roster round trip preserves the data (supports Property 9 / Task 7.6).
 *
 * This class reads all of its data through the existing [ExamRepository] and validates
 * with the existing [ValidationEngine]; it does not reach into DAOs or duplicate CSV logic.
 *
 * Validates: Requirements 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7, 24.5
 */
class ImportExportService(
    private val context: Context,
    private val repository: ExamRepository,
    private val validation: ValidationEngine = ValidationEngine()
) {

    /** The three supported export payloads (Req 11.1-11.3). */
    sealed class ExportType {
        data class ExamResults(val examId: Long) : ExportType()      // Req 11.1
        data class ClassRoster(val sectionId: Long) : ExportType()   // Req 11.2
        data class MelcMastery(val sectionId: Long) : ExportType()   // Req 11.3
    }

    /** Supported file formats for both import and export (Req 11.6). */
    enum class FileFormat { CSV, XLSX }

    /** Result of a roster import: successes plus a per-row error list (Req 11.5). */
    data class ImportResult(
        val successCount: Int,
        val failedRows: List<RowError>
    )

    /** A single rejected import row and the reason it was rejected (Req 11.5). */
    data class RowError(val rowNumber: Int, val message: String)

    // region Export -----------------------------------------------------------------

    /**
     * Exports [type] as [format] to the device Downloads folder with a descriptive,
     * timestamped name (Req 11.7) and returns a human-readable location of the file.
     *
     * CSV is written via a shared CSV writer; XLSX via Apache POI (Req 11.6). Both use
     * the same header + column structure so the two formats are interchangeable.
     */
    suspend fun export(type: ExportType, format: FileFormat): String =
        withContext(Dispatchers.IO) {
            val table = when (type) {
                is ExportType.ExamResults -> buildExamResultTable(type.examId)
                is ExportType.ClassRoster -> buildRosterTable(type.sectionId)
                is ExportType.MelcMastery -> buildMasteryTable(type.sectionId)
            }
            val fileName = buildFileName(type, format)
            val location = writeToDownloads(fileName, table.header, table.rows, format)
            SecureLogger.d(TAG, "Exported ${table.rows.size} rows to $location")
            location
        }

    /** A simple in-memory table: one header row plus data rows, all as strings. */
    private data class Table(val header: List<String>, val rows: List<List<String>>)

    private suspend fun buildExamResultTable(examId: Long): Table {
        val exam = repository.examDao.getExam(examId)
            ?: throw IllegalArgumentException("Exam $examId not found")
        val answerKeys: List<AnswerKeyEntity> = repository.getAnswerKeysList(examId)
        val students: List<StudentEntity> = repository.getStudentsList(examId)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Req 11.1: student_id, name, exam_name, score, percentage, date
        val rows = students.map { student ->
            val percentage = repository.calculateScore(student, answerKeys)
            val correct = countCorrect(percentage, answerKeys)
            listOf(
                student.studentId,
                student.name,
                exam.name,
                "$correct/${answerKeys.size}",
                "$percentage%",
                dateFormat.format(Date(student.scannedAt))
            )
        }
        return Table(
            header = listOf("student_id", "name", "exam_name", "score", "percentage", "date"),
            rows = rows
        )
    }

    private suspend fun buildRosterTable(sectionId: Long): Table {
        val section = repository.examDao.getSectionById(sectionId)
        val sectionName = section?.name ?: ""
        val students = repository.getStudentsBySection(sectionId)

        // Req 11.2: student_id, name, grade_level, section
        val rows = students.map { student ->
            listOf(student.studentId, student.name, student.gradeLevel, sectionName)
        }
        return Table(
            header = listOf("student_id", "name", "grade_level", "section"),
            rows = rows
        )
    }

    private suspend fun buildMasteryTable(sectionId: Long): Table {
        val students = repository.getStudentsBySection(sectionId)
        // MELC id -> code lookup so we can emit melc_code rather than the internal id.
        val melcCodeById: Map<Long, String> = repository.examDao.getAllMelcsSync()
            .associate { it.id to it.code }

        // Req 11.3: student_id, name, melc_code, mastery_level, percentage
        val rows = mutableListOf<List<String>>()
        students.forEach { student ->
            val mastery = repository.getStudentMastery(student.id).first()
            mastery.forEach { record ->
                rows.add(
                    listOf(
                        student.studentId,
                        student.name,
                        melcCodeById[record.melcId] ?: record.melcId.toString(),
                        record.masteryLevel,
                        formatPercentage(record.percentage)
                    )
                )
            }
        }
        return Table(
            header = listOf("student_id", "name", "melc_code", "mastery_level", "percentage"),
            rows = rows
        )
    }

    // endregion

    // region Import -----------------------------------------------------------------

    /**
     * Imports a student roster into [sectionId] from [uri] in [format] (Req 11.4).
     *
     * Requires the headers student_id, name, grade_level (Req 11.4). Each data row is
     * validated via [ValidationEngine] (student ID format + uniqueness within the section,
     * non-empty name) before it is persisted (Req 11.5, Req 24.5). Invalid rows are
     * collected into [ImportResult.failedRows] without aborting the whole import; valid
     * rows are inserted and counted.
     */
    suspend fun importRoster(sectionId: Long, uri: Uri, format: FileFormat): ImportResult =
        withContext(Dispatchers.IO) {
            val parsed = readRows(uri, format)
            requireHeaders(parsed.header, listOf("student_id", "name", "grade_level"))

            val col = parsed.header.withIndex().associate { it.value.trim().lowercase() to it.index }
            val idIdx = col.getValue("student_id")
            val nameIdx = col.getValue("name")
            val gradeIdx = col.getValue("grade_level")

            val errors = mutableListOf<RowError>()
            var success = 0
            // Track IDs accepted so far (plus those already in the section) so duplicates
            // within the same import file are caught too (Req 24.2).
            val existingIds = repository.getStudentsBySection(sectionId)
                .map { it.studentId }
                .toMutableList()

            parsed.rows.forEachIndexed { index, row ->
                val rowNumber = index + 1 // 1-based data row number (header excluded)
                val id = row.getOrNull(idIdx)?.trim().orEmpty()
                val name = row.getOrNull(nameIdx)?.trim().orEmpty()
                val gradeLevel = row.getOrNull(gradeIdx)?.trim().orEmpty()

                val idResult = validation.validateStudentId(id, existingIds)
                when {
                    idResult is ValidationEngine.ValidationResult.Invalid ->
                        errors.add(RowError(rowNumber, idResult.message))
                    name.isBlank() ->
                        errors.add(RowError(rowNumber, "Name is required"))
                    else -> {
                        repository.addStudentToSection(
                            studentId = id,
                            name = name,
                            gradeLevel = gradeLevel,
                            contactInfo = "",
                            sectionId = sectionId
                        )
                        existingIds.add(id)
                        success++
                    }
                }
            }
            SecureLogger.d(TAG, "Roster import: $success ok, ${errors.size} failed")
            ImportResult(success, errors)
        }

    // endregion

    // region CSV / XLSX I/O ---------------------------------------------------------

    /** Header + data rows read from an import file, regardless of source format. */
    private data class ParsedFile(val header: List<String>, val rows: List<List<String>>)

    /**
     * Reads the import file at [uri] in [format]. The byte-level parsing (CSV escaping /
     * XLSX via POI) lives in [RosterCodec]; this method only opens the Uri stream.
     */
    private fun readRows(uri: Uri, format: FileFormat): ParsedFile {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open import file")
        val parsed = input.use { stream ->
            when (format) {
                FileFormat.CSV -> RosterCodec.decodeCsv(stream)
                FileFormat.XLSX -> RosterCodec.decodeXlsx(stream)
            }
        }
        return ParsedFile(parsed.header, parsed.rows)
    }

    /**
     * Writes [header] + [rows] into the Downloads folder as [fileName] in [format].
     * Uses MediaStore on Android 10+ (scoped storage) and a direct file write on older
     * versions. Returns a human-readable file location.
     */
    private fun writeToDownloads(
        fileName: String,
        header: List<String>,
        rows: List<List<String>>,
        format: FileFormat
    ): String {
        val bytes = when (format) {
            FileFormat.CSV -> RosterCodec.encodeCsv(header, rows)
            FileFormat.XLSX -> RosterCodec.encodeXlsx(header, rows)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType(format))
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Could not create Downloads entry")
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: throw IllegalStateException("Could not write to Downloads")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return "Downloads/$fileName"
        } else {
            @Suppress("DEPRECATION")
            val downloads =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloads.exists()) downloads.mkdirs()
            val outFile = File(downloads, fileName)
            FileOutputStream(outFile).use { it.write(bytes) }
            return outFile.absolutePath
        }
    }

    // CSV/XLSX byte encoding lives in [RosterCodec] (JVM-testable, Android-free).

    // endregion

    // region Helpers ----------------------------------------------------------------

    private fun requireHeaders(header: List<String>, required: List<String>) {
        val present = header.map { it.trim().lowercase() }.toSet()
        val missing = required.filter { it !in present }
        require(missing.isEmpty()) {
            "Missing required column(s): ${missing.joinToString(", ")}"
        }
    }

    private fun buildFileName(type: ExportType, format: FileFormat): String {
        val timestamp = SimpleDateFormat("yyyy_MM_dd_HHmmss", Locale.getDefault()).format(Date())
        val prefix = when (type) {
            is ExportType.ExamResults -> "exam_results"
            is ExportType.ClassRoster -> "class_roster"
            is ExportType.MelcMastery -> "melc_mastery"
        }
        val extension = if (format == FileFormat.XLSX) "xlsx" else "csv"
        return "${prefix}_$timestamp.$extension"
    }

    private fun mimeType(format: FileFormat): String = when (format) {
        FileFormat.CSV -> "text/csv"
        FileFormat.XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    }

    /** Recovers the raw correct-count from a rounded percentage for the "score" column. */
    private fun countCorrect(percentage: Int, answerKeys: List<AnswerKeyEntity>): Int {
        val total = answerKeys.size
        if (total == 0) return 0
        return Math.round(percentage / 100.0 * total).toInt()
    }

    private fun formatPercentage(value: Float): String {
        val rounded = Math.round(value)
        return if (Math.abs(value - rounded) < 0.001f) "$rounded%"
        else "${"%.1f".format(value)}%"
    }

    // CSV escaping/parsing lives in [RosterCodec].

    // endregion

    companion object {
        private const val TAG = "ImportExportService"
    }
}
