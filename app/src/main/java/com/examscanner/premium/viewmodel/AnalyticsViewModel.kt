package com.examscanner.premium.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.examscanner.premium.analytics.AnalyticsEngine
import com.examscanner.premium.billing.SubscriptionManager
import com.examscanner.premium.data.ExamRepository
import com.examscanner.premium.utils.SecureLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Per-question item-analysis row surfaced to the UI. Combines the difficulty index,
 * discrimination index, and its quality classification for a single question.
 */
data class QuestionAnalyticsRow(
    val questionNumber: Int,
    val difficulty: Float,
    val discrimination: Float,
    val classification: AnalyticsEngine.DiscriminationQuality,
    val isLowQuality: Boolean
)

/**
 * UI state exposed by [AnalyticsViewModel].
 *
 * Basic item statistics (difficulty/discrimination/classification, low-quality flags) are
 * always available. The advanced-analytics outputs — [learningGaps] and [itemResponseCurve]
 * — are gated behind [SubscriptionManager.checkLimit] with
 * [SubscriptionManager.LimitedOperation.AdvancedAnalytics]; when the tier does not permit
 * them, [advancedAnalyticsLocked] is set and an upgrade prompt should be shown.
 */
data class AnalyticsUiState(
    val questionRows: List<QuestionAnalyticsRow> = emptyList(),
    val learningGaps: List<AnalyticsEngine.LearningGap> = emptyList(),
    val itemResponseCurve: AnalyticsEngine.ItemResponseCurve? = null,
    val selectedQuestion: Int? = null,
    val advancedAnalyticsLocked: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * AnalyticsViewModel - exposes [AnalyticsEngine] outputs to the UI as observable state.
 *
 * Responsibilities:
 * - Compute per-question difficulty, discrimination, and quality classification for an exam
 *   (basic item analysis, available to all tiers).
 * - Compute advanced-analytics outputs — learning gaps for a section and the item response
 *   curve for a question — gated behind [SubscriptionManager.checkLimit] with
 *   [SubscriptionManager.LimitedOperation.AdvancedAnalytics].
 *
 * [SubscriptionManager] is a constructor dependency supplied by the DI/UI layer (it needs a
 * BillingClient, so it cannot be constructed here). It is nullable so screens that do not yet
 * wire billing can still render basic analytics; when null, advanced analytics is treated as
 * locked.
 *
 * Requirements: 6.4 (item response curve), 6.6 (analytics visualisation feed), 15.4 / 15.7
 * (advanced-analytics tier gating).
 */
class AnalyticsViewModel(
    private val repository: ExamRepository,
    private val subscriptionManager: SubscriptionManager? = null,
    private val analyticsEngine: AnalyticsEngine = AnalyticsEngine(repository)
) : ViewModel() {

    private val _state = MutableStateFlow(AnalyticsUiState())
    val state: StateFlow<AnalyticsUiState> = _state.asStateFlow()

    /**
     * Loads the basic per-question item analysis (difficulty, discrimination, classification,
     * low-quality flag) for [examId]. Available to all tiers.
     */
    fun loadItemAnalysis(examId: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val flags = analyticsEngine.flagLowQualityQuestions(examId)
                val rows = flags.map { flag ->
                    QuestionAnalyticsRow(
                        questionNumber = flag.questionNumber,
                        difficulty = flag.difficulty,
                        discrimination = flag.discrimination,
                        classification = analyticsEngine.classifyDiscrimination(flag.discrimination),
                        isLowQuality = flag.isLowQuality
                    )
                }
                _state.value = _state.value.copy(
                    questionRows = rows,
                    isLoading = false
                )
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Failed to load item analysis for exam $examId", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load item analysis"
                )
            }
        }
    }

    /**
     * Loads the item response curve for a single question. This is an advanced-analytics
     * feature (Req 6.4) and is gated behind [SubscriptionManager.LimitedOperation.AdvancedAnalytics].
     * When the tier does not permit it, [AnalyticsUiState.advancedAnalyticsLocked] is set and
     * the curve is left null so the UI can present an upgrade prompt.
     */
    fun loadItemResponseCurve(examId: Long, questionNumber: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                error = null,
                selectedQuestion = questionNumber
            )
            try {
                if (!isAdvancedAllowed()) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        advancedAnalyticsLocked = true,
                        itemResponseCurve = null
                    )
                    return@launch
                }
                val curve = analyticsEngine.getItemResponseCurve(examId, questionNumber)
                _state.value = _state.value.copy(
                    itemResponseCurve = curve,
                    advancedAnalyticsLocked = false,
                    isLoading = false
                )
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Failed to load item response curve for exam $examId q$questionNumber", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load item response curve"
                )
            }
        }
    }

    /**
     * Loads the learning gaps (MELCs below 75% mastery) for a section. Advanced-analytics
     * feature gated behind [SubscriptionManager.LimitedOperation.AdvancedAnalytics]; when the
     * tier does not permit it, [AnalyticsUiState.advancedAnalyticsLocked] is set.
     */
    fun loadLearningGaps(sectionId: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                if (!isAdvancedAllowed()) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        advancedAnalyticsLocked = true,
                        learningGaps = emptyList()
                    )
                    return@launch
                }
                val gaps = analyticsEngine.identifyLearningGaps(sectionId)
                _state.value = _state.value.copy(
                    learningGaps = gaps,
                    advancedAnalyticsLocked = false,
                    isLoading = false
                )
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Failed to load learning gaps for section $sectionId", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load learning gaps"
                )
            }
        }
    }

    /**
     * Resolves whether advanced analytics is permitted under the current tier. Delegates to
     * the suspend [SubscriptionManager.checkLimit]; when no manager is wired, advanced
     * analytics is treated as locked.
     */
    private suspend fun isAdvancedAllowed(): Boolean {
        val manager = subscriptionManager ?: return false
        return manager.checkLimit(SubscriptionManager.LimitedOperation.AdvancedAnalytics)
    }

    companion object {
        private const val TAG = "AnalyticsViewModel"
    }
}

/**
 * Factory for [AnalyticsViewModel]; supplies the repository and the optional
 * [SubscriptionManager] from the DI/UI layer.
 */
class AnalyticsViewModelFactory(
    private val repository: ExamRepository,
    private val subscriptionManager: SubscriptionManager? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalyticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AnalyticsViewModel(repository, subscriptionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
