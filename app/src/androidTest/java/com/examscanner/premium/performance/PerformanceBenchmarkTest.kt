package com.examscanner.premium.performance

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.examscanner.premium.data.AppDatabase
import com.examscanner.premium.data.ExamRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

/**
 * PerformanceBenchmarkTest - turns the Requirement 19 / AT-16 runtime targets into a real,
 * device-runnable gate for the DB-bound thresholds that cannot be unit-tested on the JVM.
 *
 * These are INSTRUMENTED (device/emulator) benchmarks: they run via
 * `./gradlew :app:connectedDebugAndroidTest` on a connected device or emulator. The absolute
 * millisecond numbers are device-dependent, so the budgets below are deliberately generous
 * and measure DB query latency against a realistic dataset rather than wall-clock UI time.
 *
 * To keep the benchmark fast, deterministic, and free of SQLCipher setup, it runs against an
 * in-memory Room [AppDatabase] (the standard approach for Room benchmarks) rather than the
 * encrypted production database — encryption is an orthogonal concern verified elsewhere.
 *
 * Covered here (DB-bound, Req 19.2 / 19.7):
 *   - Paginated exam-list load (20 items) completes < 1000 ms (Req 19.2).
 *   - A representative query over the ~100-exam / ~1000-student dataset completes < 500 ms
 *     (Req 19.7).
 *
 * NOT covered here (require a full-app / UI harness, tracked by the manual acceptance
 * checklist, not this micro-benchmark):
 *   - App launch < 3 s (Req 19.1)
 *   - Scan processing < 5 s (Req 19.3)
 *   - Report generation without OOM on large datasets (Req 19.5)
 */
@RunWith(AndroidJUnit4::class)
class PerformanceBenchmarkTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ExamRepository

    // Representative dataset sizes (Req 19.7 targets ~100 exams / ~1000 students).
    private val examCount = 100
    private val studentCount = 1000

    private var folderId: Long = 0L
    private var sectionId: Long = 0L

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        // In-memory DB: no SQLCipher, no file I/O, torn down after each test.
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ExamRepository(db.examDao())

        // Seed: 1 subject folder + 1 section (students FK-reference a section).
        folderId = repository.createSubjectFolder("Benchmark Subject")
        sectionId = repository.createSection(folderId, "Benchmark Section", capacity = studentCount)

        // ~100 exams under the folder.
        repeat(examCount) { i ->
            repository.createExam(
                name = "Exam ${i + 1}",
                totalQuestions = 20,
                folderId = folderId
            )
        }

        // ~1000 students in the section.
        repeat(studentCount) { i ->
            repository.addStudentToSection(
                studentId = "S-${(i + 1).toString().padStart(5, '0')}",
                name = "Student ${i + 1}",
                gradeLevel = "Grade 7",
                contactInfo = "",
                sectionId = sectionId
            )
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    /** Req 19.2: a paginated exam-list page (20 items) must load in under one second. */
    @Test
    fun paginatedExamListLoad_under1000ms() = runBlocking {
        lateinit var page: List<*>
        val elapsed = measureTimeMillis {
            page = repository.getExamsBySubjectPaged(folderId, limit = 20, offset = 0)
        }
        assertTrue(
            "paginated exam list returned ${page.size} items (expected a non-empty page)",
            page.isNotEmpty()
        )
        assertTrue(
            "exam list load took ${elapsed}ms (budget 1000ms)",
            elapsed < 1000
        )
    }

    /**
     * Req 19.7: a representative query over the full ~100-exam / ~1000-student dataset must
     * complete in under 500 ms. Exercises the roster query the UI uses most heavily.
     */
    @Test
    fun representativeQuery_under500ms() = runBlocking {
        lateinit var students: List<*>
        val elapsed = measureTimeMillis {
            students = repository.getStudentsBySection(sectionId)
        }
        assertTrue(
            "roster query returned ${students.size} students (expected $studentCount)",
            students.size == studentCount
        )
        assertTrue(
            "representative query took ${elapsed}ms (budget 500ms)",
            elapsed < 500
        )
    }
}
