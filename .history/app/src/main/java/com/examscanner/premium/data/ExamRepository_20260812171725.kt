package com.examscanner.premium.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
    
    suspend fun createExam(name: String, totalQuestions: Int): Long {
        val exam = ExamEntity(name = name, totalQuestions = totalQuestions)
        return dao.insertExam(exam)
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
