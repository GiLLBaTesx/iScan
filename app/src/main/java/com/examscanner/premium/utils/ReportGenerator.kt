package com.examscanner.premium.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.examscanner.premium.analytics.AnalyticsEngine
import com.examscanner.premium.analytics.AnalyticsTracker
import com.examscanner.premium.analytics.MasteryCalculator
import com.examscanner.premium.data.AnswerKeyEntity
import com.examscanner.premium.data.ExamEntity
import com.examscanner.premium.data.ExamRepository
import com.examscanner.premium.data.MelcEntity
import com.examscanner.premium.data.QuestionAnalysis
import com.examscanner.premium.data.SectionEntity
import com.examscanner.premium.data.StudentEntity
import com.examscanner.premium.data.StudentMelcMasteryEntity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * ReportGenerator - Produces professional PDF assessment reports.
 *
 * Three report types are supported (Requirement 5.1):
 *  - [ReportType.IndividualStudent]: one student's score, question breakdown, and
 *    MELC competency mastery for a single exam (Req 5.2).
 *  - [ReportType.ClassSummary]: class statistics (mean/median/highest/lowest/std-dev),
 *    item analysis, and student rankings for an exam (Req 5.3, 5.4).
 *  - [ReportType.SchoolLevel]: aggregated statistics across every section of a subject
 *    for a quarter/school-year, plus identified learning gaps (Req 5.5).
 *
 * Every report carries optional school name + logo branding (Req 5.6) and is rendered
 * with **incremental page rendering** - a fresh page is started whenever content
 * overflows the current page (Req 5.7, 19.5). The generated PDF is written to the
 * device Downloads folder and its path is returned for the Android share sheet.
 *
 * PDF generation uses Android's built-in [android.graphics.pdf.PdfDocument] + [Canvas],
 * matching the existing [TemplatePDFGenerator] / [AnswerSheetPrettyPrinter] conventions.
 * iText7 is intentionally NOT used because it is not a project dependency.
 *
 * Data is read exclusively through the existing [ExamRepository] plus the net-new
 * [MasteryCalculator] and [AnalyticsEngine]; no scoring or analytics logic is duplicated.
 *
 * Testability seam: [computeClassStatistics] is a **pure function** (no Android/PDF/DB
 * dependencies) so the statistics can be unit-tested off-device (Task 7.2).
 *
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 19.5.
 */
class ReportGenerator(
    private val context: Context,
    private val repository: ExamRepository,
    private val masteryCalculator: MasteryCalculator = MasteryCalculator(repository),
    private val analyticsEngine: AnalyticsEngine = AnalyticsEngine(repository)
) {

    /** Discriminated set of report requests. */
    sealed class ReportType {
        data class IndividualStudent(val studentId: Long, val examId: Long) : ReportType()
        data class ClassSummary(val examId: Long, val sectionId: Long = 0L) : ReportType()
        data class SchoolLevel(
            val subjectId: Long,
            val quarter: Int,
            val schoolYear: String
        ) : ReportType()
    }

    /** Class-level descriptive statistics over a cohort of percentage scores. */
    data class ClassStatistics(
        val count: Int,
        val mean: Float,
        val median: Float,
        val highest: Float,
        val lowest: Float,
        val standardDeviation: Float
    )

    /**
     * Generates the requested [type] of report, writes it to Downloads, and returns the
     * absolute file path of the saved PDF (for sharing).
     *
     * @param schoolName optional school name printed in the branded header.
     * @param logoPath optional absolute path to a logo image drawn in the header.
     */
    suspend fun generateReport(
        type: ReportType,
        schoolName: String = "",
        logoPath: String? = null
    ): String = withContext(Dispatchers.IO) {
        try {
            val result = when (type) {
                is ReportType.IndividualStudent -> generateIndividualReport(type, schoolName, logoPath)
                is ReportType.ClassSummary -> generateClassReport(type, schoolName, logoPath)
                is ReportType.SchoolLevel -> generateSchoolReport(type, schoolName, logoPath)
            }
            AnalyticsTracker.trackFeatureUsage(
                userId = currentUserId(),
                featureName = "report_generated",
                success = true,
                metadata = mapOf("report_type" to type::class.java.simpleName)
            )
            result
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Failed to generate report ${type::class.java.simpleName}", e)
            AnalyticsTracker.trackError(
                userId = currentUserId(),
                errorType = "report_error",
                errorMessage = e.message ?: "Report generation failed",
                stackTrace = e.stackTraceToString()
            )
            throw e
        }
    }

    // ---------------------------------------------------------------------------------------
    // Individual student report (Req 5.2)
    // ---------------------------------------------------------------------------------------

    private suspend fun generateIndividualReport(
        type: ReportType.IndividualStudent,
        schoolName: String,
        logoPath: String?
    ): String {
        val student = repository.getStudentById(type.studentId)
            ?: throw IllegalArgumentException("Student ${type.studentId} not found")
        val exam = repository.examDao.getExam(type.examId)
            ?: throw IllegalArgumentException("Exam ${type.examId} not found")
        val keys = repository.getAnswerKeys(type.examId).first().sortedBy { it.questionNumber }
        val answers = repository.getStudentAnswers(type.studentId)
        val score = repository.calculateScore(student, keys)
        val totalPoints = keys.sumOf { it.points }
        val percentage = if (totalPoints > 0) (score.toFloat() / totalPoints * 100f).roundToInt() else 0
        val mastery = masteryCalculator.getMasterySummary(type.studentId)
        val masteryRows = repository.getStudentMastery(type.studentId).first()
        val melcById = repository.getAllMelcs().first().associateBy { it.id }

        val writer = PageWriter(logoPath)
        writer.startPage()
        writer.drawBranding(schoolName, "Individual Student Report", logoPath)

        writer.section("Student Information")
        writer.keyValue("Name", student.name.ifBlank { "-" })
        writer.keyValue("Student ID", student.studentId.ifBlank { "-" })
        writer.keyValue("Exam", exam.name)
        writer.keyValue("Date Taken", formatDate(student.scannedAt))

        writer.section("Score Summary")
        writer.scoreBox(score, totalPoints, percentage, exam.passingGrade)

        writer.section("Detailed Breakdown")
        writer.tableHeader(listOf("Q#", "Answer", "Key", "Result"), INDIVIDUAL_COLS)
        keys.forEach { key ->
            val given = answers.find { it.questionNumber == key.questionNumber }?.answer ?: "-"
            val correct = isCorrect(given, key)
            writer.tableRow(
                listOf(
                    key.questionNumber.toString(),
                    given.ifBlank { "-" },
                    key.correctAnswer,
                    if (correct) "Correct" else "Wrong"
                ),
                INDIVIDUAL_COLS
            )
        }

        writer.section("Competency Mastery Levels")
        if (masteryRows.isEmpty()) {
            writer.body("No MELC-mapped competencies recorded for this student.")
        } else {
            writer.tableHeader(listOf("Competency", "Mastery", "%"), MASTERY_COLS)
            masteryRows.sortedByDescending { it.percentage }.forEach { m ->
                writer.tableRow(
                    listOf(
                        melcLabel(m, melcById),
                        m.masteryLevel,
                        "${m.percentage.roundToInt()}%"
                    ),
                    MASTERY_COLS
                )
            }
            writer.spacer()
            writer.body(
                "Assessed: ${mastery.totalMelcsAssessed}  •  Advanced: ${mastery.advanced}  •  " +
                    "Proficient: ${mastery.proficient}  •  Approaching: ${mastery.approaching}  •  " +
                    "Developing: ${mastery.developing}"
            )
        }

        writer.section("Visual Analysis")
        writer.simpleBarChart(
            labels = listOf("Correct", "Wrong"),
            values = listOf(
                keys.count { isCorrect(answers.find { a -> a.questionNumber == it.questionNumber }?.answer ?: "", it) }.toFloat(),
                keys.count { !isCorrect(answers.find { a -> a.questionNumber == it.questionNumber }?.answer ?: "", it) }.toFloat()
            )
        )

        writer.footer()
        val fileName = "individual_report_${sanitize(student.studentId.ifBlank { student.id.toString() })}_${sanitize(exam.name)}.pdf"
        return finish(writer, fileName)
    }

    // ---------------------------------------------------------------------------------------
    // Class summary report (Req 5.3, 5.4)
    // ---------------------------------------------------------------------------------------

    private suspend fun generateClassReport(
        type: ReportType.ClassSummary,
        schoolName: String,
        logoPath: String?
    ): String {
        val exam = repository.examDao.getExam(type.examId)
            ?: throw IllegalArgumentException("Exam ${type.examId} not found")
        val allStudents = repository.getStudents(type.examId).first()
        val students = if (type.sectionId > 0L) {
            allStudents.filter { it.sectionId == type.sectionId }
        } else {
            allStudents
        }
        val keys = repository.getAnswerKeys(type.examId).first().sortedBy { it.questionNumber }
        val totalPoints = keys.sumOf { it.points }

        val scored = students.map { student ->
            val raw = repository.calculateScore(student, keys)
            val pct = if (totalPoints > 0) raw.toFloat() / totalPoints * 100f else 0f
            Triple(student, raw, pct)
        }
        val percentages = scored.map { it.third }
        val stats = computeClassStatistics(percentages)

        val writer = PageWriter(logoPath)
        writer.startPage()
        writer.drawBranding(schoolName, "Class Summary Report", logoPath)

        writer.section("Exam Information")
        writer.keyValue("Exam", exam.name)
        writer.keyValue("Total Students", students.size.toString())
        writer.keyValue("Total Points", totalPoints.toString())
        writer.keyValue("Date", formatDate(exam.createdAt))

        writer.section("Class Statistics")
        writer.tableHeader(listOf("Metric", "Value"), STAT_COLS)
        writer.tableRow(listOf("Mean", "${stats.mean.roundToInt()}%"), STAT_COLS)
        writer.tableRow(listOf("Median", "${stats.median.roundToInt()}%"), STAT_COLS)
        writer.tableRow(listOf("Highest", "${stats.highest.roundToInt()}%"), STAT_COLS)
        writer.tableRow(listOf("Lowest", "${stats.lowest.roundToInt()}%"), STAT_COLS)
        writer.tableRow(listOf("Std. Deviation", format1(stats.standardDeviation)), STAT_COLS)

        writer.section("Score Distribution")
        writer.simpleBarChart(
            labels = listOf("<60", "60-74", "75-79", "80-89", "90+"),
            values = distributionBuckets(percentages)
        )

        writer.section("Item Analysis")
        if (keys.isEmpty()) {
            writer.body("No answer key available for item analysis.")
        } else {
            val analyses = if (type.sectionId > 0L) {
                repository.getSectionItemAnalysis(type.examId, type.sectionId)
            } else {
                computeItemAnalysis(students, keys)
            }
            writer.tableHeader(listOf("Q#", "Correct", "Total", "% Correct"), ITEM_COLS)
            keys.forEach { key ->
                val a = analyses[key.questionNumber]
                writer.tableRow(
                    listOf(
                        key.questionNumber.toString(),
                        (a?.correctCount ?: 0).toString(),
                        (a?.totalStudents ?: students.size).toString(),
                        "${a?.percentageCorrect ?: 0}%"
                    ),
                    ITEM_COLS
                )
            }
        }

        writer.section("Student Rankings")
        writer.tableHeader(listOf("Rank", "Student", "Score", "%"), RANK_COLS)
        scored.sortedByDescending { it.third }.forEachIndexed { index, (student, raw, pct) ->
            writer.tableRow(
                listOf(
                    (index + 1).toString(),
                    student.name.ifBlank { student.studentId },
                    "$raw/$totalPoints",
                    "${pct.roundToInt()}%"
                ),
                RANK_COLS
            )
        }

        writer.footer()
        val fileName = "class_report_${sanitize(exam.name)}.pdf"
        return finish(writer, fileName)
    }

    // ---------------------------------------------------------------------------------------
    // School-level report (Req 5.5)
    // ---------------------------------------------------------------------------------------

    private suspend fun generateSchoolReport(
        type: ReportType.SchoolLevel,
        schoolName: String,
        logoPath: String?
    ): String {
        val subject = repository.examDao.getSubjectFolder(type.subjectId)
        val sections: List<SectionEntity> = repository.getSections(type.subjectId).first()

        // Aggregate per-section performance across every exam in the subject folder.
        val exams = repository.getExamsByFolder(type.subjectId).first()
        val sectionSummaries = sections.map { section ->
            aggregateSection(section, exams)
        }
        val allPercentages = sectionSummaries.flatMap { it.percentages }
        val overallStats = computeClassStatistics(allPercentages)
        val totalStudents = sectionSummaries.sumOf { it.studentCount }

        val writer = PageWriter(logoPath)
        writer.startPage()
        writer.drawBranding(schoolName, "School-Level Analytics Report", logoPath)

        writer.section("Overview")
        writer.keyValue("Subject", subject?.name ?: "Subject #${type.subjectId}")
        writer.keyValue("School Year", type.schoolYear.ifBlank { "-" })
        writer.keyValue("Quarter", type.quarter.toString())
        writer.keyValue("Total Sections", sections.size.toString())
        writer.keyValue("Total Students", totalStudents.toString())

        writer.section("Overall Performance")
        writer.tableHeader(listOf("Metric", "Value"), STAT_COLS)
        writer.tableRow(listOf("Mean", "${overallStats.mean.roundToInt()}%"), STAT_COLS)
        writer.tableRow(listOf("Median", "${overallStats.median.roundToInt()}%"), STAT_COLS)
        writer.tableRow(listOf("Highest", "${overallStats.highest.roundToInt()}%"), STAT_COLS)
        writer.tableRow(listOf("Lowest", "${overallStats.lowest.roundToInt()}%"), STAT_COLS)
        writer.tableRow(listOf("Std. Deviation", format1(overallStats.standardDeviation)), STAT_COLS)

        writer.section("Section Comparisons")
        if (sectionSummaries.isEmpty()) {
            writer.body("No sections found for this subject.")
        } else {
            writer.tableHeader(listOf("Section", "Students", "Mean %"), SECTION_COLS)
            sectionSummaries.forEach { s ->
                writer.tableRow(
                    listOf(
                        s.name,
                        s.studentCount.toString(),
                        "${s.stats.mean.roundToInt()}%"
                    ),
                    SECTION_COLS
                )
            }
            writer.spacer()
            writer.simpleBarChart(
                labels = sectionSummaries.map { it.name.take(8) },
                values = sectionSummaries.map { it.stats.mean }
            )
        }

        writer.section("Learning Gaps Identified")
        val gaps = sections.flatMap { section ->
            analyticsEngine.identifyLearningGaps(section.id)
        }.sortedBy { it.averageMastery }.take(MAX_GAP_ROWS)
        if (gaps.isEmpty()) {
            writer.body("No learning gaps below 75% mastery were identified.")
        } else {
            writer.tableHeader(listOf("Competency", "Avg %", "Severity"), GAP_COLS)
            gaps.forEach { gap ->
                writer.tableRow(
                    listOf(
                        "${gap.melc.code} ${gap.melc.description}".take(60),
                        "${gap.averageMastery.roundToInt()}%",
                        gap.severity.name
                    ),
                    GAP_COLS
                )
            }
        }

        writer.footer()
        val fileName = "school_report_${sanitize(type.schoolYear.ifBlank { "SY" })}_Q${type.quarter}.pdf"
        return finish(writer, fileName)
    }

    /** Per-section aggregation used by the school-level report. */
    private data class SectionSummary(
        val name: String,
        val studentCount: Int,
        val percentages: List<Float>,
        val stats: ClassStatistics
    )

    private suspend fun aggregateSection(
        section: SectionEntity,
        exams: List<ExamEntity>
    ): SectionSummary {
        val students = repository.getStudentsBySection(section.id)
        val percentages = mutableListOf<Float>()
        exams.forEach { exam ->
            val keys = repository.getAnswerKeys(exam.id).first()
            val totalPoints = keys.sumOf { it.points }
            if (totalPoints <= 0) return@forEach
            students.filter { it.examId == exam.id || it.sectionId == section.id }
                .distinctBy { it.id }
                .forEach { student ->
                    val raw = repository.calculateScore(student, keys)
                    percentages.add(raw.toFloat() / totalPoints * 100f)
                }
        }
        return SectionSummary(
            name = section.name,
            studentCount = students.size,
            percentages = percentages,
            stats = computeClassStatistics(percentages)
        )
    }

    // ---------------------------------------------------------------------------------------
    // Pure helpers (device-free, unit-testable)
    // ---------------------------------------------------------------------------------------

    /**
     * Computes descriptive statistics over a cohort of percentage scores.
     *
     * Pure function - no Android, PDF, or repository dependencies - so it can be exercised
     * directly by unit tests (Task 7.2). Handles the empty-cohort edge case by returning a
     * zeroed [ClassStatistics] with `count = 0`.
     *
     * - mean: arithmetic average.
     * - median: middle value of the sorted list (average of the two middle values when the
     *   cohort size is even).
     * - highest / lowest: max / min.
     * - standardDeviation: population standard deviation (divides by N).
     */
    fun computeClassStatistics(percentages: List<Float>): ClassStatistics {
        if (percentages.isEmpty()) {
            return ClassStatistics(0, 0f, 0f, 0f, 0f, 0f)
        }
        val n = percentages.size
        val mean = percentages.average().toFloat()
        val sorted = percentages.sorted()
        val median = if (n % 2 == 1) {
            sorted[n / 2]
        } else {
            (sorted[n / 2 - 1] + sorted[n / 2]) / 2f
        }
        val variance = percentages.map { val d = it - mean; d * d }.average()
        return ClassStatistics(
            count = n,
            mean = mean,
            median = median,
            highest = sorted.last(),
            lowest = sorted.first(),
            standardDeviation = sqrt(variance).toFloat()
        )
    }

    /** Buckets percentages into 5 DepEd-aligned bands for the distribution chart. */
    private fun distributionBuckets(percentages: List<Float>): List<Float> {
        val buckets = FloatArray(5)
        percentages.forEach { p ->
            val idx = when {
                p < 60f -> 0
                p < 75f -> 1
                p < 80f -> 2
                p < 90f -> 3
                else -> 4
            }
            buckets[idx] += 1f
        }
        return buckets.toList()
    }

    /**
     * Computes per-question item analysis over an arbitrary cohort, mirroring the shape of
     * [ExamRepository.getSectionItemAnalysis] but for a whole-exam (no-section) cohort.
     */
    private suspend fun computeItemAnalysis(
        students: List<StudentEntity>,
        keys: List<AnswerKeyEntity>
    ): Map<Int, QuestionAnalysis> {
        val answersByStudent = students.associateWith { repository.getStudentAnswers(it.id) }
        return keys.associate { key ->
            val distribution = mutableMapOf<String, Int>()
            var correct = 0
            students.forEach { student ->
                val given = answersByStudent[student]
                    ?.find { it.questionNumber == key.questionNumber }?.answer
                    ?.takeIf { it.isNotBlank() } ?: "-"
                distribution[given] = (distribution[given] ?: 0) + 1
                if (isCorrect(given, key)) correct++
            }
            val total = students.size
            key.questionNumber to QuestionAnalysis(
                questionNumber = key.questionNumber,
                correctAnswer = key.correctAnswer,
                correctCount = correct,
                totalStudents = total,
                percentageCorrect = if (total > 0) (correct * 100 / total) else 0,
                answerDistribution = distribution
            )
        }
    }

    private fun isCorrect(given: String, key: AnswerKeyEntity): Boolean {
        if (given.isBlank()) return false
        if (given == key.correctAnswer) return true
        return key.alternativeAnswers.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .contains(given)
    }

    private fun melcLabel(
        mastery: StudentMelcMasteryEntity,
        melcById: Map<Long, MelcEntity>
    ): String {
        val melc = melcById[mastery.melcId]
        return if (melc != null) "${melc.code} ${melc.description}".take(60) else "MELC #${mastery.melcId}"
    }

    private fun currentUserId(): String? = FirebaseAuth.getInstance().currentUser?.uid

    private fun formatDate(millis: Long): String =
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))

    private fun format1(value: Float): String = String.format(Locale.US, "%.1f", value)

    private fun sanitize(name: String): String = InputSanitizer.sanitizeFileName(name)

    // ---------------------------------------------------------------------------------------
    // Output: save to Downloads and return path
    // ---------------------------------------------------------------------------------------

    /**
     * Writes the finished [PdfDocument] to the device Downloads folder and returns the
     * saved file path. Uses [MediaStore] on Android 10+ (scoped storage) and the public
     * Downloads directory on older versions.
     */
    private fun finish(writer: PageWriter, fileName: String): String {
        val document = writer.finishDocument()
        try {
            return saveToDownloads(document, fileName)
        } finally {
            document.close()
        }
    }

    private fun saveToDownloads(document: PdfDocument, fileName: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Unable to create Downloads entry for $fileName")
            resolver.openOutputStream(uri)?.use { out ->
                document.writeTo(out)
                out.flush()
            } ?: throw IllegalStateException("Unable to open output stream for $fileName")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            SecureLogger.d(TAG, "Report saved to Downloads: $fileName")
            // Return the MediaStore uri string; it is a valid, shareable reference.
            return uri.toString()
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val outFile = File(downloadsDir, fileName)
            FileOutputStream(outFile).use { out ->
                document.writeTo(out)
                out.flush()
            }
            SecureLogger.d(TAG, "Report saved to Downloads: ${outFile.absolutePath}")
            return outFile.absolutePath
        }
    }

    // ---------------------------------------------------------------------------------------
    // PageWriter: incremental page rendering over PdfDocument/Canvas
    // ---------------------------------------------------------------------------------------

    /**
     * Cursor-based writer that lays content out top-to-bottom and automatically starts a
     * new page when the next element would overflow the bottom margin (Req 5.7, 19.5).
     */
    private inner class PageWriter(logoPath: String?) {
        private val document = PdfDocument()
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var y = 0f
        private var pageNumber = 0

        private val logoBitmap: Bitmap? = logoPath?.let { loadLogo(it) }

        private val titlePaint = Paint().apply {
            color = Color.parseColor(COLOR_ELECTRIC_BLUE)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        private val subtitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
            isAntiAlias = true
        }
        private val sectionPaint = Paint().apply {
            color = Color.parseColor(COLOR_ICY_CYAN)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        private val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            isAntiAlias = true
        }
        private val keyPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        private val headerCellPaint = Paint().apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        private val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }
        private val headerBgPaint = Paint().apply {
            color = Color.parseColor(COLOR_ELECTRIC_BLUE)
            style = Paint.Style.FILL
        }
        private val barPaint = Paint().apply {
            color = Color.parseColor(COLOR_LUMINOUS_AZURE)
            style = Paint.Style.FILL
        }
        private val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 8f
            isAntiAlias = true
        }

        fun startPage() {
            page?.let { document.finishPage(it) }
            pageNumber++
            val info = PdfDocument.PageInfo
                .Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber)
                .create()
            val p = document.startPage(info)
            page = p
            canvas = p.canvas
            y = MARGIN
        }

        /** Ensures [needed] points fit below the cursor; otherwise breaks to a new page. */
        private fun ensureSpace(needed: Float) {
            if (y + needed > PAGE_HEIGHT - MARGIN) {
                startPage()
            }
        }

        fun drawBranding(schoolName: String, reportTitle: String, logoPath: String?) {
            val c = canvas ?: return
            var textX = MARGIN
            if (logoBitmap != null) {
                val dest = Rect(
                    MARGIN.toInt(),
                    y.toInt(),
                    (MARGIN + LOGO_SIZE).toInt(),
                    (y + LOGO_SIZE).toInt()
                )
                c.drawBitmap(logoBitmap, null, dest, null)
                textX = MARGIN + LOGO_SIZE + 12f
            }
            if (schoolName.isNotBlank()) {
                c.drawText(schoolName, textX, y + 16f, titlePaint)
                c.drawText(reportTitle, textX, y + 34f, subtitlePaint)
            } else {
                c.drawText(reportTitle, textX, y + 16f, titlePaint)
            }
            y += maxOf(LOGO_SIZE, 40f) + 8f
            c.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += 16f
        }

        fun section(title: String) {
            ensureSpace(28f)
            val c = canvas ?: return
            y += 6f
            c.drawText(title, MARGIN, y, sectionPaint)
            y += 6f
            c.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += 14f
        }

        fun keyValue(key: String, value: String) {
            ensureSpace(ROW_HEIGHT)
            val c = canvas ?: return
            c.drawText("$key:", MARGIN, y, keyPaint)
            c.drawText(value, MARGIN + 130f, y, bodyPaint)
            y += ROW_HEIGHT
        }

        fun body(text: String) {
            ensureSpace(ROW_HEIGHT)
            val c = canvas ?: return
            c.drawText(text, MARGIN, y, bodyPaint)
            y += ROW_HEIGHT
        }

        fun spacer() {
            y += ROW_HEIGHT / 2f
        }

        fun scoreBox(score: Int, total: Int, percentage: Int, passingGrade: Int) {
            ensureSpace(ROW_HEIGHT * 2)
            val c = canvas ?: return
            val passed = percentage >= passingGrade
            c.drawText("Score: $score / $total", MARGIN, y, keyPaint)
            y += ROW_HEIGHT
            val resultPaint = Paint(keyPaint).apply {
                color = Color.parseColor(if (passed) COLOR_SUCCESS else COLOR_ERROR)
            }
            c.drawText("$percentage%  (${if (passed) "PASSED" else "FAILED"})", MARGIN, y, resultPaint)
            y += ROW_HEIGHT
        }

        fun tableHeader(cells: List<String>, widths: List<Float>) {
            ensureSpace(ROW_HEIGHT + 4f)
            val c = canvas ?: return
            c.drawRect(MARGIN, y - 11f, PAGE_WIDTH - MARGIN, y + 5f, headerBgPaint)
            var x = MARGIN + 4f
            cells.forEachIndexed { i, cell ->
                c.drawText(cell, x, y, headerCellPaint)
                x += widths.getOrElse(i) { 60f }
            }
            y += ROW_HEIGHT
        }

        fun tableRow(cells: List<String>, widths: List<Float>) {
            ensureSpace(ROW_HEIGHT)
            val c = canvas ?: return
            var x = MARGIN + 4f
            cells.forEachIndexed { i, cell ->
                val w = widths.getOrElse(i) { 60f }
                c.drawText(truncateToWidth(cell, w - 6f), x, y, bodyPaint)
                x += w
            }
            y += ROW_HEIGHT
            c.drawLine(MARGIN, y - ROW_HEIGHT + 4f, PAGE_WIDTH - MARGIN, y - ROW_HEIGHT + 4f, linePaint)
        }

        fun simpleBarChart(labels: List<String>, values: List<Float>) {
            if (labels.isEmpty()) return
            val chartHeight = 90f
            ensureSpace(chartHeight + ROW_HEIGHT)
            val c = canvas ?: return
            val maxValue = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
            val usableWidth = PAGE_WIDTH - 2 * MARGIN
            val slot = usableWidth / labels.size
            val barWidth = slot * 0.6f
            val baseY = y + chartHeight
            values.forEachIndexed { i, value ->
                val barH = (value / maxValue) * chartHeight
                val left = MARGIN + i * slot + (slot - barWidth) / 2f
                c.drawRect(left, baseY - barH, left + barWidth, baseY, barPaint)
                val valuePaint = Paint(bodyPaint).apply { textAlign = Paint.Align.CENTER }
                c.drawText(value.roundToInt().toString(), left + barWidth / 2f, baseY - barH - 3f, valuePaint)
                c.drawText(labels[i], left + barWidth / 2f, baseY + 12f, valuePaint)
            }
            y = baseY + ROW_HEIGHT
        }

        fun footer() {
            val c = canvas ?: return
            val text = "Generated ${formatDate(System.currentTimeMillis())}  •  Page $pageNumber"
            c.drawText(text, MARGIN, PAGE_HEIGHT - MARGIN / 2f, footerPaint)
        }

        private fun truncateToWidth(text: String, maxWidth: Float): String {
            if (bodyPaint.measureText(text) <= maxWidth) return text
            var end = text.length
            while (end > 1 && bodyPaint.measureText(text.substring(0, end) + "…") > maxWidth) {
                end--
            }
            return text.substring(0, end) + "…"
        }

        private fun loadLogo(path: String): Bitmap? {
            return try {
                val file = File(path)
                if (file.exists()) BitmapFactory.decodeFile(path) else null
            } catch (e: Exception) {
                SecureLogger.w(TAG, "Failed to load report logo: $path", e)
                null
            }
        }

        /** Finalizes the last open page and returns the document (caller closes/writes it). */
        fun finishDocument(): PdfDocument {
            page?.let { document.finishPage(it) }
            page = null
            canvas = null
            return document
        }
    }

    companion object {
        private const val TAG = "ReportGenerator"

        // A4 at 72 DPI (points).
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        private const val MARGIN = 36f
        private const val ROW_HEIGHT = 18f
        private const val LOGO_SIZE = 48f

        private const val MAX_GAP_ROWS = 15

        // Azure Glass theme colors (workspace convention).
        private const val COLOR_ELECTRIC_BLUE = "#FF03045E"
        private const val COLOR_ICY_CYAN = "#FF00B4D8"
        private const val COLOR_LUMINOUS_AZURE = "#FF38BDF8"
        private const val COLOR_SUCCESS = "#FF00B4D8"
        private const val COLOR_ERROR = "#FFEF5350"

        // Column layouts (points). Sum should stay within the usable width (~523pt).
        private val INDIVIDUAL_COLS = listOf(50f, 120f, 120f, 120f)
        private val MASTERY_COLS = listOf(330f, 110f, 60f)
        private val STAT_COLS = listOf(300f, 200f)
        private val ITEM_COLS = listOf(60f, 130f, 130f, 130f)
        private val RANK_COLS = listOf(60f, 250f, 100f, 80f)
        private val SECTION_COLS = listOf(300f, 100f, 100f)
        private val GAP_COLS = listOf(330f, 90f, 100f)
    }
}
