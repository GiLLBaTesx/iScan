package com.examscanner.premium.analytics

import com.examscanner.premium.data.AnswerKeyEntity
import com.examscanner.premium.data.ExamDao
import com.examscanner.premium.data.ExamRepository
import com.examscanner.premium.data.GradingScaleEntity
import com.examscanner.premium.data.MelcCoverageEntity
import com.examscanner.premium.data.MelcEntity
import com.examscanner.premium.data.QuestionMelcMappingEntity
import com.examscanner.premium.data.SectionEntity
import com.examscanner.premium.data.StudentAnswerEntity
import com.examscanner.premium.data.StudentEnrollmentEntity
import com.examscanner.premium.data.StudentEntity
import com.examscanner.premium.data.StudentMelcMasteryEntity
import com.examscanner.premium.data.StudentNoteEntity
import com.examscanner.premium.data.SubjectFolderEntity
import com.examscanner.premium.data.SyncLogEntity
import com.examscanner.premium.data.TemplateEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Combinators
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import net.jqwik.api.constraints.FloatRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Property-based tests for [AnalyticsEngine]'s item-analysis statistics.
 *
 * These are JVM unit tests (jqwik on the JUnit 5 platform) and run WITHOUT a
 * device or a real database. `calculateDifficulty` and
 * `calculateDiscriminationIndex` are `suspend` functions that read exam data
 * through [ExamRepository]; rather than mock the repository (no mocking library
 * is on the test classpath) we drive the REAL [ExamRepository] and REAL
 * [AnalyticsEngine] over an in-memory [FakeExamDao]. This exercises the actual
 * production code paths (`getStudents`, `getAnswerKeys`, `getStudentAnswers`,
 * `calculateScore`) with no I/O.
 *
 * Properties covered:
 *  - Property 3 (Req 6.1): difficulty index equals proportion correct x 100.
 *  - Property 4 (Req 6.2, 6.3): discrimination index is bounded to [-1, 1] and
 *    [AnalyticsEngine.classifyDiscrimination] partitions the real line into the
 *    four defined quality bands.
 */
class AnalyticsEngineItemAnalysisPropertyTest {

    // ---------------------------------------------------------------------
    // Task 4.5 — Property 3: Difficulty index equals proportion correct.
    // ---------------------------------------------------------------------

    // Feature: offline-assessment-transformation, Property 3: Difficulty index equals proportion correct
    // Validates: Requirements 6.1
    //
    // For any cohort and set of answers, AnalyticsEngine.calculateDifficulty for
    // a keyed question equals (students answering correctly / total students) x
    // 100, always lies within [0, 100], and is 0 for an empty cohort.
    @Property(tries = 100)
    fun `difficulty index equals proportion correct times 100`(
        @ForAll("cohorts") cohort: Cohort
    ) = runTest {
        val engine = engineFor(cohort)
        val actual = engine.calculateDifficulty(EXAM_ID, QUESTION)

        // Independent oracle over the same generated data.
        val total = cohort.students.size
        val correct = cohort.students.count { it.correct }
        val expected = if (total == 0) 0f else (correct.toFloat() / total) * 100f

        assertEquals(expected, actual, 1e-4f, "cohort=$cohort")

        // Bounded to the inclusive percentage range.
        assertTrue(actual in 0f..100f, "difficulty out of [0,100]: $actual")
    }

    // Feature: offline-assessment-transformation, Property 3: empty cohort yields zero difficulty
    // Validates: Requirements 6.1
    @Property(tries = 100)
    fun `empty cohort has zero difficulty`(
        @ForAll("answerLabels") correctAnswer: String
    ) = runTest {
        val engine = engineFor(Cohort(students = emptyList(), correctAnswer = correctAnswer))
        assertEquals(0f, engine.calculateDifficulty(EXAM_ID, QUESTION), 0f)
    }

    // ---------------------------------------------------------------------
    // Task 4.6 — Property 4: Discrimination index bounded and classified.
    // ---------------------------------------------------------------------

    // Feature: offline-assessment-transformation, Property 4: Discrimination index is bounded and classified consistently
    // Validates: Requirements 6.2, 6.3
    //
    // For any cohort of at least MIN_DISCRIMINATION_SAMPLE students, the
    // discrimination index computed by the real engine lies within [-1, 1].
    @Property(tries = 100)
    fun `discrimination index is bounded to minus one and plus one`(
        @ForAll("discriminationCohorts") cohort: Cohort
    ) = runTest {
        val engine = engineFor(cohort)
        val index = engine.calculateDiscriminationIndex(EXAM_ID, QUESTION)

        assertTrue(
            index in -1f..1f,
            "discrimination index out of [-1,1]: $index for cohort size ${cohort.students.size}"
        )
    }

    // Feature: offline-assessment-transformation, Property 4: Discrimination index is bounded and classified consistently
    // Validates: Requirements 6.2, 6.3
    //
    // classifyDiscrimination is a PURE function. For any index value it returns
    // exactly the band defined by Req 6.3:
    //   Poor      (index < 0.20)
    //   Fair      (0.20 <= index < 0.30)
    //   Good      (0.30 <= index < 0.40)
    //   Excellent (index >= 0.40)
    @Property(tries = 100)
    fun `classifyDiscrimination partitions into the four defined bands`(
        @ForAll @FloatRange(min = -1f, max = 1f) index: Float
    ) {
        val engine = AnalyticsEngine(ExamRepository(FakeExamDao()))
        val actual = engine.classifyDiscrimination(index)

        val expected = when {
            index < 0.20f -> AnalyticsEngine.DiscriminationQuality.POOR
            index < 0.30f -> AnalyticsEngine.DiscriminationQuality.FAIR
            index < 0.40f -> AnalyticsEngine.DiscriminationQuality.GOOD
            else -> AnalyticsEngine.DiscriminationQuality.EXCELLENT
        }

        assertEquals(expected, actual, "index=$index")
        // Result is always one of the four defined bands.
        assertTrue(
            actual in AnalyticsEngine.DiscriminationQuality.values(),
            "unexpected band $actual"
        )
    }

    // Explicit band edges the four inequalities hinge on (Req 6.3).
    @Property(tries = 100)
    fun `classifyDiscrimination band edges classify exactly`(
        @ForAll("bandEdges") edge: Float
    ) {
        val engine = AnalyticsEngine(ExamRepository(FakeExamDao()))
        val expected = when {
            edge < 0.20f -> AnalyticsEngine.DiscriminationQuality.POOR
            edge < 0.30f -> AnalyticsEngine.DiscriminationQuality.FAIR
            edge < 0.40f -> AnalyticsEngine.DiscriminationQuality.GOOD
            else -> AnalyticsEngine.DiscriminationQuality.EXCELLENT
        }
        assertEquals(expected, engine.classifyDiscrimination(edge), "edge=$edge")
    }

    @Provide
    fun bandEdges(): Arbitrary<Float> = Arbitraries.of(
        -1f, 0f, 0.1999f, 0.20f, 0.2001f, 0.2999f, 0.30f, 0.3001f,
        0.3999f, 0.40f, 0.4001f, 0.5f, 1f
    )

    // ---------------------------------------------------------------------
    // Generators
    // ---------------------------------------------------------------------

    /** A single student's response to the one keyed question under test. */
    data class Resp(val answer: String, val correct: Boolean)

    /** A generated cohort: the students' responses plus the correct answer. */
    data class Cohort(val students: List<Resp>, val correctAnswer: String)

    /** Cohorts of size 0..40 for the difficulty property (empty allowed). */
    @Provide
    fun cohorts(): Arbitrary<Cohort> = buildCohorts(minStudents = 0, maxStudents = 40)

    /**
     * Cohorts of size 10..40 for the discrimination property (the engine returns
     * 0 below MIN_DISCRIMINATION_SAMPLE, which is still within [-1, 1], but we
     * bias generation toward samples that actually compute an index).
     */
    @Provide
    fun discriminationCohorts(): Arbitrary<Cohort> =
        buildCohorts(minStudents = AnalyticsEngine.MIN_DISCRIMINATION_SAMPLE, maxStudents = 40)

    @Provide
    fun answerLabels(): Arbitrary<String> = Arbitraries.of("A", "B", "C", "D", "E", "F", "G")

    private fun buildCohorts(minStudents: Int, maxStudents: Int): Arbitrary<Cohort> {
        return answerLabels().flatMap { correct ->
            val respArb: Arbitrary<Resp> = Arbitraries.oneOf(
                // A correct response: answer equals the key.
                Arbitraries.just(Resp(answer = correct, correct = true)),
                // A wrong response: any label other than the key, or a blank.
                Arbitraries.of("A", "B", "C", "D", "E", "F", "G", "")
                    .filter { it != correct }
                    .map { Resp(answer = it, correct = false) }
            )
            respArb.list().ofMinSize(minStudents).ofMaxSize(maxStudents)
                .map { Cohort(students = it, correctAnswer = correct) }
        }
    }

    // ---------------------------------------------------------------------
    // Real engine wiring over an in-memory DAO
    // ---------------------------------------------------------------------

    private fun engineFor(cohort: Cohort): AnalyticsEngine {
        val students = cohort.students.mapIndexed { i, _ ->
            StudentEntity(id = (i + 1).toLong(), studentId = "S$i", name = "Student $i", examId = EXAM_ID)
        }
        val answers: Map<Long, List<StudentAnswerEntity>> = cohort.students
            .mapIndexed { i, resp ->
                val studentRowId = (i + 1).toLong()
                studentRowId to listOf(
                    StudentAnswerEntity(
                        studentEntityId = studentRowId,
                        questionNumber = QUESTION,
                        answer = resp.answer
                    )
                )
            }.toMap()
        val keys = listOf(
            AnswerKeyEntity(
                examId = EXAM_ID,
                questionNumber = QUESTION,
                correctAnswer = cohort.correctAnswer,
                points = 1
            )
        )
        val dao = FakeExamDao(
            studentsByExam = mapOf(EXAM_ID to students),
            answerKeysByExam = mapOf(EXAM_ID to keys),
            studentAnswersById = answers
        )
        return AnalyticsEngine(ExamRepository(dao))
    }

    companion object {
        private const val EXAM_ID = 1L
        private const val QUESTION = 1
    }

    /**
     * In-memory [ExamDao] fake. Only the reads used by [AnalyticsEngine] and the
     * repository's `calculateScore` (getStudents, getAnswerKeys,
     * getStudentAnswers) are implemented; everything else is intentionally
     * unsupported so accidental new dependencies surface loudly.
     */
    class FakeExamDao(
        private val studentsByExam: Map<Long, List<StudentEntity>> = emptyMap(),
        private val answerKeysByExam: Map<Long, List<AnswerKeyEntity>> = emptyMap(),
        private val studentAnswersById: Map<Long, List<StudentAnswerEntity>> = emptyMap()
    ) : ExamDao {

        override fun getStudents(examId: Long): Flow<List<StudentEntity>> =
            flowOf(studentsByExam[examId] ?: emptyList())

        override fun getAnswerKeys(examId: Long): Flow<List<AnswerKeyEntity>> =
            flowOf(answerKeysByExam[examId] ?: emptyList())

        override suspend fun getStudentAnswers(studentId: Long): List<StudentAnswerEntity> =
            studentAnswersById[studentId] ?: emptyList()

        // --- Everything below is unused by these tests ---
        private fun unused(): Nothing = error("FakeExamDao: method not supported in this test")

        override fun getAllSubjectFolders(): Flow<List<SubjectFolderEntity>> = unused()
        override suspend fun getSubjectFolder(folderId: Long): SubjectFolderEntity? = unused()
        override suspend fun insertSubjectFolder(folder: SubjectFolderEntity): Long = unused()
        override suspend fun updateSubjectFolder(folder: SubjectFolderEntity) = unused()
        override suspend fun softDeleteSubjectFolder(folderId: Long, timestamp: Long) = unused()
        override fun getSections(folderId: Long): Flow<List<SectionEntity>> = unused()
        override suspend fun insertSection(section: SectionEntity): Long = unused()
        override suspend fun updateSection(section: SectionEntity) = unused()
        override suspend fun softDeleteSection(sectionId: Long, timestamp: Long) = unused()
        override suspend fun getSectionById(sectionId: Long): SectionEntity? = unused()
        override suspend fun getTotalSectionsCount(): Int = unused()
        override suspend fun getTotalExamsCount(): Int = unused()
        override fun getMelcs(subject: String, gradeLevel: String): Flow<List<MelcEntity>> = unused()
        override fun getMelcsByQuarter(quarter: Int): Flow<List<MelcEntity>> = unused()
        override fun getAllMelcs(): Flow<List<MelcEntity>> = unused()
        override suspend fun getAllMelcsSync(): List<MelcEntity> = unused()
        override suspend fun insertMelcs(melcs: List<MelcEntity>) = unused()
        override fun getAllTemplates(): Flow<List<TemplateEntity>> = unused()
        override fun getBuiltInTemplates(): Flow<List<TemplateEntity>> = unused()
        override suspend fun getTemplate(templateId: Long): TemplateEntity? = unused()
        override suspend fun insertTemplate(template: TemplateEntity): Long = unused()
        override suspend fun insertTemplates(templates: List<TemplateEntity>) = unused()
        override suspend fun updateTemplate(template: TemplateEntity) = unused()
        override suspend fun deleteTemplate(template: TemplateEntity) = unused()
        override fun getAllGradingScales(): Flow<List<GradingScaleEntity>> = unused()
        override fun getBuiltInGradingScales(): Flow<List<GradingScaleEntity>> = unused()
        override suspend fun getGradingScaleByType(scaleType: String): GradingScaleEntity? = unused()
        override suspend fun insertGradingScale(scale: GradingScaleEntity): Long = unused()
        override suspend fun insertGradingScales(scales: List<GradingScaleEntity>) = unused()
        override suspend fun updateGradingScale(scale: GradingScaleEntity) = unused()
        override suspend fun deleteGradingScale(scale: GradingScaleEntity) = unused()
        override fun getQuestionMelcMappings(examId: Long): Flow<List<QuestionMelcMappingEntity>> = unused()
        override suspend fun insertQuestionMelcMapping(mapping: QuestionMelcMappingEntity): Long = unused()
        override suspend fun insertQuestionMelcMappings(mappings: List<QuestionMelcMappingEntity>) = unused()
        override suspend fun deleteQuestionMelcMappings(examId: Long) = unused()
        override fun getAllExams(): Flow<List<com.examscanner.premium.data.ExamEntity>> = unused()
        override fun getExamsByFolder(folderId: Long): Flow<List<com.examscanner.premium.data.ExamEntity>> = unused()
        override suspend fun getExam(examId: Long): com.examscanner.premium.data.ExamEntity? = unused()
        override suspend fun getLatestExam(): com.examscanner.premium.data.ExamEntity? = unused()
        override suspend fun insertExam(exam: com.examscanner.premium.data.ExamEntity): Long = unused()
        override suspend fun updateExam(exam: com.examscanner.premium.data.ExamEntity) = unused()
        override suspend fun deleteExam(exam: com.examscanner.premium.data.ExamEntity) = unused()
        override suspend fun softDeleteExam(examId: Long, timestamp: Long) = unused()
        override suspend fun insertAnswerKey(answerKey: AnswerKeyEntity) = unused()
        override suspend fun insertAnswerKeys(answerKeys: List<AnswerKeyEntity>) = unused()
        override suspend fun updateAnswerKey(answerKey: AnswerKeyEntity) = unused()
        override suspend fun deleteAnswerKeys(examId: Long) = unused()
        override fun getStudentsBySection(sectionId: Long): Flow<List<StudentEntity>> = unused()
        override suspend fun insertStudent(student: StudentEntity): Long = unused()
        override suspend fun updateStudent(student: StudentEntity) = unused()
        override suspend fun deleteStudent(student: StudentEntity) = unused()
        override suspend fun getStudentByStudentId(studentId: String, sectionId: Long): StudentEntity? = unused()
        override suspend fun insertStudentAnswers(answers: List<StudentAnswerEntity>) = unused()
        override suspend fun deleteStudents(examId: Long) = unused()
        override suspend fun deleteStudentAnswers(examId: Long) = unused()
        override fun getUnassignedStudents(examId: Long): Flow<List<StudentEntity>> = unused()
        override suspend fun updateStudentSection(studentId: Long, sectionId: Long) = unused()
        override fun getStudentAnswersForQuestion(examId: Long, questionNumber: Int): Flow<List<StudentAnswerEntity>> = unused()
        override fun getStudentMelcMastery(studentId: Long): Flow<List<StudentMelcMasteryEntity>> = unused()
        override suspend fun insertStudentMelcMastery(mastery: StudentMelcMasteryEntity) = unused()
        override fun getDeletedExams(cutoffTime: Long): Flow<List<com.examscanner.premium.data.ExamEntity>> = unused()
        override suspend fun restoreExam(examId: Long) = unused()
        override fun getDeletedSubjectFolders(cutoffTime: Long): Flow<List<SubjectFolderEntity>> = unused()
        override suspend fun getExpiredSubjectFolders(cutoffTime: Long): List<SubjectFolderEntity> = unused()
        override suspend fun restoreSubjectFolder(folderId: Long) = unused()
        override suspend fun hardDeleteSubjectFolder(folderId: Long) = unused()
        override fun getDeletedSections(cutoffTime: Long): Flow<List<SectionEntity>> = unused()
        override suspend fun getExpiredSections(cutoffTime: Long): List<SectionEntity> = unused()
        override suspend fun restoreSection(sectionId: Long) = unused()
        override suspend fun hardDeleteSection(sectionId: Long) = unused()
        override suspend fun getExpiredExams(cutoffTime: Long): List<com.examscanner.premium.data.ExamEntity> = unused()
        override suspend fun getExamsBySubjectPaged(folderId: Long, limit: Int, offset: Int): List<com.examscanner.premium.data.ExamEntity> = unused()
        override suspend fun getAllStudentRecords(studentId: String): List<StudentEntity> = unused()
        override suspend fun getStudentById(id: Long): StudentEntity? = unused()
        override fun getEnrollmentsBySection(sectionId: Long): Flow<List<StudentEnrollmentEntity>> = unused()
        override fun getEnrollmentsByStudent(studentId: Long): Flow<List<StudentEnrollmentEntity>> = unused()
        override suspend fun getEnrollment(studentId: Long, sectionId: Long): StudentEnrollmentEntity? = unused()
        override suspend fun enrollStudent(enrollment: StudentEnrollmentEntity): Long = unused()
        override suspend fun unenrollStudent(studentId: Long, sectionId: Long) = unused()
        override suspend fun getMelcCoverageByMelc(melcId: Long): MelcCoverageEntity? = unused()
        override suspend fun insertMelcCoverage(coverage: MelcCoverageEntity): Long = unused()
        override suspend fun deleteMelcCoverageByMelc(melcId: Long) = unused()
        override fun getCoverageBySubjectAndQuarter(subject: String, quarter: Int): Flow<List<MelcCoverageEntity>> = unused()
        override fun getCoverageBySubjectFolder(subjectId: Long): Flow<List<MelcCoverageEntity>> = unused()
        override fun getStudentNotes(studentId: Long): Flow<List<StudentNoteEntity>> = unused()
        override suspend fun insertStudentNote(note: StudentNoteEntity): Long = unused()
        override suspend fun updateStudentNote(note: StudentNoteEntity) = unused()
        override suspend fun deleteStudentNote(note: StudentNoteEntity) = unused()
        override fun getRecentSyncLogs(limit: Int): Flow<List<SyncLogEntity>> = unused()
        override suspend fun insertSyncLog(log: SyncLogEntity): Long = unused()
        override suspend fun clearSyncLogs() = unused()
    }
}
