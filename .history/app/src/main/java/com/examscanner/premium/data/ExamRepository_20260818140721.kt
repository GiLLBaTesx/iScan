package com.examscanner.premium.data

import kotlinx.coroutines.flow.Flow
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
    
    // Sections
    fun getSections(folderId: Long): Flow<List<SectionEntity>> {
        return dao.getSections(folderId)
    }
    
    suspend fun createSection(folderId: Long, name: String, capacity: Int = 50): Long {
        val section = SectionEntity(
            subjectFolderId = folderId,
            name = name,
            capacity = capacity
        )
        return dao.insertSection(section)
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
        folderId: Long = 0,
        subject: String = "",
        section: String = ""
    ): Long {
        val exam = ExamEntity(
            name = name,
            totalQuestions = totalQuestions,
            subjectFolderId = folderId,
            subject = subject,
            section = section
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
}
