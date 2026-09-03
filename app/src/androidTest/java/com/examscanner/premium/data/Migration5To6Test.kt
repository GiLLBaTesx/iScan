package com.examscanner.premium.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migration5To6Test - validates the additive v5 -> v6 Room migration (Requirement 18.7).
 *
 * Seeds a representative v5 database (subject_folders, exams, students with a String
 * gradeLevel), runs [AppDatabase.MIGRATION_5_6], and asserts:
 *   1. every pre-existing row survives the migration untouched, and
 *   2. the four net-new tables (student_enrollments, melc_coverage, student_notes,
 *      sync_logs) exist and are queryable.
 *
 * SQLCipher-in-test caveat (see design.md "Testing Strategy"): production opens the DB
 * with a SQLCipher SupportFactory, but MigrationTestHelper does not use that factory by
 * default. The point of this test is to validate the *schema/migration SQL* correctness,
 * so we deliberately let MigrationTestHelper create/validate an ordinary UNENCRYPTED test
 * DB via FrameworkSQLiteOpenHelperFactory. Encryption is an orthogonal concern verified by
 * the security smoke test (Req 25.1), not here.
 *
 * Note on the seed step: only the v6 schema is exported (exportSchema was false at v5, so
 * there is no 5.json). MigrationTestHelper.createDatabase() requires the exported schema for
 * the requested version, which we don't have, so we hand-build the v5 database file directly
 * with a plain framework SQLite helper (raw SQL shapes copied verbatim from the exported schema
 * and prior migrations, plus PRAGMA user_version = 5). runMigrationsAndValidate(..., 6, ...)
 * then reopens that file, applies MIGRATION_5_6, and validates the result against 6.json.
 */
@RunWith(AndroidJUnit4::class)
class Migration5To6Test {

    companion object {
        private const val TEST_DB = "migration-5-6-test-db"

        /**
         * The 11 v5 tables, copied verbatim from the exported schema (they are byte-for-byte
         * identical in v6 since MIGRATION_5_6 is additive). Used to seed a complete v5 database
         * so runMigrationsAndValidate can validate against the full v6 schema.
         */
        private val V5_TABLE_CREATE_SQL = listOf(
            "CREATE TABLE IF NOT EXISTS `subject_folders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `settingsJson` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `deletedAt` INTEGER)",
            "CREATE TABLE IF NOT EXISTS `sections` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `subjectFolderId` INTEGER NOT NULL, `name` TEXT NOT NULL, `capacity` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `deletedAt` INTEGER, FOREIGN KEY(`subjectFolderId`) REFERENCES `subject_folders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE TABLE IF NOT EXISTS `melcs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `code` TEXT NOT NULL, `description` TEXT NOT NULL, `gradeLevel` TEXT NOT NULL, `subject` TEXT NOT NULL, `quarter` INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS `templates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `totalQuestions` INTEGER NOT NULL, `numberOfChoices` INTEGER NOT NULL, `templateType` TEXT NOT NULL, `sectionsJson` TEXT NOT NULL, `isBuiltIn` INTEGER NOT NULL, `headerText` TEXT NOT NULL, `includeSchoolLogo` INTEGER NOT NULL, `qrCodePosition` TEXT NOT NULL, `filePath` TEXT NOT NULL, `fileType` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS `grading_scales` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `scaleType` TEXT NOT NULL, `minGrade` INTEGER NOT NULL, `maxGrade` INTEGER NOT NULL, `passingGrade` INTEGER NOT NULL, `transmutationJson` TEXT NOT NULL, `isBuiltIn` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS `exams` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `subjectFolderId` INTEGER NOT NULL, `sectionId` INTEGER, `templateId` INTEGER, `name` TEXT NOT NULL, `totalQuestions` INTEGER NOT NULL, `qrCode` TEXT NOT NULL, `gradingScale` TEXT NOT NULL, `passingGrade` INTEGER NOT NULL, `useNegativeMarking` INTEGER NOT NULL, `negativeMarkValue` REAL NOT NULL, `examDate` INTEGER, `timeLimit` INTEGER, `allowLateScans` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL, `deletedAt` INTEGER, FOREIGN KEY(`subjectFolderId`) REFERENCES `subject_folders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`sectionId`) REFERENCES `sections`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , FOREIGN KEY(`templateId`) REFERENCES `templates`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
            "CREATE TABLE IF NOT EXISTS `answer_keys` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `examId` INTEGER NOT NULL, `questionNumber` INTEGER NOT NULL, `correctAnswer` TEXT NOT NULL, `alternativeAnswers` TEXT NOT NULL, `points` INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS `question_melc_mappings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `examId` INTEGER NOT NULL, `questionNumber` INTEGER NOT NULL, `melcId` INTEGER NOT NULL, FOREIGN KEY(`examId`) REFERENCES `exams`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`melcId`) REFERENCES `melcs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE TABLE IF NOT EXISTS `students` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `studentId` TEXT NOT NULL, `name` TEXT NOT NULL, `sectionId` INTEGER NOT NULL, `examId` INTEGER NOT NULL, `gradeLevel` TEXT NOT NULL, `contactInfo` TEXT NOT NULL, `photoPath` TEXT NOT NULL, `scannedAt` INTEGER NOT NULL, FOREIGN KEY(`sectionId`) REFERENCES `sections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE TABLE IF NOT EXISTS `student_answers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `studentEntityId` INTEGER NOT NULL, `questionNumber` INTEGER NOT NULL, `answer` TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS `student_melc_mastery` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `studentId` INTEGER NOT NULL, `melcId` INTEGER NOT NULL, `masteryLevel` TEXT NOT NULL, `percentage` REAL NOT NULL, `lastUpdated` INTEGER NOT NULL, FOREIGN KEY(`studentId`) REFERENCES `students`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`melcId`) REFERENCES `melcs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
    }

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        // Unencrypted framework factory: validates migration SQL, not SQLCipher (see caveat above).
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate5To6_preservesExistingRows_andCreatesNewTables() {
        // --- Seed a representative v5 database -------------------------------------------
        // Create only the pre-existing tables that we seed with data. MIGRATION_5_6 is purely
        // additive, so it does not touch these tables; creating them alone is sufficient to
        // prove data preservation.
        val seedHelper = openV5SeedHelper(TEST_DB)
        try {
            val db = seedHelper.writableDatabase // triggers onCreate + user_version = 5
            db.execSQL(
                "INSERT INTO subject_folders (id, name, settingsJson, createdAt, isDeleted, deletedAt) " +
                    "VALUES (1, 'Mathematics', '{}', 1000, 0, NULL)"
            )
            db.execSQL(
                "INSERT INTO exams (id, subjectFolderId, sectionId, templateId, name, totalQuestions, qrCode, " +
                    "gradingScale, passingGrade, useNegativeMarking, negativeMarkValue, examDate, timeLimit, " +
                    "allowLateScans, createdAt, isDeleted, deletedAt) " +
                    "VALUES (10, 1, NULL, NULL, 'Quarter 1 Exam', 20, '', 'DEPED_K12', 75, 0, 0.0, NULL, NULL, 1, 2000, 0, NULL)"
            )
            // String gradeLevel is explicitly exercised here.
            db.execSQL(
                "INSERT INTO students (id, studentId, name, sectionId, examId, gradeLevel, contactInfo, photoPath, scannedAt) " +
                    "VALUES (100, 'S-001', 'Juan Dela Cruz', 0, 10, 'Grade 7', '', '', 3000)"
            )
        } finally {
            // Closing flushes the v5 data to disk and releases the file for
            // runMigrationsAndValidate to reopen.
            seedHelper.close()
        }

        // --- Run the real production migration and validate against exported 6.json -------
        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            6,
            /* validateDroppedTables = */ true,
            AppDatabase.MIGRATION_5_6
        )

        // --- Assert pre-existing rows are preserved untouched -----------------------------
        db.query("SELECT name FROM subject_folders WHERE id = 1").use { c ->
            assertTrue("subject_folders row missing after migration", c.moveToFirst())
            assertEquals("Mathematics", c.getString(0))
        }
        db.query("SELECT name, totalQuestions, subjectFolderId FROM exams WHERE id = 10").use { c ->
            assertTrue("exams row missing after migration", c.moveToFirst())
            assertEquals("Quarter 1 Exam", c.getString(0))
            assertEquals(20, c.getInt(1))
            assertEquals(1L, c.getLong(2))
        }
        db.query("SELECT studentId, name, gradeLevel FROM students WHERE id = 100").use { c ->
            assertTrue("students row missing after migration", c.moveToFirst())
            assertEquals("S-001", c.getString(0))
            assertEquals("Juan Dela Cruz", c.getString(1))
            assertEquals("Grade 7", c.getString(2)) // gradeLevel is a String, preserved verbatim
        }

        // --- Assert the four new tables exist and are queryable ---------------------------
        assertTableExists(db, "student_enrollments")
        assertTableExists(db, "melc_coverage")
        assertTableExists(db, "student_notes")
        assertTableExists(db, "sync_logs")

        // Queryable = we can insert into them and read the row back.
        db.execSQL(
            "INSERT INTO student_enrollments (studentId, sectionId, enrolledAt, status) VALUES (100, 5, 4000, 'Active')"
        )
        db.query("SELECT status FROM student_enrollments WHERE studentId = 100 AND sectionId = 5").use { c ->
            assertTrue("student_enrollments not queryable", c.moveToFirst())
            assertEquals("Active", c.getString(0))
        }

        db.execSQL(
            "INSERT INTO melc_coverage (melcId, subjectId, coveredAt, notes, coverageType) VALUES (1, 1, 5000, '', 'Manual')"
        )
        assertRowCount(db, "SELECT COUNT(*) FROM melc_coverage", 1)

        db.execSQL(
            "INSERT INTO student_notes (studentId, note, createdAt, updatedAt) VALUES (100, 'Needs review', 6000, 6000)"
        )
        assertRowCount(db, "SELECT COUNT(*) FROM student_notes", 1)

        db.execSQL(
            "INSERT INTO sync_logs (operation, entityType, entityId, status, message, timestamp) " +
                "VALUES ('upload', 'exam', 10, 'success', '', 7000)"
        )
        assertRowCount(db, "SELECT COUNT(*) FROM sync_logs", 1)
    }

    /**
     * Builds an empty v5-shaped database file (PRAGMA user_version = 5) with just the tables we
     * seed, using a plain framework SQLite helper (NOT MigrationTestHelper.createDatabase, which
     * would require a non-existent 5.json). Shapes are copied verbatim from the exported schema
     * (unchanged v5->v6) so the subsequent MIGRATION_5_6 + validation against 6.json succeeds.
     * The file is created under the instrumentation target context's standard databases dir so
     * runMigrationsAndValidate(TEST_DB, ...) reopens the very same file. Caller owns closing it.
     */
    private fun openV5SeedHelper(name: String): SupportSQLiteOpenHelper {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Start from a clean file so reruns are deterministic.
        context.deleteDatabase(name)

        val callback = object : SupportSQLiteOpenHelper.Callback(5) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                // Create the FULL set of 11 v5 tables. runMigrationsAndValidate validates the
                // migrated DB against the complete v6 schema (all 15 tables), so every pre-existing
                // v5 table must be present before MIGRATION_5_6 adds the four new ones. Statements
                // below are copied verbatim from the exported schema (v5 tables are unchanged in v6).
                V5_TABLE_CREATE_SQL.forEach(db::execSQL)
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                // No-op: seed helper only ever creates the v5 file.
            }
        }

        return FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(callback)
                .build()
        )
    }

    private fun assertTableExists(db: SupportSQLiteDatabase, table: String) {
        db.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf<Any>(table)
        ).use { c ->
            assertTrue("Expected table '$table' to exist after migration", c.moveToFirst())
        }
    }

    private fun assertRowCount(db: SupportSQLiteDatabase, sql: String, expected: Int) {
        db.query(sql).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(expected, c.getInt(0))
        }
    }
}
