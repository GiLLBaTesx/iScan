package com.examscanner.premium.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

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
        return dao.insertSubjectFolder(folder)
    }
    
    suspend fun updateSubjectFolder(folderId: Long, newName: String) {
        val folder = dao.getSubjectFolder(folderId)
        folder?.let {
            dao.updateSubjectFolder(it.copy(name = newName))
        }
    }
    
    suspend fun deleteSubjectFolder(folderId: Long) {
        dao.softDeleteSubjectFolder(folderId)
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
    
    // Clear all data
    suspend fun clearAllData() {
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
        
        // Get all valid MELC IDs to validate
        val allMelcs = getAllMelcs().first()
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
        val result = mutableMapOf<Int, MelcEntity>()
        
        mappings.forEach { mapping ->
            // Get the MELC by ID - need to add this method to DAO
            // For now, we'll return empty map
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
}

data class QuestionAnalysis(
    val questionNumber: Int,
    val correctAnswer: String,
    val correctCount: Int,
    val totalStudents: Int,
    val percentageCorrect: Int,
    val answerDistribution: Map<String, Int>
)
