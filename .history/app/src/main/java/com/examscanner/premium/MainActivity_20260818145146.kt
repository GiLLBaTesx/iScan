package com.examscanner.premium

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.examscanner.premium.auth.AuthState
import com.examscanner.premium.auth.AuthViewModel
import com.examscanner.premium.data.AppDatabase
import com.examscanner.premium.data.ExamRepository
import com.examscanner.premium.data.ExamWithStats
import com.examscanner.premium.scanner.BubbleSheetProcessor
import com.examscanner.premium.scanner.CameraScreen
import com.examscanner.premium.ui.screens.*
import com.examscanner.premium.ui.screens.auth.*
import com.examscanner.premium.ui.theme.ExamScannerTheme
import com.examscanner.premium.utils.ExportUtility
import com.examscanner.premium.viewmodel.ExamViewModel
import com.examscanner.premium.viewmodel.ExamViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExamScannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ExamScannerApp()
                }
            }
        }
    }
}

@Composable
fun ExamScannerApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember { ExamRepository(database.examDao()) }
    val viewModel: ExamViewModel = viewModel(
        factory = ExamViewModelFactory(repository)
    )
    
    val examState by viewModel.examState.collectAsState()
    val detailState by viewModel.detailState.collectAsState()
    val scope = rememberCoroutineScope()
    
    var currentExamId by remember { mutableStateOf<Long?>(null) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // Streamlined navigation - bottom tabs for efficiency
    NavHost(navController = navController, startDestination = "main") {
        // Main screen with bottom navigation
        composable("main") {
            MainScreenWithTabs(
                viewModel = viewModel,
                onExamClick = { exam ->
                    currentExamId = exam.id
                    viewModel.loadExamDetail(exam.id)
                    navController.navigate("exam_detail/${exam.id}") {
                        launchSingleTop = true
                    }
                },
                onNewExamClick = {
                    navController.navigate("new_exam")
                }
            )
        }
        
        // New Exam Screen (simplified - no folder required)
        composable("new_exam") {
            NewExamScreen(
                onBack = { navController.popBackStack() },
                onCreate = { name, questions, subject, section ->
                    scope.launch {
                        // Create a subject folder if it doesn't exist
                        val folderId = if (subject.isNotEmpty()) {
                            val existingFolder = repository.examDao.getAllSubjectFolders()
                                .first()
                                .find { it.name.equals(subject, ignoreCase = true) }
                            
                            existingFolder?.id ?: repository.examDao.insertSubjectFolder(
                                com.examscanner.premium.data.SubjectFolderEntity(name = subject)
                            )
                        } else {
                            0L
                        }
                        
                        val examId = repository.createExam(name, questions, folderId, subject, section)
                        Toast.makeText(context, "Exam created!", Toast.LENGTH_SHORT).show()
                        navController.navigate("edit_key/$examId/$name/$questions") {
                            popUpTo("main")
                        }
                    }
                }
            )
        }
                onCreate = { name, questions, subject, section ->
                    scope.launch {
                        val examId = repository.createExam(name, questions, folderId, subject, section)
                        Toast.makeText(context, "Exam created!", Toast.LENGTH_SHORT).show()
                        navController.navigate("edit_key/$examId/$name/$questions") {
                            popUpTo("exam_list")
                        }
                    }
                }
            )
        }
        
        // Exam Detail Screen
        composable(
            route = "exam_detail/{examId}",
            arguments = listOf(navArgument("examId") { type = NavType.LongType })
        ) { backStackEntry ->
            val examId = backStackEntry.arguments?.getLong("examId") ?: return@composable
            
            detailState.exam?.let { exam ->
                ExamDetailScreen(
                    exam = exam,
                    answerKeys = detailState.answerKeys,
                    students = detailState.students,
                    onBack = { navController.popBackStack() },
                    onScanClick = {
                        currentExamId = examId
                        navController.navigate("camera")
                    },
                    onEditKeyClick = {
                        val answers = detailState.answerKeys.associate { 
                            it.questionNumber to it.correctAnswer 
                        }
                        navController.navigate("edit_key/$examId/${exam.name}/${exam.totalQuestions}?answers=${answers}")
                    },
                    onResetClick = {
                        viewModel.resetExam(examId)
                        Toast.makeText(context, "Exam reset", Toast.LENGTH_SHORT).show()
                    },
                    onExportClick = {
                        scope.launch {
                            try {
                                val file = ExportUtility.exportExamToCSV(
                                    context = context,
                                    exam = exam,
                                    students = detailState.students,
                                    answerKeys = detailState.answerKeys
                                )
                                ExportUtility.shareFile(context, file)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onEditExam = { newName ->
                        viewModel.updateExam(examId, newName)
                        viewModel.loadExamDetail(examId) // Reload to show new name
                        Toast.makeText(context, "Exam renamed", Toast.LENGTH_SHORT).show()
                    },
                    onDeleteExam = {
                        viewModel.deleteExam(exam)
                        Toast.makeText(context, "Exam deleted", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                )
            }
        }
        
        // Edit Key Screen
        composable(
            route = "edit_key/{examId}/{examName}/{totalQuestions}",
            arguments = listOf(
                navArgument("examId") { type = NavType.LongType },
                navArgument("examName") { type = NavType.StringType },
                navArgument("totalQuestions") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val examId = backStackEntry.arguments?.getLong("examId") ?: return@composable
            val examName = backStackEntry.arguments?.getString("examName") ?: ""
            val totalQuestions = backStackEntry.arguments?.getInt("totalQuestions") ?: 20
            
            val currentAnswers = detailState.answerKeys.associate { 
                it.questionNumber to it.correctAnswer 
            }
            
            EditKeyScreen(
                examId = examId,
                examName = examName,
                totalQuestions = totalQuestions,
                currentAnswers = currentAnswers,
                onBack = { navController.popBackStack() },
                onSave = { answers ->
                    val keysList = answers.map { (question, answer) -> 
                        question to answer 
                    }
                    viewModel.saveAnswerKey(examId, keysList)
                    Toast.makeText(context, "Answer key saved!", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }
            )
        }
        
        // Camera Screen
        composable("camera") {
            CameraScreen(
                onBack = { navController.popBackStack() },
                onImageCaptured = { uri ->
                    capturedImageUri = uri
                    navController.navigate("processing")
                }
            )
        }
        
        // Processing Screen
        composable("processing") {
            ProcessingScreen(
                imageUri = capturedImageUri,
                onProcessingComplete = { result ->
                    currentExamId?.let { examId ->
                        viewModel.saveStudentResults(
                            examId = examId,
                            studentId = result.studentId,
                            name = result.studentName,
                            answers = result.answers
                        )
                        Toast.makeText(context, "Sheet processed!", Toast.LENGTH_SHORT).show()
                        navController.popBackStack("exam_detail/$examId", false)
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }
    }
}
