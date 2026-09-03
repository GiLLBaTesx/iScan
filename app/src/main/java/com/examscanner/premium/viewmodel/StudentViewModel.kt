package com.examscanner.premium.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.examscanner.premium.analytics.AnalyticsTracker
import com.examscanner.premium.analytics.MasteryCalculator
import com.examscanner.premium.analytics.SessionTracker
import com.examscanner.premium.data.ExamRepository
import com.examscanner.premium.data.MelcEntity
import com.examscanner.premium.data.StudentEntity
import com.examscanner.premium.data.StudentMelcMasteryEntity
import com.examscanner.premium.data.StudentNoteEntity
import com.examscanner.premium.utils.SecureLogger
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * StudentViewModel - Backs the comprehensive student profile screen (Req 8).
 *
 * Exposes, as a single [StudentUiState] StateFlow:
 * - the student profile ([StudentEntity], including `photoPath`),
 * - a performance history (every scanned exam record sharing the student's
 *   school id, with its score/percentage),
 * - the competency mastery summary + matrix ([MasteryCalculator.MasterySummary]
 *   plus each persisted mastery row joined to its [MelcEntity]),
 * - the list of timestamped teacher notes (from `student_notes`).
 *
 * It is a net-new ViewModel (design §10.1) backed by the existing
 * [ExamRepository] and the [MasteryCalculator] domain service - it never
 * duplicates repository logic. Notes and mastery are observed reactively so the
 * profile refreshes when data changes; the profile + performance history are
 * loaded on demand.
 *
 * Follows workspace conventions: [SecureLogger] for logging, analytics via
 * [SessionTracker]/[AnalyticsTracker] for important actions.
 */
class StudentViewModel(
    private val repository: ExamRepository,
    private val masteryCalculator: MasteryCalculator = MasteryCalculator(repository)
) : ViewModel() {

    /** One row of a student's performance history. */
    data class PerformanceRecord(
        val examId: Long,
        val examName: String,
        val score: Int,
        val total: Int,
        val percentage: Int,
        val scannedAt: Long
    )

    /** A mastery record joined to the MELC it belongs to, for the matrix UI. */
    data class MasteryMatrixEntry(
        val mastery: StudentMelcMasteryEntity,
        val melc: MelcEntity?
    )

    data class StudentUiState(
        val profile: StudentEntity? = null,
        val performanceHistory: List<PerformanceRecord> = emptyList(),
        val overallAverage: Float = 0f,
        val masterySummary: MasteryCalculator.MasterySummary? = null,
        val masteryMatrix: List<MasteryMatrixEntry> = emptyList(),
        val notes: List<StudentNoteEntity> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(StudentUiState())
    val uiState: StateFlow<StudentUiState> = _uiState.asStateFlow()

    private var currentStudentId: Long = -1L

    /**
     * Load a student's profile and start observing their notes and mastery.
     *
     * Performance history and the mastery matrix are recomputed each time the
     * observed sources emit, so the profile stays current after new scans,
     * added notes, or a mastery recalculation.
     */
    fun loadStudentProfile(studentId: Long) {
        currentStudentId = studentId
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        SessionTracker.getInstance(currentUserId()).trackScreenEnter("student_profile")

        viewModelScope.launch {
            try {
                val profile = repository.getStudentById(studentId)
                if (profile == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Student not found"
                    )
                    return@launch
                }

                val performanceHistory = loadPerformanceHistory(profile)
                val overallAverage = if (performanceHistory.isNotEmpty()) {
                    performanceHistory.map { it.percentage }.average().toFloat()
                } else {
                    0f
                }

                // Cache all MELCs once so the mastery matrix can resolve details.
                val melcsById = repository.getAllMelcs().first().associateBy { it.id }

                _uiState.value = _uiState.value.copy(
                    profile = profile,
                    performanceHistory = performanceHistory,
                    overallAverage = overallAverage,
                    isLoading = false,
                    error = null
                )

                // Observe notes + mastery reactively and merge into state.
                combine(
                    repository.getStudentNotes(studentId),
                    repository.getStudentMastery(studentId)
                ) { notes, masteryRecords ->
                    val summary = masteryCalculator.getMasterySummary(studentId)
                    val matrix = masteryRecords.map { record ->
                        MasteryMatrixEntry(
                            mastery = record,
                            melc = melcsById[record.melcId]
                        )
                    }
                    Triple(notes, summary, matrix)
                }.catch { e ->
                    SecureLogger.e(TAG, "Observing student profile $studentId failed", e)
                    _uiState.value = _uiState.value.copy(error = e.message)
                }.collect { (notes, summary, matrix) ->
                    _uiState.value = _uiState.value.copy(
                        notes = notes,
                        masterySummary = summary,
                        masteryMatrix = matrix
                    )
                }
            } catch (e: Exception) {
                SecureLogger.e(TAG, "loadStudentProfile failed for $studentId", e)
                AnalyticsTracker.trackError(
                    userId = currentUserId(),
                    errorType = "student_profile_error",
                    errorMessage = e.message ?: "Failed to load student profile",
                    stackTrace = e.stackTraceToString()
                )
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    /**
     * Build the performance history across every scanned record that shares the
     * student's school id. Each exam scan is a separate [StudentEntity] row, so
     * we aggregate them all (falling back to the single anchor record when the
     * school id is blank).
     */
    private suspend fun loadPerformanceHistory(anchor: StudentEntity): List<PerformanceRecord> {
        val records = if (anchor.studentId.isNotBlank()) {
            repository.getAllStudentRecords(anchor.studentId)
        } else {
            listOf(anchor)
        }

        return records
            .filter { it.examId > 0L }
            .map { record ->
                val exam = repository.examDao.getExam(record.examId)
                val answerKeys = repository.getAnswerKeys(record.examId).first()
                val score = repository.calculateScore(record, answerKeys)
                val total = answerKeys.sumOf { it.points }
                PerformanceRecord(
                    examId = record.examId,
                    examName = exam?.name ?: "Unknown Exam",
                    score = score,
                    total = total,
                    percentage = score,
                    scannedAt = record.scannedAt
                )
            }
            .sortedBy { it.scannedAt }
    }

    /**
     * Add a timestamped teacher note (Req 8.5). Enforces the 500-character cap;
     * the observed notes flow refreshes the UI automatically.
     */
    fun addNote(note: String) {
        val studentId = currentStudentId
        val trimmed = note.trim()
        if (studentId <= 0L) return
        if (trimmed.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Note cannot be empty")
            return
        }
        if (trimmed.length > MAX_NOTE_LENGTH) {
            _uiState.value = _uiState.value.copy(
                error = "Note cannot exceed $MAX_NOTE_LENGTH characters"
            )
            return
        }

        viewModelScope.launch {
            try {
                repository.addStudentNote(studentId, trimmed)
                SessionTracker.getInstance(currentUserId()).trackAction(
                    actionName = "student_note_added",
                    metadata = mapOf("student_id" to studentId)
                )
            } catch (e: Exception) {
                SecureLogger.e(TAG, "addNote failed for student $studentId", e)
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    /**
     * Attach or replace the student's profile photo (Req 8.6). The caller is
     * responsible for persisting the JPEG/PNG (≤2 MB) to storage and passing
     * the resulting file path; this only updates the [StudentEntity.photoPath].
     */
    fun attachPhoto(photoPath: String) {
        val profile = _uiState.value.profile ?: return

        viewModelScope.launch {
            try {
                val updated = profile.copy(photoPath = photoPath)
                repository.examDao.updateStudent(updated)
                _uiState.value = _uiState.value.copy(profile = updated)
                SessionTracker.getInstance(currentUserId()).trackAction(
                    actionName = "student_photo_attached",
                    metadata = mapOf("student_id" to profile.id)
                )
            } catch (e: Exception) {
                SecureLogger.e(TAG, "attachPhoto failed for student ${profile.id}", e)
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    /**
     * Update editable profile fields (name, grade level, contact info).
     * Delegates to [ExamRepository.updateStudentInfo] and reflects the change
     * in state.
     */
    fun updateProfile(name: String, gradeLevel: String, contactInfo: String) {
        val profile = _uiState.value.profile ?: return

        viewModelScope.launch {
            try {
                repository.updateStudentInfo(profile, name, gradeLevel, contactInfo)
                _uiState.value = _uiState.value.copy(
                    profile = profile.copy(
                        name = name,
                        gradeLevel = gradeLevel,
                        contactInfo = contactInfo
                    )
                )
                SessionTracker.getInstance(currentUserId()).trackAction(
                    actionName = "student_profile_updated",
                    metadata = mapOf("student_id" to profile.id)
                )
            } catch (e: Exception) {
                SecureLogger.e(TAG, "updateProfile failed for student ${profile.id}", e)
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    /**
     * Recalculate MELC mastery for the loaded student across all their exams
     * (Req 8.3/8.4). The observed mastery flow updates the matrix afterward.
     */
    fun recalculateMastery() {
        val studentId = currentStudentId
        if (studentId <= 0L) return

        viewModelScope.launch {
            try {
                masteryCalculator.updateStudentMastery(studentId)
            } catch (e: Exception) {
                SecureLogger.e(TAG, "recalculateMastery failed for student $studentId", e)
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    /** Clear a transient error after the UI has surfaced it. */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun currentUserId(): String? =
        FirebaseAuth.getInstance().currentUser?.uid

    companion object {
        private const val TAG = "StudentViewModel"
        private const val MAX_NOTE_LENGTH = 500
    }
}

class StudentViewModelFactory(
    private val repository: ExamRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StudentViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
