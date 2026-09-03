package com.examscanner.premium.data

import com.examscanner.premium.analytics.AnalyticsTracker
import com.examscanner.premium.utils.MemoryCache
import com.examscanner.premium.utils.SecureLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class ExamWithStats(
    val exam: ExamEntity,
    val keyedQuestions: Int,
    val scannedCount: Int,
    val averageScore: Int?
)

data class StudentScore(
    val student: StudentEntity,
    val score: Int,
    val total: Int,
    val percentage: Int
)

class ExamRepository(private val dao: ExamDao) {
    
    val examDao: ExamDao get() = dao
    
    // Subject Folders
    fun getAllSubjectFolders(): Flow<List<SubjectFolderEntity>> {
        return dao.getAllSubjectFolders()
    }
    
    suspend fun createSubjectFolder(name: String): Long {
        val folder = SubjectFolderEntity(name = name)
        val id = dao.insertSubjectFolder(folder)
        // Req 19.6: invalidate the cached subject-folder list after a write.
        MemoryCache.evict(MemoryCache.KEY_ALL_SUBJECT_FOLDERS)
        return id
    }
    
    suspend fun updateSubjectFolder(folderId: Long, newName: String) {
        val folder = dao.getSubjectFolder(folderId)
        folder?.let {
            dao.updateSubjectFolder(it.copy(name = newName))
            MemoryCache.evict(MemoryCache.KEY_ALL_SUBJECT_FOLDERS)
        }
    }
    
    suspend fun deleteSubjectFolder(folderId: Long) {
        dao.softDeleteSubjectFolder(folderId)
        MemoryCache.evict(MemoryCache.KEY_ALL_SUBJECT_FOLDERS)
    }

    /**
     * Read the full subject-folder list once (suspend), served from the LRU
     * [MemoryCache] when warm (Req 19.6). Unlike [getAllSubjectFolders], which
     * returns a live [Flow] the UI observes, this is a one-shot cached read for
     * callers that just need the current snapshot. The cache is invalidated by
     * every mutating subject-folder method above, so it can never serve stale
     * data. Query time is logged for performance monitoring (Req 19.7).
     */
    suspend fun getSubjectFoldersCached(): List<SubjectFolderEntity> {
        MemoryCache.get<List<SubjectFolderEntity>>(MemoryCache.KEY_ALL_SUBJECT_FOLDERS)?.let {
            return it
        }
        val folders = SecureLogger.logQueryTime(TAG, "getAllSubjectFolders") {
            dao.getAllSubjectFolders().first()
        }
        MemoryCache.put(MemoryCache.KEY_ALL_SUBJECT_FOLDERS, folders)
        return folders
    }
    
    // Grading Scales
    fun getAllGradingScales(): Flow<List<GradingScaleEntity>> {
        return dao.getAllGradingScales()
    }
    
    fun getBuiltInGradingScales(): Flow<List<GradingScaleEntity>> {
        return dao.getBuiltInGradingScales()
    }
    
    suspend fun getGradingScaleByType(scaleType: String): GradingScaleEntity? {
        return dao.getGradingScaleByType(scaleType)
    }
    
    suspend fun initializeBuiltInGradingScales() {
        val existingScales = dao.getBuiltInGradingScales().first()
        if (existingScales.isEmpty()) {
            dao.insertGradingScales(BuiltInData.getBuiltInGradingScales())
        }
    }
    
    // Answer Sheet Templates
    fun getAllTemplates(): Flow<List<TemplateEntity>> {
        return dao.getAllTemplates()
    }
    
    fun getBuiltInTemplates(): Flow<List<TemplateEntity>> {
        return dao.getBuiltInTemplates()
    }
    
    suspend fun getTemplate(templateId: Long): TemplateEntity? {
        return dao.getTemplate(templateId)
    }
    
    suspend fun createTemplate(template: TemplateEntity): Long {
        return dao.insertTemplate(template)
    }
    
    suspend fun updateTemplate(template: TemplateEntity) {
        dao.updateTemplate(template)
    }
    
    suspend fun deleteTemplate(template: TemplateEntity) {
        dao.deleteTemplate(template)
    }
    
    suspend fun initializeBuiltInTemplates() {
        val existingTemplates = dao.getBuiltInTemplates().first()
        if (existingTemplates.isEmpty()) {
            dao.insertTemplates(BuiltInData.getBuiltInTemplates())
        }
    }
    
    // MELCs
    fun getMelcs(subject: String, gradeLevel: String): Flow<List<MelcEntity>> {
        return dao.getMelcs(subject, gradeLevel)
    }
    
    suspend fun initializeSampleMelcs() {
        val melcs = SampleMelcsData.getAllSampleMelcs()
        dao.insertMelcs(melcs)
        // Req 19.6: the MELC dataset changed; drop the cached copy so the next
        // read reflects the seeded rows.
        MemoryCache.evict(MemoryCache.KEY_ALL_MELCS)
    }

    /**
     * Read the full MELC dataset once (suspend), served from the LRU
     * [MemoryCache] when warm (Req 19.6). The MELC table is bundled, read-mostly
     * reference data, so caching the whole list avoids repeated full-table scans
     * on the hot mapping/mastery paths. Invalidated by [initializeSampleMelcs].
     * Query time is logged for performance monitoring (Req 19.7).
     */
    suspend fun getAllMelcsCached(): List<MelcEntity> {
        MemoryCache.get<List<MelcEntity>>(MemoryCache.KEY_ALL_MELCS)?.let { return it }
        val melcs = SecureLogger.logQueryTime(TAG, "getAllMelcsSync") {
            dao.getAllMelcsSync()
        }
        MemoryCache.put(MemoryCache.KEY_ALL_MELCS, melcs)
        return melcs
    }
    
    // Exams
    fun getAllExamsWithStats(): Flow<List<ExamWithStats>> {
        return dao.getAllExams().map { exams ->
            exams.map { exam ->
                ExamWithStats(
                    exam = exam,
                    keyedQuestions = 0,
                    scannedCount = 0,
                    averageScore = null
                )
            }
        }
    }
    
    fun getExamsByFolder(folderId: Long): Flow<List<ExamEntity>> {
        return dao.getExamsByFolder(folderId)
    }
    
    fun getAnswerKeys(examId: Long): Flow<List<AnswerKeyEntity>> {
        return dao.getAnswerKeys(examId)
    }
    
    fun getStudents(examId: Long): Flow<List<StudentEntity>> {
        return dao.getStudents(examId)
    }
    
    suspend fun getAnswerKeysList(examId: Long): List<AnswerKeyEntity> {
        return dao.getAnswerKeys(examId).first()
    }
    
    suspend fun getStudentsList(examId: Long): List<StudentEntity> {
        return dao.getStudents(examId).first()
    }
    
    suspend fun createExam(
        name: String, 
        totalQuestions: Int, 
        folderId: Long = 0
    ): Long {
        val exam = ExamEntity(
            name = name,
            totalQuestions = totalQuestions,
            subjectFolderId = folderId
        )
        return dao.insertExam(exam)
    }
    
    suspend fun updateExam(examId: Long, newName: String) {
        val exam = dao.getExam(examId)
        exam?.let {
            dao.updateExam(it.copy(name = newName))
        }
    }
    
    suspend fun saveAnswerKey(examId: Long, keys: List<Pair<Int, String>>) {
        dao.deleteAnswerKeys(examId)
        val entities = keys.map { (questionNum, answer) ->
            AnswerKeyEntity(
                examId = examId,
                questionNumber = questionNum,
                correctAnswer = answer
            )
        }
        dao.insertAnswerKeys(entities)
    }
    
    suspend fun saveStudentResults(
        examId: Long,
        studentId: String,
        name: String,
        answers: List<Pair<Int, String>>
    ) {
        val student = StudentEntity(
            examId = examId,
            studentId = studentId,
            name = name
        )
        val studentEntityId = dao.insertStudent(student)
        
        val answerEntities = answers.map { (questionNum, answer) ->
            StudentAnswerEntity(
                studentEntityId = studentEntityId,
                questionNumber = questionNum,
                answer = answer
            )
        }
        dao.insertStudentAnswers(answerEntities)
    }
    
    suspend fun calculateScore(student: StudentEntity, answerKeys: List<AnswerKeyEntity>): Int {
        val studentAnswers = dao.getStudentAnswers(student.id)
        var correct = 0
        
        studentAnswers.forEach { studentAnswer ->
            val key = answerKeys.find { it.questionNumber == studentAnswer.questionNumber }
            if (key?.correctAnswer == studentAnswer.answer) {
                correct += key.points
            }
        }
        
        val total = answerKeys.sumOf { it.points }
        return if (total > 0) (correct * 100) / total else 0
    }
    
    suspend fun resetExam(examId: Long) {
        dao.deleteStudentAnswers(examId)
        dao.deleteStudents(examId)
    }
    
    suspend fun deleteExam(exam: ExamEntity) {
        dao.deleteStudentAnswers(exam.id)
        dao.deleteStudents(exam.id)
        dao.deleteAnswerKeys(exam.id)
        dao.deleteExam(exam)
    }
    
    // Clear all data with automatic safety backup
    suspend fun clearAllData(context: android.content.Context): String {
        android.util.Log.i("ExamRepository", "clearAllData called - starting backup process...")
        
        // SAFETY: Create automatic backup before clearing
        val backupResult = com.examscanner.premium.utils.BackupManager.createBackup(context)
        
        var backupMessage = ""
        if (backupResult.isSuccess) {
            val backupFile = backupResult.getOrNull()
            backupMessage = "Backup created: ${backupFile?.name ?: "backup.db"}"
            android.util.Log.i("ExamRepository", "✅ SUCCESS: $backupMessage")
        } else {
            val error = backupResult.exceptionOrNull()
            android.util.Log.e("ExamRepository", "❌ BACKUP FAILED: ${error?.message}", error)
            backupMessage = "⚠️ Backup failed: ${error?.message ?: "Unknown error"}"
            // Still return the message so user knows backup failed
        }
        
        // Delete all student data
        val allExams = dao.getAllExams().first()
        allExams.forEach { exam ->
            dao.deleteStudentAnswers(exam.id)
            dao.deleteStudents(exam.id)
        }
        
        // Delete all exams and related data
        allExams.forEach { exam ->
            dao.deleteAnswerKeys(exam.id)
            dao.deleteQuestionMelcMappings(exam.id)
            dao.deleteExam(exam)
        }
        
        // Delete all folders
        val allFolders = dao.getAllSubjectFolders().first()
        allFolders.forEach { folder ->
            dao.softDeleteSubjectFolder(folder.id)
        }
        
        return backupMessage
    }
    
    // Student Roster Management
    suspend fun getStudentsBySection(sectionId: Long): List<StudentEntity> {
        return dao.getStudentsBySection(sectionId).first()
    }
    
    suspend fun addStudentToSection(
        studentId: String,
        name: String,
        gradeLevel: String,
        contactInfo: String,
        sectionId: Long
    ): Long {
        val student = StudentEntity(
            studentId = studentId,
            name = name,
            gradeLevel = gradeLevel,
            contactInfo = contactInfo,
            sectionId = sectionId
        )
        return dao.insertStudent(student)
    }
    
    suspend fun updateStudentInfo(
        student: StudentEntity,
        name: String,
        gradeLevel: String,
        contactInfo: String
    ) {
        dao.updateStudent(
            student.copy(
                name = name,
                gradeLevel = gradeLevel,
                contactInfo = contactInfo
            )
        )
    }
    
    suspend fun deleteStudent(student: StudentEntity) {
        dao.deleteStudent(student)
    }
    
    suspend fun bulkInsertStudents(students: List<StudentEntity>) {
        students.forEach { student ->
            dao.insertStudent(student)
        }
    }
    
    suspend fun getStudentByStudentId(studentId: String, sectionId: Long): StudentEntity? {
        return dao.getStudentByStudentId(studentId, sectionId)
    }
    
    // Section Management
    fun getSections(folderId: Long): Flow<List<SectionEntity>> {
        return dao.getSections(folderId)
    }
    
    suspend fun createSection(folderId: Long, name: String, capacity: Int): Long {
        val section = SectionEntity(
            subjectFolderId = folderId,
            name = name,
            capacity = capacity
        )
        return dao.insertSection(section)
    }
    
    suspend fun updateSection(section: SectionEntity, name: String, capacity: Int) {
        dao.updateSection(section.copy(name = name, capacity = capacity))
    }
    
    suspend fun deleteSection(section: SectionEntity) {
        dao.softDeleteSection(section.id)
    }
    
    // MELC Management
    fun getMelcsBySubject(subject: String, gradeLevel: String): Flow<List<MelcEntity>> {
        return dao.getMelcs(subject, gradeLevel)
    }
    
    fun getAllMelcs(): Flow<List<MelcEntity>> {
        return dao.getAllMelcs()
    }
    
    suspend fun saveQuestionMelcMappings(examId: Long, mappings: Map<Int, Long>) {
        // Clear existing mappings
        dao.deleteQuestionMelcMappings(examId)
        
        // Get all valid MELC IDs to validate (served from the LRU cache when warm)
        val allMelcs = getAllMelcsCached()
        val validMelcIds = allMelcs.map { it.id }.toSet()
        
        // Insert new mappings (only for valid MELCs)
        val entities = mappings.filter { (_, melcId) ->
            validMelcIds.contains(melcId)
        }.map { (questionNum, melcId) ->
            QuestionMelcMappingEntity(
                examId = examId,
                questionNumber = questionNum,
                melcId = melcId
            )
        }
        
        if (entities.isNotEmpty()) {
            dao.insertQuestionMelcMappings(entities)
        }
    }
    
    suspend fun getQuestionMelcMappings(examId: Long): Map<Int, MelcEntity> {
        val mappings = dao.getQuestionMelcMappings(examId).first()
        val allMelcs = getAllMelcsCached()
        val melcMap = allMelcs.associateBy { it.id }
        
        val result = mutableMapOf<Int, MelcEntity>()
        
        mappings.forEach { mapping ->
            melcMap[mapping.melcId]?.let { melc ->
                result[mapping.questionNumber] = melc
            }
        }
        
        return result
    }
    
    // Post-Scan Section Organization
    fun getUnassignedStudents(examId: Long): Flow<List<StudentEntity>> {
        return dao.getUnassignedStudents(examId)
    }
    
    suspend fun assignStudentsToSection(studentIds: List<Long>, sectionId: Long) {
        studentIds.forEach { studentId ->
            dao.updateStudentSection(studentId, sectionId)
        }
    }
    
    suspend fun autoOrganizeSections(examId: Long, folderId: Long) {
        // Get all unassigned students for this exam
        val unassignedStudents = dao.getUnassignedStudents(examId).first()
        
        // Group students by grade level or other criteria
        // For now, create sections based on groups of 30 students
        val groupSize = 30
        unassignedStudents.chunked(groupSize).forEachIndexed { index, group ->
            // Create section
            val sectionName = "Section ${(index + 1)}"
            val section = SectionEntity(
                name = sectionName,
                subjectFolderId = folderId
            )
            val sectionId = dao.insertSection(section)
            
            // Assign students to section
            group.forEach { student ->
                dao.updateStudentSection(student.id, sectionId)
            }
        }
    }
    
    suspend fun getSectionItemAnalysis(examId: Long, sectionId: Long): Map<Int, QuestionAnalysis> {
        val students = dao.getStudentsBySection(sectionId).first()
        val studentIds = students.map { it.id }
        val answerKeys = dao.getAnswerKeys(examId).first()
        
        val questionAnalysis = mutableMapOf<Int, QuestionAnalysis>()
        
        answerKeys.forEach { key ->
            val allAnswers = dao.getStudentAnswersForQuestion(examId, key.questionNumber).first()
                .filter { it.studentEntityId in studentIds }
            
            val answerDistribution = mutableMapOf<String, Int>()
            allAnswers.forEach { answer ->
                answerDistribution[answer.answer] = answerDistribution.getOrDefault(answer.answer, 0) + 1
            }
            
            val correctCount = allAnswers.count { it.answer == key.correctAnswer }
            val totalStudents = students.size
            
            questionAnalysis[key.questionNumber] = QuestionAnalysis(
                questionNumber = key.questionNumber,
                correctAnswer = key.correctAnswer,
                correctCount = correctCount,
                totalStudents = totalStudents,
                percentageCorrect = if (totalStudents > 0) (correctCount * 100) / totalStudents else 0,
                answerDistribution = answerDistribution
            )
        }
        
        return questionAnalysis
    }
    
    // Get all student answers for an exam (needed for SmartDashboard)
    suspend fun getAllStudentAnswersForExam(examId: Long): List<StudentAnswerEntity> {
        val students = dao.getStudents(examId).first()
        return students.flatMap { student -> dao.getStudentAnswers(student.id) }
    }
    
    // Recycle Bin operations
    fun getDeletedExams(): Flow<List<ExamEntity>> {
        // Get exams deleted in last 30 days
        val cutoffTime = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        return dao.getDeletedExams(cutoffTime)
    }
    
    suspend fun restoreExam(examId: Long) {
        dao.restoreExam(examId)
    }
    
    suspend fun permanentlyDeleteExam(exam: ExamEntity) {
        // Delete all related data
        dao.deleteStudentAnswers(exam.id)
        dao.deleteStudents(exam.id)
        dao.deleteAnswerKeys(exam.id)
        dao.deleteQuestionMelcMappings(exam.id)
        dao.deleteExam(exam)
    }
    
    suspend fun emptyRecycleBin() {
        val cutoffTime = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        val deletedExams = dao.getDeletedExams(cutoffTime).first()
        for (exam in deletedExams) {
            permanentlyDeleteExam(exam)
        }
    }

    // ============================================================
    // Task 3.2: Subject & section recycle-bin symmetry + pagination
    // Mirrors the exam recycle-bin methods (restoreExam /
    // permanentlyDeleteExam) for subject folders and sections, adds a
    // retention purge sweep across all three item types, and adds the
    // paginated exam read + cross-section student-record lookup used by
    // mastery/reports. Write operations are wrapped with analytics
    // tracking (AnalyticsTracker) and SecureLogger on failure per the
    // pattern established in Task 3.1.
    // Requirements: 1.6, 7.x, 10.1, 10.2, 10.4, 10.5, 10.6, 10.7, 19.2
    // ============================================================

    /** Retention window (days) for soft-deleted items in the recycle bin. */
    private val recycleBinRetentionMillis: Long
        get() = 30L * 24 * 60 * 60 * 1000L

    // ---- Subject folder recycle bin ----

    /**
     * Observe soft-deleted subject folders within the 30-day retention window.
     */
    fun getDeletedSubjectFolders(): Flow<List<SubjectFolderEntity>> {
        val cutoffTime = System.currentTimeMillis() - recycleBinRetentionMillis
        return dao.getDeletedSubjectFolders(cutoffTime)
    }

    /**
     * Restore a soft-deleted subject folder to its original location by
     * clearing its isDeleted flag and deletedAt timestamp.
     */
    suspend fun restoreSubjectFolder(folderId: Long) {
        try {
            dao.restoreSubjectFolder(folderId)
            AnalyticsTracker.trackFeatureUsage(
                userId = currentUserId(),
                featureName = "subject_folder_restored",
                success = true,
                metadata = mapOf("folder_id" to folderId)
            )
        } catch (e: Exception) {
            SecureLogger.e(TAG, "restoreSubjectFolder failed for folder $folderId", e)
            AnalyticsTracker.trackError(
                userId = currentUserId(),
                errorType = "recycle_bin_error",
                errorMessage = e.message ?: "Failed to restore subject folder",
                stackTrace = e.stackTraceToString()
            )
            throw e
        }
    }

    /**
     * Permanently delete a subject folder. Cascading foreign keys remove any
     * contained sections and exams (and their children) at the database level.
     */
    suspend fun permanentlyDeleteSubjectFolder(folderId: Long) {
        try {
            dao.hardDeleteSubjectFolder(folderId)
            AnalyticsTracker.trackFeatureUsage(
                userId = currentUserId(),
                featureName = "subject_folder_permanently_deleted",
                success = true,
                metadata = mapOf("folder_id" to folderId)
            )
        } catch (e: Exception) {
            SecureLogger.e(TAG, "permanentlyDeleteSubjectFolder failed for folder $folderId", e)
            AnalyticsTracker.trackError(
                userId = currentUserId(),
                errorType = "recycle_bin_error",
                errorMessage = e.message ?: "Failed to permanently delete subject folder",
                stackTrace = e.stackTraceToString()
            )
            throw e
        }
    }

    // ---- Section recycle bin ----

    /**
     * Observe soft-deleted sections within the 30-day retention window.
     */
    fun getDeletedSections(): Flow<List<SectionEntity>> {
        val cutoffTime = System.currentTimeMillis() - recycleBinRetentionMillis
        return dao.getDeletedSections(cutoffTime)
    }

    /**
     * Restore a soft-deleted section to its original location.
     */
    suspend fun restoreSection(sectionId: Long) {
        try {
            dao.restoreSection(sectionId)
            AnalyticsTracker.trackFeatureUsage(
                userId = currentUserId(),
                featureName = "section_restored",
                success = true,
                metadata = mapOf("section_id" to sectionId)
            )
        } catch (e: Exception) {
            SecureLogger.e(TAG, "restoreSection failed for section $sectionId", e)
            AnalyticsTracker.trackError(
                userId = currentUserId(),
                errorType = "recycle_bin_error",
                errorMessage = e.message ?: "Failed to restore section",
                stackTrace = e.stackTraceToString()
            )
            throw e
        }
    }

    /**
     * Permanently delete a section. Cascading foreign keys remove enrolled
     * students and their answers at the database level.
     */
    suspend fun permanentlyDeleteSection(sectionId: Long) {
        try {
            dao.hardDeleteSection(sectionId)
            AnalyticsTracker.trackFeatureUsage(
                userId = currentUserId(),
                featureName = "section_permanently_deleted",
                success = true,
                metadata = mapOf("section_id" to sectionId)
            )
        } catch (e: Exception) {
            SecureLogger.e(TAG, "permanentlyDeleteSection failed for section $sectionId", e)
            AnalyticsTracker.trackError(
                userId = currentUserId(),
                errorType = "recycle_bin_error",
                errorMessage = e.message ?: "Failed to permanently delete section",
                stackTrace = e.stackTraceToString()
            )
            throw e
        }
    }

    // ---- Retention purge sweep ----

    /**
     * Permanently remove every soft-deleted subject folder, section, AND exam
     * whose deletedAt timestamp is older than the retention window
     * (default 30 days). Exams are purged via [permanentlyDeleteExam] so their
     * answer keys, students, answers, and MELC mappings are cleaned up
     * explicitly; folders and sections rely on cascading foreign keys.
     *
     * @param retentionDays number of days to retain soft-deleted items.
     * @return the total number of items permanently removed.
     */
    suspend fun purgeExpiredDeleted(retentionDays: Int = 30): Int {
        return try {
            val cutoffTime = System.currentTimeMillis() - (retentionDays.toLong() * 24 * 60 * 60 * 1000L)

            val expiredExams = dao.getExpiredExams(cutoffTime)
            val expiredSections = dao.getExpiredSections(cutoffTime)
            val expiredFolders = dao.getExpiredSubjectFolders(cutoffTime)

            expiredExams.forEach { permanentlyDeleteExam(it) }
            expiredSections.forEach { dao.hardDeleteSection(it.id) }
            expiredFolders.forEach { dao.hardDeleteSubjectFolder(it.id) }

            val purgedCount = expiredExams.size + expiredSections.size + expiredFolders.size
            AnalyticsTracker.trackFeatureUsage(
                userId = currentUserId(),
                featureName = "recycle_bin_purged",
                success = true,
                metadata = mapOf(
                    "retention_days" to retentionDays,
                    "purged_count" to purgedCount
                )
            )
            purgedCount
        } catch (e: Exception) {
            SecureLogger.e(TAG, "purgeExpiredDeleted failed for retention $retentionDays days", e)
            AnalyticsTracker.trackError(
                userId = currentUserId(),
                errorType = "recycle_bin_error",
                errorMessage = e.message ?: "Failed to purge expired recycle-bin items",
                stackTrace = e.stackTraceToString()
            )
            throw e
        }
    }

    // ---- Paginated / aggregate reads ----

    /**
     * Read a page of non-deleted exams for a subject folder, newest first.
     * Backs paginated exam-list loading (Req 19.2).
     */
    suspend fun getExamsBySubjectPaged(folderId: Long, limit: Int, offset: Int): List<ExamEntity> {
        // Req 19.7: log query execution time for performance monitoring.
        return SecureLogger.logQueryTime(TAG, "getExamsBySubjectPaged") {
            dao.getExamsBySubjectPaged(folderId, limit, offset)
        }
    }

    /**
     * Return every [StudentEntity] row for a given school studentId (the String
     * id, not the Long PK), across all exams and sections. Used by
     * mastery/reports to aggregate a single student's records.
     */
    suspend fun getAllStudentRecords(studentId: String): List<StudentEntity> {
        return dao.getAllStudentRecords(studentId)
    }

    /**
     * Look up a single [StudentEntity] by its Long primary key. Used by
     * [com.examscanner.premium.analytics.MasteryCalculator] to resolve the
     * school studentId String so mastery can be aggregated across every exam
     * record that shares it.
     */
    suspend fun getStudentById(id: Long): StudentEntity? {
        return dao.getStudentById(id)
    }

    /**
     * All answers a student (identified by the [StudentEntity] Long PK)
     * submitted for their exam, ordered by question number.
     */
    suspend fun getStudentAnswers(studentEntityId: Long): List<StudentAnswerEntity> {
        return dao.getStudentAnswers(studentEntityId)
    }

    /**
     * Observe the persisted MELC mastery rows for a student (by the
     * [StudentEntity] Long PK the mastery rows are keyed on).
     */
    fun getStudentMastery(studentId: Long): Flow<List<StudentMelcMasteryEntity>> {
        return dao.getStudentMelcMastery(studentId)
    }

    /**
     * Persist a reduced MELC mastery summary row (percentage + level).
     * Backed by [ExamDao.insertStudentMelcMastery] (REPLACE on conflict).
     */
    suspend fun insertStudentMelcMastery(mastery: StudentMelcMasteryEntity) {
        dao.insertStudentMelcMastery(mastery)
    }

    // ============================================================
    // Task 3.1: Enrollment + MELC coverage + student notes access
    // Net-new reads/writes over student_enrollments, melc_coverage,
    // and student_notes, delegating to the DAO methods added in Task 1.2.
    // Operations are wrapped with analytics tracking (AnalyticsTracker)
    // and SecureLogger on failure per workspace conventions.
    // Requirements: 7.3, 8.5, 9.2, 9.6
    // ============================================================

    private companion object {
        private const val TAG = "ExamRepository"
    }

    // Resolve the authenticated user id for analytics (null when signed out / anonymous)
    private fun currentUserId(): String? =
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

    // ---- Student Enrollments (student_enrollments) ----

    /**
     * Enroll a student into a section. Delegates to [ExamDao.enrollStudent]
     * (REPLACE on conflict, so re-enrolling is idempotent per unique
     * studentId+sectionId index).
     *
     * @return the row id of the enrollment.
     */
    suspend fun enrollStudent(
        studentId: Long,
        sectionId: Long,
        status: String = "Active"
    ): Long {
        return try {
            val id = dao.enrollStudent(
                StudentEnrollmentEntity(
                    studentId = studentId,
                    sectionId = sectionId,
                    status = status
                )
            )
            AnalyticsTracker.trackFeatureUsage(
                userId = currentUserId(),
                featureName = "student_enrolled",
                success = true,
                metadata = mapOf("section_id" to sectionId)
            )
            id
        } catch (e: Exception) {
            SecureLogger.e(TAG, "enrollStudent failed for section $sectionId", e)
            AnalyticsTracker.trackError(
                userId = currentUserId(),
                errorType = "enrollment_error",
                errorMessage = e.message ?: "Failed to enroll student",
                stackTrace = e.stackTraceToString()
            )
            throw e
        }
    }

    /**
     * Remove a student's enrollment from a section.
     */
    suspend fun unenrollStudent(studentId: Long, sectionId: Long) {
        try {
            dao.unenrollStudent(studentId, sectionId)
            AnalyticsTracker.trackFeatureUsage(
                userId = currentUserId(),
                featureName = "student_unenrolled",
                success = true,
                metadata = mapOf("section_id" to sectionId)
            )
        } catch (e: Exception) {
            SecureLogger.e(TAG, "unenrollStudent failed for section $sectionId", e)
            AnalyticsTracker.trackError(
                userId = currentUserId(),
                errorType = "enrollment_error",
                errorMessage = e.message ?: "Failed to unenroll student",
                stackTrace = e.stackTraceToString()
            )
            throw e
        }
    }

    /**
     * Observe the enrollments for a section. The UI observes changes, so this
     * returns a [Flow].
     */
    fun getEnrollments(sectionId: Long): Flow<List<StudentEnrollmentEntity>> {
        return dao.getEnrollmentsBySection(sectionId)
    }

    /**
     * Observe the enrollments for a single student across sections.
     */
    fun getEnrollmentsForStudent(studentId: Long): Flow<List<StudentEnrollmentEntity>> {
        return dao.getEnrollmentsByStudent(studentId)
    }

    // ---- MELC Coverage (melc_coverage) ----

    /**
     * Upsert a manual MELC coverage record. Delegates to the transactional
     * [ExamDao.upsertMelcCoverage] which replaces any existing coverage row
     * for the same MELC.
     *
     * @return the row id of the coverage record.
     */
    suspend fun upsertMelcCoverage(
        melcId: Long,
        subjectId: Long,
        coveredAt: Long = System.currentTimeMillis(),
        notes: String = "",
        coverageType: String = "Manual"
    ): Long {
        return try {
            val id = dao.upsertMelcCoverage(
                MelcCoverageEntity(
                    melcId = melcId,
                    subjectId = subjectId,
                    coveredAt = coveredAt,
                    notes = notes,
                    coverageType = coverageType
                )
            )
            AnalyticsTracker.trackFeatureUsage(
                userId = currentUserId(),
                featureName = "melc_coverage_updated",
                success = true,
                metadata = mapOf("melc_id" to melcId, "coverage_type" to coverageType)
            )
            id
        } catch (e: Exception) {
            SecureLogger.e(TAG, "upsertMelcCoverage failed for melc $melcId", e)
            AnalyticsTracker.trackError(
                userId = currentUserId(),
                errorType = "coverage_error",
                errorMessage = e.message ?: "Failed to update MELC coverage",
                stackTrace = e.stackTraceToString()
            )
            throw e
        }
    }

    /**
     * Observe MELC coverage records for a subject and quarter. The DAO joins
     * through the melcs table (which holds subject + quarter). The UI observes
     * changes, so this returns a [Flow].
     */
    fun getCoverage(subject: String, quarter: Int): Flow<List<MelcCoverageEntity>> {
        return dao.getCoverageBySubjectAndQuarter(subject, quarter)
    }

    // ---- Student Notes (student_notes) ----

    /**
     * Add a timestamped note to a student profile. Delegates to
     * [ExamDao.insertStudentNote].
     *
     * @return the row id of the inserted note.
     */
    suspend fun addStudentNote(studentId: Long, note: String): Long {
        return try {
            val now = System.currentTimeMillis()
            val id = dao.insertStudentNote(
                StudentNoteEntity(
                    studentId = studentId,
                    note = note,
                    createdAt = now,
                    updatedAt = now
                )
            )
            AnalyticsTracker.trackFeatureUsage(
                userId = currentUserId(),
                featureName = "student_note_added",
                success = true,
                metadata = mapOf("student_id" to studentId)
            )
            id
        } catch (e: Exception) {
            SecureLogger.e(TAG, "addStudentNote failed for student $studentId", e)
            AnalyticsTracker.trackError(
                userId = currentUserId(),
                errorType = "student_note_error",
                errorMessage = e.message ?: "Failed to add student note",
                stackTrace = e.stackTraceToString()
            )
            throw e
        }
    }

    /**
     * Observe the notes for a student, newest first. The UI observes changes,
     * so this returns a [Flow].
     */
    fun getStudentNotes(studentId: Long): Flow<List<StudentNoteEntity>> {
        return dao.getStudentNotes(studentId)
    }
}

data class QuestionAnalysis(
    val questionNumber: Int,
    val correctAnswer: String,
    val correctCount: Int,
    val totalStudents: Int,
    val percentageCorrect: Int,
    val answerDistribution: Map<String, Int>
)
