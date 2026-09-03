package com.examscanner.premium.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

// Answer Sheet Templates for customization
@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val totalQuestions: Int,
    val numberOfChoices: Int = 4, // 2-6 (A-F)
    val templateType: String = "STANDARD", // STANDARD, MULTI_SECTION, TRUE_FALSE
    val sectionsJson: String = "[]", // JSON array of sections
    val isBuiltIn: Boolean = false,
    val headerText: String = "",
    val includeSchoolLogo: Boolean = false,
    val qrCodePosition: String = "TOP_RIGHT",
    val filePath: String = "",
    val fileType: String = "PDF",
    val createdAt: Long = System.currentTimeMillis()
)

// Grading Scale Presets
@Entity(tableName = "grading_scales")
data class GradingScaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, // "DepEd K-12", "Traditional", "IB", "Custom"
    val scaleType: String, // "DEPED_K12", "TRADITIONAL", "CUSTOM"
    val minGrade: Int = 60,
    val maxGrade: Int = 100,
    val passingGrade: Int = 75,
    val transmutationJson: String = "[]", // JSON array of grade brackets
    val isBuiltIn: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

// Updated Exams entity with new relationships and customization
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
    // Grading customization
    val gradingScale: String = "DEPED_K12", // DEPED_K12, TRADITIONAL, CUSTOM
    val passingGrade: Int = 75,
    val useNegativeMarking: Boolean = false,
    val negativeMarkValue: Float = 0f,
    // Exam settings
    val examDate: Long? = null,
    val timeLimit: Int? = null, // minutes
    val allowLateScans: Boolean = true,
    // Metadata
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

// Question-MELC mapping for competency tracking
@Entity(
    tableName = "question_melc_mappings",
    foreignKeys = [
        ForeignKey(
            entity = ExamEntity::class,
            parentColumns = ["id"],
            childColumns = ["examId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MelcEntity::class,
            parentColumns = ["id"],
            childColumns = ["melcId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class QuestionMelcMappingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examId: Long,
    val questionNumber: Int,
    val melcId: Long
)

// Enhanced Students entity with profiles
@Entity(
    tableName = "students",
    foreignKeys = [
        ForeignKey(
            entity = SectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: String,
    val name: String,
    val sectionId: Long = 0,
    val examId: Long = 0, // For backward compatibility
    val gradeLevel: String = "",
    val contactInfo: String = "",
    val photoPath: String = "",
    val scannedAt: Long = System.currentTimeMillis()
)

// Student MELC Mastery tracking
@Entity(
    tableName = "student_melc_mastery",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MelcEntity::class,
            parentColumns = ["id"],
            childColumns = ["melcId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class StudentMelcMasteryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val melcId: Long,
    val masteryLevel: String, // Developing, Approaching, Proficient, Advanced
    val percentage: Float,
    val lastUpdated: Long = System.currentTimeMillis()
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
    // Subject Folders
    @Query("SELECT * FROM subject_folders WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllSubjectFolders(): Flow<List<SubjectFolderEntity>>
    
    @Query("SELECT * FROM subject_folders WHERE id = :folderId")
    suspend fun getSubjectFolder(folderId: Long): SubjectFolderEntity?
    
    @Insert
    suspend fun insertSubjectFolder(folder: SubjectFolderEntity): Long
    
    @Update
    suspend fun updateSubjectFolder(folder: SubjectFolderEntity)
    
    @Query("UPDATE subject_folders SET isDeleted = 1, deletedAt = :timestamp WHERE id = :folderId")
    suspend fun softDeleteSubjectFolder(folderId: Long, timestamp: Long = System.currentTimeMillis())
    
    // Sections
    @Query("SELECT * FROM sections WHERE subjectFolderId = :folderId AND isDeleted = 0 ORDER BY name")
    fun getSections(folderId: Long): Flow<List<SectionEntity>>
    
    @Insert
    suspend fun insertSection(section: SectionEntity): Long
    
    @Update
    suspend fun updateSection(section: SectionEntity)
    
    @Query("UPDATE sections SET isDeleted = 1, deletedAt = :timestamp WHERE id = :sectionId")
    suspend fun softDeleteSection(sectionId: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT * FROM sections WHERE id = :sectionId AND isDeleted = 0")
    suspend fun getSectionById(sectionId: Long): SectionEntity?
    
    @Query("SELECT COUNT(*) FROM sections WHERE isDeleted = 0")
    suspend fun getTotalSectionsCount(): Int
    
    @Query("SELECT COUNT(*) FROM exams WHERE isDeleted = 0")
    suspend fun getTotalExamsCount(): Int
    
    // MELCs
    @Query("SELECT * FROM melcs WHERE subject = :subject AND gradeLevel = :gradeLevel ORDER BY quarter, code")
    fun getMelcs(subject: String, gradeLevel: String): Flow<List<MelcEntity>>
    
    @Query("SELECT * FROM melcs WHERE quarter = :quarter")
    fun getMelcsByQuarter(quarter: Int): Flow<List<MelcEntity>>
    
    @Insert
    suspend fun insertMelcs(melcs: List<MelcEntity>)
    
    // Templates
    @Query("SELECT * FROM templates ORDER BY createdAt DESC")
    fun getAllTemplates(): Flow<List<TemplateEntity>>
    
    @Query("SELECT * FROM templates WHERE isBuiltIn = 1 ORDER BY totalQuestions")
    fun getBuiltInTemplates(): Flow<List<TemplateEntity>>
    
    @Query("SELECT * FROM templates WHERE id = :templateId")
    suspend fun getTemplate(templateId: Long): TemplateEntity?
    
    @Insert
    suspend fun insertTemplate(template: TemplateEntity): Long
    
    @Insert
    suspend fun insertTemplates(templates: List<TemplateEntity>)
    
    @Update
    suspend fun updateTemplate(template: TemplateEntity)
    
    @Delete
    suspend fun deleteTemplate(template: TemplateEntity)
    
    // Grading Scales
    @Query("SELECT * FROM grading_scales ORDER BY isBuiltIn DESC, name ASC")
    fun getAllGradingScales(): Flow<List<GradingScaleEntity>>
    
    @Query("SELECT * FROM grading_scales WHERE isBuiltIn = 1")
    fun getBuiltInGradingScales(): Flow<List<GradingScaleEntity>>
    
    @Query("SELECT * FROM grading_scales WHERE scaleType = :scaleType LIMIT 1")
    suspend fun getGradingScaleByType(scaleType: String): GradingScaleEntity?
    
    @Insert
    suspend fun insertGradingScale(scale: GradingScaleEntity): Long
    
    @Insert
    suspend fun insertGradingScales(scales: List<GradingScaleEntity>)
    
    @Update
    suspend fun updateGradingScale(scale: GradingScaleEntity)
    
    @Delete
    suspend fun deleteGradingScale(scale: GradingScaleEntity)
    
    // Question-MELC Mappings
    @Query("SELECT * FROM question_melc_mappings WHERE examId = :examId ORDER BY questionNumber")
    fun getQuestionMelcMappings(examId: Long): Flow<List<QuestionMelcMappingEntity>>
    
    @Insert
    suspend fun insertQuestionMelcMapping(mapping: QuestionMelcMappingEntity): Long
    
    @Insert
    suspend fun insertQuestionMelcMappings(mappings: List<QuestionMelcMappingEntity>)
    
    @Query("DELETE FROM question_melc_mappings WHERE examId = :examId")
    suspend fun deleteQuestionMelcMappings(examId: Long)
    
    // Exams
    @Query("SELECT * FROM exams WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllExams(): Flow<List<ExamEntity>>
    
    @Query("SELECT * FROM exams WHERE subjectFolderId = :folderId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getExamsByFolder(folderId: Long): Flow<List<ExamEntity>>
    
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
    
    @Query("UPDATE exams SET isDeleted = 1, deletedAt = :timestamp WHERE id = :examId")
    suspend fun softDeleteExam(examId: Long, timestamp: Long = System.currentTimeMillis())
    
    // Answer Keys
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
    
    // Students
    @Query("SELECT * FROM students WHERE examId = :examId ORDER BY scannedAt")
    fun getStudents(examId: Long): Flow<List<StudentEntity>>
    
    @Query("SELECT * FROM students WHERE sectionId = :sectionId ORDER BY name")
    fun getStudentsBySection(sectionId: Long): Flow<List<StudentEntity>>
    
    @Insert
    suspend fun insertStudent(student: StudentEntity): Long
    
    @Update
    suspend fun updateStudent(student: StudentEntity)
    
    // Student Answers
    @Query("SELECT * FROM student_answers WHERE studentEntityId = :studentId ORDER BY questionNumber")
    suspend fun getStudentAnswers(studentId: Long): List<StudentAnswerEntity>
    
    @Insert
    suspend fun insertStudentAnswers(answers: List<StudentAnswerEntity>)
    
    @Query("DELETE FROM students WHERE examId = :examId")
    suspend fun deleteStudents(examId: Long)
    
    @Query("DELETE FROM student_answers WHERE studentEntityId IN (SELECT id FROM students WHERE examId = :examId)")
    suspend fun deleteStudentAnswers(examId: Long)
    
    // Student MELC Mastery
    @Query("SELECT * FROM student_melc_mastery WHERE studentId = :studentId")
    fun getStudentMelcMastery(studentId: Long): Flow<List<StudentMelcMasteryEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudentMelcMastery(mastery: StudentMelcMasteryEntity)
    
    // Recycle Bin
    @Query("SELECT * FROM exams WHERE isDeleted = 1 AND deletedAt > :cutoffTime")
    fun getDeletedExams(cutoffTime: Long): Flow<List<ExamEntity>>
    
    @Query("UPDATE exams SET isDeleted = 0, deletedAt = NULL WHERE id = :examId")
    suspend fun restoreExam(examId: Long)
}

@Database(
    entities = [
        SubjectFolderEntity::class,
        SectionEntity::class,
        MelcEntity::class,
        TemplateEntity::class,
        GradingScaleEntity::class,
        ExamEntity::class,
        AnswerKeyEntity::class,
        QuestionMelcMappingEntity::class,
        StudentEntity::class,
        StudentAnswerEntity::class,
        StudentMelcMasteryEntity::class
    ],
    version = 5,
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
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate MELCs database will be done here
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new tables
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS subject_folders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        settingsJson TEXT NOT NULL DEFAULT '{}',
                        createdAt INTEGER NOT NULL,
                        isDeleted INTEGER NOT NULL DEFAULT 0,
                        deletedAt INTEGER
                    )
                """)
                
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS sections (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        subjectFolderId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        capacity INTEGER NOT NULL DEFAULT 50,
                        createdAt INTEGER NOT NULL,
                        isDeleted INTEGER NOT NULL DEFAULT 0,
                        deletedAt INTEGER,
                        FOREIGN KEY(subjectFolderId) REFERENCES subject_folders(id) ON DELETE CASCADE
                    )
                """)
                
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS melcs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        code TEXT NOT NULL,
                        description TEXT NOT NULL,
                        gradeLevel TEXT NOT NULL,
                        subject TEXT NOT NULL,
                        quarter INTEGER NOT NULL
                    )
                """)
                
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS templates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        filePath TEXT NOT NULL,
                        fileType TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """)
                
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS question_melc_mappings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        examId INTEGER NOT NULL,
                        questionNumber INTEGER NOT NULL,
                        melcId INTEGER NOT NULL,
                        FOREIGN KEY(examId) REFERENCES exams(id) ON DELETE CASCADE,
                        FOREIGN KEY(melcId) REFERENCES melcs(id) ON DELETE CASCADE
                    )
                """)
                
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS student_melc_mastery (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        studentId INTEGER NOT NULL,
                        melcId INTEGER NOT NULL,
                        masteryLevel TEXT NOT NULL,
                        percentage REAL NOT NULL,
                        lastUpdated INTEGER NOT NULL,
                        FOREIGN KEY(studentId) REFERENCES students(id) ON DELETE CASCADE,
                        FOREIGN KEY(melcId) REFERENCES melcs(id) ON DELETE CASCADE
                    )
                """)
                
                // Migrate exams table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS exams_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        subjectFolderId INTEGER NOT NULL DEFAULT 0,
                        sectionId INTEGER,
                        templateId INTEGER,
                        name TEXT NOT NULL,
                        totalQuestions INTEGER NOT NULL,
                        qrCode TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL,
                        isDeleted INTEGER NOT NULL DEFAULT 0,
                        deletedAt INTEGER,
                        FOREIGN KEY(subjectFolderId) REFERENCES subject_folders(id) ON DELETE CASCADE,
                        FOREIGN KEY(sectionId) REFERENCES sections(id) ON DELETE SET NULL,
                        FOREIGN KEY(templateId) REFERENCES templates(id) ON DELETE SET NULL
                    )
                """)
                
                database.execSQL("""
                    INSERT INTO exams_new (id, name, totalQuestions, createdAt)
                    SELECT id, name, totalQuestions, createdAt FROM exams
                """)
                
                database.execSQL("DROP TABLE exams")
                database.execSQL("ALTER TABLE exams_new RENAME TO exams")
                
                // Migrate students table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS students_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        studentId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        sectionId INTEGER NOT NULL DEFAULT 0,
                        examId INTEGER NOT NULL DEFAULT 0,
                        gradeLevel TEXT NOT NULL DEFAULT '',
                        contactInfo TEXT NOT NULL DEFAULT '',
                        photoPath TEXT NOT NULL DEFAULT '',
                        scannedAt INTEGER NOT NULL,
                        FOREIGN KEY(sectionId) REFERENCES sections(id) ON DELETE CASCADE
                    )
                """)
                
                database.execSQL("""
                    INSERT INTO students_new (id, studentId, name, examId, scannedAt)
                    SELECT id, studentId, name, examId, scannedAt FROM students
                """)
                
                database.execSQL("DROP TABLE students")
                database.execSQL("ALTER TABLE students_new RENAME TO students")
            }
        }
        
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create grading_scales table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS grading_scales (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        scaleType TEXT NOT NULL,
                        minGrade INTEGER NOT NULL DEFAULT 60,
                        maxGrade INTEGER NOT NULL DEFAULT 100,
                        passingGrade INTEGER NOT NULL DEFAULT 75,
                        transmutationJson TEXT NOT NULL DEFAULT '[]',
                        isBuiltIn INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL
                    )
                """)
                
                // Update templates table with new fields
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS templates_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        totalQuestions INTEGER NOT NULL DEFAULT 0,
                        numberOfChoices INTEGER NOT NULL DEFAULT 4,
                        templateType TEXT NOT NULL DEFAULT 'STANDARD',
                        sectionsJson TEXT NOT NULL DEFAULT '[]',
                        isBuiltIn INTEGER NOT NULL DEFAULT 0,
                        headerText TEXT NOT NULL DEFAULT '',
                        includeSchoolLogo INTEGER NOT NULL DEFAULT 0,
                        qrCodePosition TEXT NOT NULL DEFAULT 'TOP_RIGHT',
                        filePath TEXT NOT NULL DEFAULT '',
                        fileType TEXT NOT NULL DEFAULT 'PDF',
                        createdAt INTEGER NOT NULL
                    )
                """)
                
                database.execSQL("""
                    INSERT INTO templates_new (id, name, filePath, fileType, createdAt)
                    SELECT id, name, filePath, fileType, createdAt FROM templates
                """)
                
                database.execSQL("DROP TABLE templates")
                database.execSQL("ALTER TABLE templates_new RENAME TO templates")
                
                // Update exams table with grading customization fields
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS exams_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        subjectFolderId INTEGER NOT NULL DEFAULT 0,
                        sectionId INTEGER,
                        templateId INTEGER,
                        name TEXT NOT NULL,
                        totalQuestions INTEGER NOT NULL,
                        qrCode TEXT NOT NULL DEFAULT '',
                        gradingScale TEXT NOT NULL DEFAULT 'DEPED_K12',
                        passingGrade INTEGER NOT NULL DEFAULT 75,
                        useNegativeMarking INTEGER NOT NULL DEFAULT 0,
                        negativeMarkValue REAL NOT NULL DEFAULT 0,
                        examDate INTEGER,
                        timeLimit INTEGER,
                        allowLateScans INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL,
                        isDeleted INTEGER NOT NULL DEFAULT 0,
                        deletedAt INTEGER,
                        FOREIGN KEY(subjectFolderId) REFERENCES subject_folders(id) ON DELETE CASCADE,
                        FOREIGN KEY(sectionId) REFERENCES sections(id) ON DELETE SET NULL,
                        FOREIGN KEY(templateId) REFERENCES templates(id) ON DELETE SET NULL
                    )
                """)
                
                database.execSQL("""
                    INSERT INTO exams_new (id, subjectFolderId, sectionId, templateId, name, totalQuestions, qrCode, createdAt, isDeleted, deletedAt)
                    SELECT id, subjectFolderId, sectionId, templateId, name, totalQuestions, qrCode, createdAt, isDeleted, deletedAt FROM exams
                """)
                
                database.execSQL("DROP TABLE exams")
                database.execSQL("ALTER TABLE exams_new RENAME TO exams")
            }
        }
        
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add subject and section columns to exams table for simple metadata
                database.execSQL("""
                    ALTER TABLE exams ADD COLUMN subject TEXT NOT NULL DEFAULT ''
                """)
                
                database.execSQL("""
                    ALTER TABLE exams ADD COLUMN section TEXT NOT NULL DEFAULT ''
                """)
            }
        }
    }
}
