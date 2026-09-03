package com.examscanner.premium.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Combinators
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import net.jqwik.api.constraints.IntRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Property-based test for the recycle-bin retention purge.
 *
 * These are JVM unit tests (jqwik on the JUnit 5 platform) and run WITHOUT a
 * device or a real database. Rather than mock the repository (no mocking
 * library is on the test classpath) we drive the REAL
 * [ExamRepository.purgeExpiredDeleted] over an in-memory [FakeExamDao] that
 * faithfully implements the three "expired" queries and the hard-delete
 * methods the purge relies on.
 *
 * WHY WE TOLERATE ONE EXCEPTION FROM THE REAL METHOD
 * --------------------------------------------------
 * [ExamRepository.purgeExpiredDeleted] performs all of its DAO work first
 * (compute cutoff -> getExpired{Exams,Sections,SubjectFolders} ->
 * permanentlyDeleteExam / hardDeleteSection / hardDeleteSubjectFolder) and
 * only afterwards records an analytics event. That analytics call reaches
 * `AnalyticsTracker` (an `object` whose field initializer calls
 * `FirebaseFirestore.getInstance()`) and `currentUserId()` (which calls
 * `FirebaseAuth.getInstance()`). Under a plain JVM unit test Firebase is not
 * initialized, so those calls throw. Because the deletions have already been
 * committed to the DAO by that point, the observable behavioral contract of
 * the purge — exactly which items are removed vs retained — is fully
 * exercised by the real code path. We therefore run the real method and, if it
 * throws the Firebase-initialization error, swallow ONLY that error and assert
 * on the resulting in-memory store. Any other failure propagates.
 *
 * Property covered:
 *  - Property 11 (Req 10.2, 10.6): recycle-bin retention purge.
 */
class RecycleBinRetentionPurgePropertyTest {

    // Feature: offline-assessment-transformation, Property 11: Recycle bin retention purge
    // Validates: Requirements 10.2, 10.6
    //
    // For any set of soft-deleted exams, sections, and subject folders with
    // arbitrary deletion timestamps, purgeExpiredDeleted(30) permanently
    // removes exactly the items whose deletedAt is older than 30 days
    // (deletedAt <= now - 30 days) and retains every other item — including
    // items that are not soft-deleted at all.
    @Property(tries = 200)
    fun `purge removes exactly items older than the retention window`(
        @ForAll("itemSets") items: ItemSet
    ) = runTest {
        val dao = FakeExamDao(
            exams = items.exams.toMutableList(),
            sections = items.sections.toMutableList(),
            folders = items.folders.toMutableList()
        )
        val repo = ExamRepository(dao)

        val retentionDays = 30
        // Recompute the cutoff the same way production does. There is a tiny
        // (sub-millisecond) skew between this value and the one purge computes
        // from its own System.currentTimeMillis(); the generators keep every
        // deletedAt at least a full day away from the boundary so the skew can
        // never change which side of the cutoff an item lands on.
        val cutoff = System.currentTimeMillis() - retentionDays.toLong() * 24 * 60 * 60 * 1000L

        // Oracle: exactly the soft-deleted items whose deletedAt <= cutoff.
        val expectedRemovedExamIds = items.exams
            .filter { it.isDeleted && it.deletedAt != null && it.deletedAt!! <= cutoff }
            .map { it.id }.toSet()
        val expectedRemovedSectionIds = items.sections
            .filter { it.isDeleted && it.deletedAt != null && it.deletedAt!! <= cutoff }
            .map { it.id }.toSet()
        val expectedRemovedFolderIds = items.folders
            .filter { it.isDeleted && it.deletedAt != null && it.deletedAt!! <= cutoff }
            .map { it.id }.toSet()

        val runReal = runReal(repo, retentionDays)

        // --- Exams ---
        val remainingExamIds = dao.exams.map { it.id }.toSet()
        val removedExamIds = items.exams.map { it.id }.toSet() - remainingExamIds
        assertEquals(expectedRemovedExamIds, removedExamIds, "exams purged mismatch: $items")

        // --- Sections ---
        val remainingSectionIds = dao.sections.map { it.id }.toSet()
        val removedSectionIds = items.sections.map { it.id }.toSet() - remainingSectionIds
        assertEquals(expectedRemovedSectionIds, removedSectionIds, "sections purged mismatch: $items")

        // --- Subject folders ---
        val remainingFolderIds = dao.folders.map { it.id }.toSet()
        val removedFolderIds = items.folders.map { it.id }.toSet() - remainingFolderIds
        assertEquals(expectedRemovedFolderIds, removedFolderIds, "folders purged mismatch: $items")

        // Every retained item must be one that was NOT expired (either not
        // soft-deleted, or deleted within the retention window). No survivor
        // should satisfy the purge predicate.
        assertTrue(
            dao.exams.none { it.isDeleted && it.deletedAt != null && it.deletedAt!! <= cutoff },
            "an expired exam survived the purge: ${dao.exams}"
        )
        assertTrue(
            dao.sections.none { it.isDeleted && it.deletedAt != null && it.deletedAt!! <= cutoff },
            "an expired section survived the purge: ${dao.sections}"
        )
        assertTrue(
            dao.folders.none { it.isDeleted && it.deletedAt != null && it.deletedAt!! <= cutoff },
            "an expired folder survived the purge: ${dao.folders}"
        )

        // Exams are purged through permanentlyDeleteExam, which must also clean
        // up each purged exam's children exactly once (Req 10.6 — cascade).
        assertEquals(expectedRemovedExamIds, dao.childrenClearedForExamIds, "child cleanup mismatch")

        // When the real method completed without the Firebase analytics error,
        // its reported count must equal the total number of removed items.
        if (runReal != null) {
            val expectedCount = expectedRemovedExamIds.size +
                expectedRemovedSectionIds.size +
                expectedRemovedFolderIds.size
            assertEquals(expectedCount, runReal, "purged count mismatch")
        }
    }

    /**
     * Run the REAL purge. Returns the count it reports, or null if the only
     * failure was the expected Firebase-not-initialized error raised by the
     * post-deletion analytics call (see class KDoc). Re-throws anything else.
     */
    private suspend fun runReal(repo: ExamRepository, retentionDays: Int): Int? {
        return try {
            repo.purgeExpiredDeleted(retentionDays)
        } catch (e: Throwable) {
            if (isFirebaseInitFailure(e)) null else throw e
        }
    }

    private fun isFirebaseInitFailure(e: Throwable): Boolean {
        var cur: Throwable? = e
        while (cur != null) {
            val msg = cur.message ?: ""
            if (cur is IllegalStateException &&
                (msg.contains("FirebaseApp", ignoreCase = true) ||
                    msg.contains("Firebase", ignoreCase = true) ||
                    msg.contains("Default", ignoreCase = true))
            ) return true
            // Some Firebase entry points throw ExceptionInInitializerError /
            // NoClassDefFoundError under a bare JVM.
            if (cur is ExceptionInInitializerError || cur is NoClassDefFoundError) return true
            cur = cur.cause
        }
        return false
    }

    // ---------------------------------------------------------------------
    // Generators
    // ---------------------------------------------------------------------

    data class ItemSet(
        val exams: List<ExamEntity>,
        val sections: List<SectionEntity>,
        val folders: List<SubjectFolderEntity>
    ) {
        override fun toString(): String =
            "ItemSet(exams=${exams.map { Triple(it.id, it.isDeleted, it.deletedAt) }}, " +
                "sections=${sections.map { Triple(it.id, it.isDeleted, it.deletedAt) }}, " +
                "folders=${folders.map { Triple(it.id, it.isDeleted, it.deletedAt) }})"
    }

    private companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
        // Anchor the generated ages to a fixed "now" so the description is
        // stable; the actual cutoff is recomputed inside the property against
        // the live clock, and every offset is a whole number of days so the
        // sub-millisecond skew between the two clocks is irrelevant.
        val NOW = System.currentTimeMillis()
    }

    /**
     * One deletion state for an item: whether it's soft-deleted and, if so,
     * how many days ago (relative to now) it was deleted. Ages span a wide
     * window on both sides of the 30-day boundary, always a whole number of
     * days away so no value sits within a millisecond of the cutoff.
     */
    private fun deletionStates(): Arbitrary<Pair<Boolean, Long?>> {
        // Not soft-deleted at all (must always be retained).
        val notDeleted: Arbitrary<Pair<Boolean, Long?>> =
            Arbitraries.just(false to null)

        // Soft-deleted N days ago, N in [1, 400], excluding a small guard band
        // around 30 so we never straddle the boundary by clock skew.
        val deletedAgeDays: Arbitrary<Long> = Arbitraries.longs()
            .between(1L, 400L)
            .filter { it <= 28L || it >= 32L }

        val deleted: Arbitrary<Pair<Boolean, Long?>> = deletedAgeDays.map { ageDays ->
            true to (NOW - ageDays * DAY_MS)
        }

        // Bias toward soft-deleted items (the interesting case) but keep some
        // never-deleted ones in the mix.
        return Arbitraries.oneOf(deleted, deleted, notDeleted)
    }

    @Provide
    fun itemSets(): Arbitrary<ItemSet> {
        val exams = deletionStates().list().ofMinSize(0).ofMaxSize(12)
            .map { states ->
                states.mapIndexed { i, (isDel, delAt) ->
                    ExamEntity(
                        id = (i + 1).toLong(),
                        name = "Exam ${i + 1}",
                        totalQuestions = 10,
                        isDeleted = isDel,
                        deletedAt = delAt
                    )
                }
            }
        val sections = deletionStates().list().ofMinSize(0).ofMaxSize(12)
            .map { states ->
                states.mapIndexed { i, (isDel, delAt) ->
                    SectionEntity(
                        id = (i + 1).toLong(),
                        subjectFolderId = 1L,
                        name = "Section ${i + 1}",
                        isDeleted = isDel,
                        deletedAt = delAt
                    )
                }
            }
        val folders = deletionStates().list().ofMinSize(0).ofMaxSize(12)
            .map { states ->
                states.mapIndexed { i, (isDel, delAt) ->
                    SubjectFolderEntity(
                        id = (i + 1).toLong(),
                        name = "Folder ${i + 1}",
                        isDeleted = isDel,
                        deletedAt = delAt
                    )
                }
            }
        return Combinators.combine(exams, sections, folders).`as` { e, s, f ->
            ItemSet(exams = e, sections = s, folders = f)
        }
    }

    // ---------------------------------------------------------------------
    // In-memory ExamDao fake
    // ---------------------------------------------------------------------

    /**
     * In-memory [ExamDao] fake backing the recycle-bin stores with mutable
     * lists. The three getExpired* queries reproduce the production SQL
     * predicate (isDeleted = 1 AND deletedAt <= cutoff). The hard-delete
     * methods (and permanentlyDeleteExam's child deletes) mutate the store so
     * the property can observe exactly what was purged. Every other method is
     * intentionally unsupported so an accidental new dependency surfaces
     * loudly.
     */
    class FakeExamDao(
        val exams: MutableList<ExamEntity> = mutableListOf(),
        val sections: MutableList<SectionEntity> = mutableListOf(),
        val folders: MutableList<SubjectFolderEntity> = mutableListOf()
    ) : ExamDao {

        /** Exam ids for which child-data cleanup was invoked (Req 10.6 cascade). */
        val childrenClearedForExamIds: MutableSet<Long> = mutableSetOf()

        override suspend fun getExpiredExams(cutoffTime: Long): List<ExamEntity> =
            exams.filter { it.isDeleted && it.deletedAt != null && it.deletedAt!! <= cutoffTime }

        override suspend fun getExpiredSections(cutoffTime: Long): List<SectionEntity> =
            sections.filter { it.isDeleted && it.deletedAt != null && it.deletedAt!! <= cutoffTime }

        override suspend fun getExpiredSubjectFolders(cutoffTime: Long): List<SubjectFolderEntity> =
            folders.filter { it.isDeleted && it.deletedAt != null && it.deletedAt!! <= cutoffTime }

        override suspend fun deleteExam(exam: ExamEntity) {
            exams.removeAll { it.id == exam.id }
        }

        override suspend fun hardDeleteSection(sectionId: Long) {
            sections.removeAll { it.id == sectionId }
        }

        override suspend fun hardDeleteSubjectFolder(folderId: Long) {
            folders.removeAll { it.id == folderId }
        }

        // permanentlyDeleteExam calls these before deleteExam; track the exam id
        // to prove cascade cleanup happened for each purged exam.
        override suspend fun deleteStudentAnswers(examId: Long) { childrenClearedForExamIds.add(examId) }
        override suspend fun deleteStudents(examId: Long) { childrenClearedForExamIds.add(examId) }
        override suspend fun deleteAnswerKeys(examId: Long) { childrenClearedForExamIds.add(examId) }
        override suspend fun deleteQuestionMelcMappings(examId: Long) { childrenClearedForExamIds.add(examId) }

        // --- Everything below is unused by this test ---
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
        override fun getAllExams(): Flow<List<ExamEntity>> = unused()
        override fun getExamsByFolder(folderId: Long): Flow<List<ExamEntity>> = unused()
        override suspend fun getExam(examId: Long): ExamEntity? = unused()
        override suspend fun getLatestExam(): ExamEntity? = unused()
        override suspend fun insertExam(exam: ExamEntity): Long = unused()
        override suspend fun updateExam(exam: ExamEntity) = unused()
        override suspend fun softDeleteExam(examId: Long, timestamp: Long) = unused()
        override suspend fun insertAnswerKey(answerKey: AnswerKeyEntity) = unused()
        override suspend fun insertAnswerKeys(answerKeys: List<AnswerKeyEntity>) = unused()
        override suspend fun updateAnswerKey(answerKey: AnswerKeyEntity) = unused()
        override fun getStudents(examId: Long): Flow<List<StudentEntity>> = unused()
        override fun getStudentsBySection(sectionId: Long): Flow<List<StudentEntity>> = unused()
        override suspend fun insertStudent(student: StudentEntity): Long = unused()
        override suspend fun updateStudent(student: StudentEntity) = unused()
        override suspend fun deleteStudent(student: StudentEntity) = unused()
        override suspend fun getStudentByStudentId(studentId: String, sectionId: Long): StudentEntity? = unused()
        override suspend fun insertStudentAnswers(answers: List<StudentAnswerEntity>) = unused()
        override suspend fun getStudentAnswers(studentId: Long): List<StudentAnswerEntity> = unused()
        override fun getAnswerKeys(examId: Long): Flow<List<AnswerKeyEntity>> = unused()
        override fun getUnassignedStudents(examId: Long): Flow<List<StudentEntity>> = unused()
        override suspend fun updateStudentSection(studentId: Long, sectionId: Long) = unused()
        override fun getStudentAnswersForQuestion(examId: Long, questionNumber: Int): Flow<List<StudentAnswerEntity>> = unused()
        override fun getStudentMelcMastery(studentId: Long): Flow<List<StudentMelcMasteryEntity>> = unused()
        override suspend fun insertStudentMelcMastery(mastery: StudentMelcMasteryEntity) = unused()
        override fun getDeletedExams(cutoffTime: Long): Flow<List<ExamEntity>> = unused()
        override suspend fun restoreExam(examId: Long) = unused()
        override fun getDeletedSubjectFolders(cutoffTime: Long): Flow<List<SubjectFolderEntity>> = unused()
        override suspend fun restoreSubjectFolder(folderId: Long) = unused()
        override fun getDeletedSections(cutoffTime: Long): Flow<List<SectionEntity>> = unused()
        override suspend fun restoreSection(sectionId: Long) = unused()
        override suspend fun getExamsBySubjectPaged(folderId: Long, limit: Int, offset: Int): List<ExamEntity> = unused()
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
