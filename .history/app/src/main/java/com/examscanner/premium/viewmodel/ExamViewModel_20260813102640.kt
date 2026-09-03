package com.examscanner.premium.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.examscanner.premium.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ExamUiState(
    val exams: List<ExamWithStats> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ExamDetailUiState(
    val exam: ExamEntity? = null,
    val answerKeys: List<AnswerKeyEntity> = emptyList(),
    val students: List<StudentScore> = emptyList(),
    val isLoading: Boolean = false
)

class ExamViewModel(private val repository: ExamRepository) : ViewModel() {
    
    val examDao = repository.examDao
    
    private val _examState = MutableStateFlow(ExamUiState(isLoading = true))
    val examState: StateFlow<ExamUiState> = _examState.asStateFlow()
    
    private val _detailState = MutableStateFlow(ExamDetailUiState())
    val detailState: StateFlow<ExamDetailUiState> = _detailState.asStateFlow()
    
    // Subject Folders
    val subjectFolders = repository.getAllSubjectFolders()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    init {
        loadExams()
        initializeSampleData()
    }
    
    private fun initializeSampleData() {
        viewModelScope.launch {
            // Initialize sample MELCs if database is empty
            try {
                repository.initializeSampleMelcs()
            } catch (e: Exception) {
                // Already initialized
            }
        }
    }
    
    fun getFolderExams(folderId: Long): Flow<List<ExamEntity>> {
        return repository.getExamsByFolder(folderId)
    }
    
    fun createSubjectFolder(name: String) {
        viewModelScope.launch {
            repository.createSubjectFolder(name)
        }
    }
    
    private fun loadExams() {
        viewModelScope.launch {
            repository.getAllExamsWithStats()
                .catch { e ->
                    _examState.value = ExamUiState(error = e.message, isLoading = false)
                }
                .collect { exams ->
                    _examState.value = ExamUiState(exams = exams, isLoading = false)
                }
        }
    }
    
    fun loadExamDetail(examId: Long) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isLoading = true)
            
            val exam = repository.examDao.getExam(examId)
            
            combine(
                repository.getAnswerKeys(examId),
                repository.getStudents(examId)
            ) { keys, students ->
                val scores = students.map { student ->
                    val score = repository.calculateScore(student, keys)
                    val total = keys.sumOf { it.points }
                    StudentScore(
                        student = student,
                        score = score,
                        total = total,
                        percentage = score
                    )
                }.sortedByDescending { it.percentage }
                
                ExamDetailUiState(
                    exam = exam,
                    answerKeys = keys,
                    students = scores,
                    isLoading = false
                )
            }.collect { state ->
                _detailState.value = state
            }
        }
    }
    
    fun createExam(name: String, totalQuestions: Int) {
        viewModelScope.launch {
            repository.createExam(name, totalQuestions)
        }
    }
    
    fun saveAnswerKey(examId: Long, keys: List<Pair<Int, String>>) {
        viewModelScope.launch {
            repository.saveAnswerKey(examId, keys)
        }
    }
    
    fun saveStudentResults(examId: Long, studentId: String, name: String, answers: List<Pair<Int, String>>) {
        viewModelScope.launch {
            repository.saveStudentResults(examId, studentId, name, answers)
        }
    }
    
    fun resetExam(examId: Long) {
        viewModelScope.launch {
            repository.resetExam(examId)
        }
    }
    
    fun deleteExam(exam: ExamEntity) {
        viewModelScope.launch {
            repository.deleteExam(exam)
        }
    }
}

class ExamViewModelFactory(private val repository: ExamRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExamViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExamViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
