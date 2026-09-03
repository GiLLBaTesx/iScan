package com.examscanner.premium.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// Subject Folders for organizing exams by curriculum area
@Entity(tableName = "subject_folders")
data class SubjectFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val settingsJson: String = "{}",
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

// Sections (class groups) within subjects
@Entity(
    tableName = "sections",
    foreignKeys = [ForeignKey(
        entity = SubjectFolderEntity::class,
        parentColumns = ["id"],
        childColumns = ["subjectFolderId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class SectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectFolderId: Long,
    val name: String,
    val capacity: Int = 50,
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

// DepEd MELCs (Most Essential Learning Competencies)
@Entity(tableName = "melcs")
data class MelcEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val description: String,
    val gradeLevel: String,
    val subject: String,
    val quarter: Int
)

// Assessment templates (PDF/Word uploads)
@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val filePath: String,
    val fileType: String,
    val createdAt: Long = System.currentTimeMillis()
)

// Updated Exams entity with new relationships
@Entity(
    tableName = "exams",
    foreignKeys = [
        ForeignKey(
            entity = SubjectFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectFolderId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class ExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectFolderId: Long = 0,
    val sectionId: Long? = null,
    val templateId: Long? = null,
    val name: String,
    val totalQuestions: Int,
    val qrCode: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

@Entity(tableName = "answer_keys")
data class AnswerKeyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examId: Long,
    val questionNumber: Int,
    val correctAnswer: String,
    val alternativeAnswers: String = "", // comma-separated
    val points: Int = 1
)

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: String,
    val name: String,
    val examId: Long,
    val scannedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "student_answers")
data class StudentAnswerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentEntityId: Long,
    val questionNumber: Int,
    val answer: String
)

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams ORDER BY createdAt DESC")
    fun getAllExams(): Flow<List<ExamEntity>>
    
    @Query("SELECT * FROM exams WHERE id = :examId")
    suspend fun getExam(examId: Long): ExamEntity?
    
    @Query("SELECT * FROM exams ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestExam(): ExamEntity?
    
    @Insert
    suspend fun insertExam(exam: ExamEntity): Long
    
    @Update
    suspend fun updateExam(exam: ExamEntity)
    
    @Delete
    suspend fun deleteExam(exam: ExamEntity)
    
    @Query("SELECT * FROM answer_keys WHERE examId = :examId ORDER BY questionNumber")
    fun getAnswerKeys(examId: Long): Flow<List<AnswerKeyEntity>>
    
    @Insert
    suspend fun insertAnswerKey(answerKey: AnswerKeyEntity)
    
    @Insert
    suspend fun insertAnswerKeys(answerKeys: List<AnswerKeyEntity>)
    
    @Update
    suspend fun updateAnswerKey(answerKey: AnswerKeyEntity)
    
    @Query("DELETE FROM answer_keys WHERE examId = :examId")
    suspend fun deleteAnswerKeys(examId: Long)
    
    @Query("SELECT * FROM students WHERE examId = :examId ORDER BY scannedAt")
    fun getStudents(examId: Long): Flow<List<StudentEntity>>
    
    @Insert
    suspend fun insertStudent(student: StudentEntity): Long
    
    @Query("SELECT * FROM student_answers WHERE studentEntityId = :studentId ORDER BY questionNumber")
    suspend fun getStudentAnswers(studentId: Long): List<StudentAnswerEntity>
    
    @Insert
    suspend fun insertStudentAnswers(answers: List<StudentAnswerEntity>)
    
    @Query("DELETE FROM students WHERE examId = :examId")
    suspend fun deleteStudents(examId: Long)
    
    @Query("DELETE FROM student_answers WHERE studentEntityId IN (SELECT id FROM students WHERE examId = :examId)")
    suspend fun deleteStudentAnswers(examId: Long)
}

@Database(
    entities = [ExamEntity::class, AnswerKeyEntity::class, StudentEntity::class, StudentAnswerEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun examDao(): ExamDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "exam_scanner_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
