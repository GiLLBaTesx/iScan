package com.examscanner.premium.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.examscanner.premium.billing.SubscriptionManager
import com.examscanner.premium.data.ExamRepository
import com.examscanner.premium.utils.ReportGenerator
import com.examscanner.premium.utils.SecureLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state exposed by [ReportViewModel].
 *
 * Tracks the report-generation lifecycle: [isGenerating] while a PDF is being produced,
 * [resultPath] with the saved file path (or MediaStore uri) on success, [error] on failure,
 * and [upgradeRequired] when a school-level report was requested but the current tier does
 * not permit it (the UI should present an upgrade prompt instead of generating).
 */
data class ReportUiState(
    val isGenerating: Boolean = false,
    val resultPath: String? = null,
    val error: String? = null,
    val upgradeRequired: Boolean = false
)

/**
 * ReportViewModel - orchestrates [ReportGenerator] for the three report types and exposes
 * the generation lifecycle as observable state.
 *
 * Individual and class-summary reports are available to all tiers. School-level reports are
 * gated behind [SubscriptionManager.checkLimit] with
 * [SubscriptionManager.LimitedOperation.SchoolReport]; when denied, generation is skipped and
 * [ReportUiState.upgradeRequired] is set as an upgrade signal for the UI.
 *
 * [SubscriptionManager] is a constructor dependency supplied by the DI/UI layer (it needs a
 * BillingClient). It is nullable so callers that only produce individual/class reports can
 * omit it; when null, school-level reports are treated as requiring an upgrade.
 *
 * Requirements: 5.1 (report generation), 5.7 (incremental rendering feed / share path),
 * 15.4 / 15.7 (school-report tier gating).
 */
class ReportViewModel(
    private val repository: ExamRepository,
    private val reportGenerator: ReportGenerator,
    private val subscriptionManager: SubscriptionManager? = null
) : ViewModel() {

    private val _state = MutableStateFlow(ReportUiState())
    val state: StateFlow<ReportUiState> = _state.asStateFlow()

    /** Generates an individual-student report for [studentId] on [examId]. */
    fun generateIndividualReport(
        studentId: Long,
        examId: Long,
        schoolName: String = "",
        logoPath: String? = null
    ) {
        generate(
            ReportGenerator.ReportType.IndividualStudent(studentId, examId),
            schoolName,
            logoPath
        )
    }

    /** Generates a class-summary report for [examId] (optionally scoped to [sectionId]). */
    fun generateClassReport(
        examId: Long,
        sectionId: Long = 0L,
        schoolName: String = "",
        logoPath: String? = null
    ) {
        generate(
            ReportGenerator.ReportType.ClassSummary(examId, sectionId),
            schoolName,
            logoPath
        )
    }

    /**
     * Generates a school-level report. Gated behind
     * [SubscriptionManager.LimitedOperation.SchoolReport]; when the tier does not permit it,
     * generation is skipped and [ReportUiState.upgradeRequired] is set.
     */
    fun generateSchoolReport(
        subjectId: Long,
        quarter: Int,
        schoolYear: String,
        schoolName: String = "",
        logoPath: String? = null
    ) {
        viewModelScope.launch {
            _state.value = ReportUiState(isGenerating = true)
            if (!isSchoolReportAllowed()) {
                _state.value = ReportUiState(upgradeRequired = true)
                return@launch
            }
            runGeneration(
                ReportGenerator.ReportType.SchoolLevel(subjectId, quarter, schoolYear),
                schoolName,
                logoPath
            )
        }
    }

    /** Clears a transient result/error/upgrade signal once the UI has consumed it. */
    fun consumeResult() {
        _state.value = ReportUiState()
    }

    private fun generate(
        type: ReportGenerator.ReportType,
        schoolName: String,
        logoPath: String?
    ) {
        viewModelScope.launch {
            _state.value = ReportUiState(isGenerating = true)
            runGeneration(type, schoolName, logoPath)
        }
    }

    private suspend fun runGeneration(
        type: ReportGenerator.ReportType,
        schoolName: String,
        logoPath: String?
    ) {
        try {
            val path = reportGenerator.generateReport(type, schoolName, logoPath)
            _state.value = ReportUiState(resultPath = path)
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Failed to generate report ${type::class.java.simpleName}", e)
            _state.value = ReportUiState(error = e.message ?: "Failed to generate report")
        }
    }

    /**
     * Resolves whether a school-level report is permitted under the current tier. Delegates to
     * the suspend [SubscriptionManager.checkLimit]; when no manager is wired, it is treated as
     * requiring an upgrade.
     */
    private suspend fun isSchoolReportAllowed(): Boolean {
        val manager = subscriptionManager ?: return false
        return manager.checkLimit(SubscriptionManager.LimitedOperation.SchoolReport)
    }

    companion object {
        private const val TAG = "ReportViewModel"
    }
}

/**
 * Factory for [ReportViewModel]; constructs the [ReportGenerator] from [context] +
 * [repository] and supplies the optional [SubscriptionManager] from the DI/UI layer.
 */
class ReportViewModelFactory(
    private val context: Context,
    private val repository: ExamRepository,
    private val subscriptionManager: SubscriptionManager? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReportViewModel(
                repository = repository,
                reportGenerator = ReportGenerator(context.applicationContext, repository),
                subscriptionManager = subscriptionManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
