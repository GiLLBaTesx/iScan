package com.examscanner.premium.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.examscanner.premium.analytics.AnalyticsEngine
import com.examscanner.premium.analytics.PacingEngine
import com.examscanner.premium.data.ExamEntity
import com.examscanner.premium.data.ExamRepository
import com.examscanner.premium.data.MelcEntity
import com.examscanner.premium.data.StudentEntity
import com.examscanner.premium.data.StudentMelcMasteryEntity
import com.examscanner.premium.utils.SecureLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * DashboardViewModel - Aggregates the Teacher dashboard (Requirement 12).
 *
 * Backs `SmartDashboardMVP` (wired in Task 11.4) with:
 *  - at-a-glance [DashboardStats] (total exams / students / assessments completed
 *    / current quarter) (Req 12.2);
 *  - [PerformanceTrends] over the selected [DateRange], bucketed at the interval
 *    granularity defined by [intervalFor] (daily for This Week, weekly for This
 *    Month and This Quarter, monthly for This Year) (Req 12.3, 12.6);
 *  - [ActionItems]: learning gaps (via [AnalyticsEngine.identifyLearningGaps]),
 *    behind-schedule MELCs (via [PacingEngine.getPacingGuide]), and flagged
 *    questions needing review (via [AnalyticsEngine.flagLowQualityQuestions])
 *    (Req 12.4);
 *  - [RecentActivity]: last 5 scanned sheets, recently created exams, recently
 *    added students (Req 12.5).
 *
 * State is exposed as a single [DashboardUiState] via [StateFlow]. [setDateRange]
 * recomputes the trend buckets for the newly selected range, and [refresh]
 * re-aggregates everything after underlying data changes — after scanning,
 * grading, or data operations (Req 12.7).
 *
 * Follows workspace conventions: reads flow through the existing [ExamRepository]
 * and the [AnalyticsEngine]/[PacingEngine] domain services rather than
 * duplicating logic; failures are logged with [SecureLogger].
 */
class DashboardViewModel(
    private val repository: ExamRepository,
    private val analyticsEngine: AnalyticsEngine = AnalyticsEngine(repository),
    private val pacingEngine: PacingEngine = PacingEngine(repository)
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(isLoading = true))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /**
     * Change the trend date-range filter and recompute the trend buckets at the
     * granularity mapped by [intervalFor]. Stats, action items, and recent
     * activity are range-independent, so only trends are recomputed. (Req 12.6)
     */
    fun setDateRange(range: DateRange) {
        if (range == _uiState.value.selectedRange) return
        _uiState.value = _uiState.value.copy(selectedRange = range)
        viewModelScope.launch {
            try {
                val trends = withContext(Dispatchers.Default) { computeTrends(range) }
                _uiState.value = _uiState.value.copy(trends = trends, error = null)
            } catch (e: Exception) {
                SecureLogger.e(TAG, "setDateRange failed for $range", e)
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    /**
     * Re-aggregate the entire dashboard. Call after scanning, grading, or any
     * data operation so statistics reflect the underlying data. (Req 12.7)
     */
    fun refresh() {
        val range = _uiState.value.selectedRange
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val exams = repository.examDao.getAllExams().first()
                val masteryRecords = withContext(Dispatchers.Default) { loadAllMastery(exams) }

                val stats = withContext(Dispatchers.Default) { computeStats(exams, masteryRecords) }
                val trends = withContext(Dispatchers.Default) { computeTrends(range, exams, masteryRecords) }
                val actionItems = computeActionItems(exams)
                val recentActivity = withContext(Dispatchers.Default) { computeRecentActivity(exams) }

                _uiState.value = DashboardUiState(
                    stats = stats,
                    trends = trends,
                    actionItems = actionItems,
                    recentActivity = recentActivity,
                    selectedRange = range,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Dashboard refresh failed", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    // ---- Stats (Req 12.2) ----

    private suspend fun computeStats(
        exams: List<ExamEntity>,
        masteryRecords: List<StudentMelcMasteryEntity>
    ): DashboardStats {
        val students = allStudents(exams)
        // An "assessment completed" is an exam that has at least one scanned
        // student sheet (a graded result exists for it).
        val assessmentsCompleted = exams.count { exam ->
            repository.getStudents(exam.id).first().isNotEmpty()
        }
        return DashboardStats(
            totalExams = exams.size,
            totalStudents = students.size,
            assessmentsCompleted = assessmentsCompleted,
            currentQuarter = currentQuarter()
        )
    }

    // ---- Trends (Req 12.3, 12.6) ----

    private suspend fun computeTrends(range: DateRange): PerformanceTrends {
        val exams = repository.examDao.getAllExams().first()
        return computeTrends(range, exams, loadAllMastery(exams))
    }

    private suspend fun computeTrends(
        range: DateRange,
        exams: List<ExamEntity>,
        masteryRecords: List<StudentMelcMasteryEntity>
    ): PerformanceTrends {
        val interval = intervalFor(range)
        val now = System.currentTimeMillis()
        val windowStart = rangeStart(range, now)

        val students = allStudents(exams).filter { it.scannedAt in windowStart..now }

        // Average score per interval: score each scanned sheet and average per bucket.
        val scoreByBucket = linkedMapOf<String, MutableList<Int>>()
        // Assessment frequency per interval: count scanned sheets per bucket.
        val frequencyByBucket = linkedMapOf<String, Int>()

        for (student in students) {
            val label = bucketLabel(student.scannedAt, interval)
            val keys = repository.getAnswerKeys(student.examId).first()
            if (keys.isNotEmpty()) {
                val score = repository.calculateScore(student, keys)
                scoreByBucket.getOrPut(label) { mutableListOf() }.add(score)
            }
            frequencyByBucket[label] = (frequencyByBucket[label] ?: 0) + 1
        }

        val averageScorePerInterval = scoreByBucket.map { (label, scores) ->
            TrendPoint(label = label, value = scores.average().toFloat())
        }
        val assessmentFrequencyPerInterval = frequencyByBucket.map { (label, count) ->
            TrendPoint(label = label, value = count.toFloat())
        }

        // Competency mastery distribution across all recorded mastery rows.
        val masteryDistribution = masteryRecords
            .groupingBy { it.masteryLevel }
            .eachCount()

        return PerformanceTrends(
            interval = interval,
            averageScorePerInterval = averageScorePerInterval,
            assessmentFrequencyPerInterval = assessmentFrequencyPerInterval,
            masteryDistribution = masteryDistribution
        )
    }

    // ---- Action items (Req 12.4) ----

    private suspend fun computeActionItems(exams: List<ExamEntity>): ActionItems {
        // Learning gaps across every section referenced by an exam.
        val sectionIds = exams.mapNotNull { it.sectionId }.filter { it != 0L }.distinct()
        val learningGaps = sectionIds.flatMap { sectionId ->
            try {
                analyticsEngine.identifyLearningGaps(sectionId)
            } catch (e: Exception) {
                SecureLogger.e(TAG, "identifyLearningGaps failed for section $sectionId", e)
                emptyList()
            }
        }.sortedBy { it.averageMastery }

        // Behind-schedule MELCs across the current quarter for every (subject,
        // gradeLevel) pair present in the seeded MELC set.
        val quarter = currentQuarter()
        val currentWeek = currentWeekInQuarter()
        val behindSchedule = try {
            val melcs = repository.getAllMelcs().first()
            melcs.map { it.subject to it.gradeLevel }.distinct()
                .flatMap { (subject, grade) ->
                    pacingEngine.getPacingGuide(subject, grade, quarter, currentWeek).behindSchedule
                }
                .distinctBy { it.id }
        } catch (e: Exception) {
            SecureLogger.e(TAG, "behind-schedule detection failed", e)
            emptyList()
        }

        // Flagged questions needing review, per exam.
        val flaggedQuestions = exams.flatMap { exam ->
            try {
                analyticsEngine.flagLowQualityQuestions(exam.id)
                    .filter { it.isLowQuality }
                    .map { FlaggedQuestion(examId = exam.id, examName = exam.name, flag = it) }
            } catch (e: Exception) {
                SecureLogger.e(TAG, "flagLowQualityQuestions failed for exam ${exam.id}", e)
                emptyList()
            }
        }

        return ActionItems(
            learningGaps = learningGaps,
            behindScheduleMelcs = behindSchedule,
            flaggedQuestions = flaggedQuestions
        )
    }

    // ---- Recent activity (Req 12.5) ----

    private suspend fun computeRecentActivity(exams: List<ExamEntity>): RecentActivity {
        val students = allStudents(exams)
        val lastScannedSheets = students
            .sortedByDescending { it.scannedAt }
            .take(RECENT_LIMIT)
        val recentExams = exams
            .sortedByDescending { it.createdAt }
            .take(RECENT_LIMIT)
        val recentStudents = students
            .sortedByDescending { it.scannedAt }
            .take(RECENT_LIMIT)

        return RecentActivity(
            lastScannedSheets = lastScannedSheets,
            recentExams = recentExams,
            recentStudents = recentStudents
        )
    }

    // ---- Helpers ----

    private suspend fun allStudents(exams: List<ExamEntity>): List<StudentEntity> {
        return exams.flatMap { repository.getStudents(it.id).first() }
    }

    private suspend fun loadAllMastery(exams: List<ExamEntity>): List<StudentMelcMasteryEntity> {
        return allStudents(exams).flatMap { repository.getStudentMastery(it.id).first() }
    }

    companion object {
        private const val TAG = "DashboardViewModel"

        /** Recent-activity list size (Req 12.5: "last 5"). */
        const val RECENT_LIMIT = 5

        /**
         * PURE, testable mapping from a dashboard [DateRange] filter to the
         * trend [Interval] granularity (Requirement 12.3 / 12.6). This is the
         * single source of truth exercised by Task 10.5's unit test.
         *
         *  - THIS_WEEK    -> DAILY
         *  - THIS_MONTH   -> WEEKLY
         *  - THIS_QUARTER -> WEEKLY
         *  - THIS_YEAR    -> MONTHLY
         */
        fun intervalFor(range: DateRange): Interval = when (range) {
            DateRange.THIS_WEEK -> Interval.DAILY
            DateRange.THIS_MONTH -> Interval.WEEKLY
            DateRange.THIS_QUARTER -> Interval.WEEKLY
            DateRange.THIS_YEAR -> Interval.MONTHLY
        }

        /** DepEd quarter (1-4) for the given month index (0-based, Jan = 0). */
        fun quarterForMonth(monthZeroBased: Int): Int {
            return (monthZeroBased / 3) + 1
        }

        private fun currentQuarter(): Int =
            quarterForMonth(Calendar.getInstance().get(Calendar.MONTH))

        private fun currentWeekInQuarter(): Int {
            val cal = Calendar.getInstance()
            val monthInQuarter = cal.get(Calendar.MONTH) % 3          // 0..2
            val weekOfMonth = cal.get(Calendar.WEEK_OF_MONTH)          // ~1..5
            return (monthInQuarter * 4 + weekOfMonth)
                .coerceIn(1, PacingEngine.WEEKS_PER_QUARTER)
        }

        /** Inclusive start-of-window timestamp for a date range, relative to [now]. */
        private fun rangeStart(range: DateRange, now: Long): Long {
            val cal = Calendar.getInstance().apply { timeInMillis = now }
            when (range) {
                DateRange.THIS_WEEK -> cal.add(Calendar.DAY_OF_YEAR, -7)
                DateRange.THIS_MONTH -> cal.add(Calendar.MONTH, -1)
                DateRange.THIS_QUARTER -> cal.add(Calendar.MONTH, -3)
                DateRange.THIS_YEAR -> cal.add(Calendar.YEAR, -1)
            }
            return cal.timeInMillis
        }

        /** Bucket label for a timestamp at the given interval granularity. */
        private fun bucketLabel(timestamp: Long, interval: Interval): String {
            val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
            return when (interval) {
                Interval.DAILY ->
                    "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
                Interval.WEEKLY ->
                    "${cal.get(Calendar.YEAR)}-W${cal.get(Calendar.WEEK_OF_YEAR)}"
                Interval.MONTHLY ->
                    "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}"
            }
        }
    }
}

/** Dashboard trend date-range filter (Req 12.6). */
enum class DateRange {
    THIS_WEEK,
    THIS_MONTH,
    THIS_QUARTER,
    THIS_YEAR
}

/** Trend bucketing granularity (Req 12.3). */
enum class Interval {
    DAILY,
    WEEKLY,
    MONTHLY
}

/** At-a-glance dashboard statistics (Req 12.2). */
data class DashboardStats(
    val totalExams: Int = 0,
    val totalStudents: Int = 0,
    val assessmentsCompleted: Int = 0,
    val currentQuarter: Int = 1
)

/** A single point in a trend series (bucket label + value). */
data class TrendPoint(
    val label: String,
    val value: Float
)

/** Performance trends over the selected range at [interval] granularity (Req 12.3). */
data class PerformanceTrends(
    val interval: Interval = Interval.DAILY,
    val averageScorePerInterval: List<TrendPoint> = emptyList(),
    val assessmentFrequencyPerInterval: List<TrendPoint> = emptyList(),
    val masteryDistribution: Map<String, Int> = emptyMap()
)

/** A flagged low-quality question needing review (Req 12.4). */
data class FlaggedQuestion(
    val examId: Long,
    val examName: String,
    val flag: AnalyticsEngine.QuestionQualityFlag
)

/** Actionable dashboard items requiring teacher attention (Req 12.4). */
data class ActionItems(
    val learningGaps: List<AnalyticsEngine.LearningGap> = emptyList(),
    val behindScheduleMelcs: List<MelcEntity> = emptyList(),
    val flaggedQuestions: List<FlaggedQuestion> = emptyList()
)

/** Recent activity across the app (Req 12.5). */
data class RecentActivity(
    val lastScannedSheets: List<StudentEntity> = emptyList(),
    val recentExams: List<ExamEntity> = emptyList(),
    val recentStudents: List<StudentEntity> = emptyList()
)

/** Aggregated dashboard UI state exposed via StateFlow. */
data class DashboardUiState(
    val stats: DashboardStats = DashboardStats(),
    val trends: PerformanceTrends = PerformanceTrends(),
    val actionItems: ActionItems = ActionItems(),
    val recentActivity: RecentActivity = RecentActivity(),
    val selectedRange: DateRange = DateRange.THIS_WEEK,
    val isLoading: Boolean = false,
    val error: String? = null
)

/** Factory mirroring [ExamViewModelFactory] for manual instantiation. */
class DashboardViewModelFactory(
    private val repository: ExamRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
