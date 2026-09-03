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
import java.io.File

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
    
    // Authentication disabled for testing - will enable after Firebase setup
    // Start with subject folders (organized approach)
    NavHost(navController = navController, startDestination = "subject_folders") {
        // Subject Folders Screen (Home)
        composable("subject_folders") {
            val folders by viewModel.subjectFolders.collectAsState(initial = emptyList())
            
            SubjectFolderListScreen(
                folders = folders,
                onFolderClick = { folder ->
                    navController.navigate("folder_detail/${folder.id}/${folder.name}")
                },
                onNewFolderClick = {
                    // Handled by dialog in SubjectFolderListScreen
                },
                onSettingsClick = {
                    navController.navigate("settings")
                },
                viewModel = viewModel
            )
        }
        
        // Settings Screen
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onCreateTemplate = {
                    navController.navigate("template_generator")
                },
                onBackupData = {
                    scope.launch {
                        try {
                            // Export database file
                            val dbFile = context.getDatabasePath("exam_scanner_database")
                            if (dbFile.exists()) {
                                val outputDir = File(context.getExternalFilesDir(null), "backups")
                                outputDir.mkdirs()
                                
                                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                                val backupFile = File(outputDir, "backup_$timestamp.db")
                                
                                dbFile.copyTo(backupFile, overwrite = true)
                                
                                // Share the backup file
                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    backupFile
                                )
                                
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "application/octet-stream"
                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                
                                context.startActivity(android.content.Intent.createChooser(intent, "Backup Database"))
                                Toast.makeText(context, "Database backed up successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Database not found", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onRestoreData = {
                    Toast.makeText(context, "Restore: Please select a backup file from your device", Toast.LENGTH_LONG).show()
                    // TODO: Implement file picker for restore
                },
                onClearData = {
                    scope.launch {
                        try {
                            repository.clearAllData()
                            Toast.makeText(context, "All data cleared successfully", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Clear data failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
        
        // Template Generator Screen
        composable("template_generator") {
            TemplateGeneratorScreen(
                onBack = { navController.popBackStack() },
                onGenerate = { totalQuestions, choicesPerQuestion, templateName ->
                    scope.launch {
                        try {
                            val file = com.examscanner.premium.utils.TemplatePDFGenerator.generateTemplate(
                                context = context,
                                templateName = templateName,
                                totalQuestions = totalQuestions,
                                choicesPerQuestion = choicesPerQuestion
                            )
                            
                            // Share the generated PDF
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                file
                            )
                            
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        android.content.Intent.FLAG_ACTIVITY_NO_HISTORY
                            }
                            
                            try {
                                context.startActivity(intent)
                                Toast.makeText(context, "Template generated successfully!", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                // If no PDF viewer, show share dialog
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Open or Share Template"))
                                Toast.makeText(context, "Template saved: ${file.name}", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to generate template: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }
        
        // Folder Detail Screen (Shows exams in folder)
        composable(
            route = "folder_detail/{folderId}/{folderName}",
            arguments = listOf(
                navArgument("folderId") { type = NavType.LongType },
                navArgument("folderName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getLong("folderId") ?: return@composable
            val folderName = backStackEntry.arguments?.getString("folderName") ?: ""
            
            val exams by viewModel.getFolderExams(folderId).collectAsState(initial = emptyList())
            
            FolderExamListScreen(
                folderName = folderName,
                exams = exams.map { exam -> 
                    ExamWithStats(exam, 0, 0, null) 
                },
                onBack = { navController.popBackStack() },
                onExamClick = { examWithStats ->
                    currentExamId = examWithStats.exam.id
                    viewModel.loadExamDetail(examWithStats.exam.id)
                    navController.navigate("exam_detail/${examWithStats.exam.id}") {
                        launchSingleTop = true
                    }
                },
                onNewExamClick = {
                    navController.navigate("new_exam/$folderId")
                },
                onEditExam = { examWithStats, newName ->
                    viewModel.updateExam(examWithStats.exam.id, newName)
                    Toast.makeText(context, "Exam renamed", Toast.LENGTH_SHORT).show()
                },
                onDeleteExam = { examWithStats ->
                    viewModel.deleteExam(examWithStats.exam)
                    Toast.makeText(context, "Exam deleted", Toast.LENGTH_SHORT).show()
                },
                onManageSections = {
                    navController.navigate("sections/$folderId/$folderName")
                }
            )
        }
        
        // Section Management Screen
        composable(
            route = "sections/{folderId}/{folderName}",
            arguments = listOf(
                navArgument("folderId") { type = NavType.LongType },
                navArgument("folderName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getLong("folderId") ?: return@composable
            val folderName = backStackEntry.arguments?.getString("folderName") ?: ""
            
            val sections by viewModel.getSections(folderId).collectAsState(initial = emptyList())
            
            SectionManagementScreen(
                folderId = folderId,
                folderName = folderName,
                sections = sections,
                onBack = { navController.popBackStack() },
                onSectionClick = { section ->
                    navController.navigate("student_roster/${section.id}/${section.name}")
                },
                onAddSection = { name, capacity ->
                    viewModel.createSection(folderId, name, capacity)
                    Toast.makeText(context, "Section created", Toast.LENGTH_SHORT).show()
                },
                onEditSection = { section, name, capacity ->
                    viewModel.updateSection(section, name, capacity)
                    Toast.makeText(context, "Section updated", Toast.LENGTH_SHORT).show()
                },
                onDeleteSection = { section ->
                    viewModel.deleteSection(section)
                    Toast.makeText(context, "Section deleted", Toast.LENGTH_SHORT).show()
                }
            )
        }
        
        // Student Roster Screen
        composable(
            route = "student_roster/{sectionId}/{sectionName}",
            arguments = listOf(
                navArgument("sectionId") { type = NavType.LongType },
                navArgument("sectionName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sectionId = backStackEntry.arguments?.getLong("sectionId") ?: return@composable
            val sectionName = backStackEntry.arguments?.getString("sectionName") ?: ""
            
            val students by viewModel.getStudentsBySection(sectionId).collectAsState(initial = emptyList())
            
            StudentRosterScreen(
                sectionId = sectionId,
                sectionName = sectionName,
                students = students,
                onBack = { navController.popBackStack() },
                onAddStudent = { studentId, name, gradeLevel, contactInfo ->
                    viewModel.addStudentToSection(studentId, name, gradeLevel, contactInfo, sectionId)
                    Toast.makeText(context, "Student added", Toast.LENGTH_SHORT).show()
                },
                onEditStudent = { student, name, gradeLevel, contactInfo ->
                    viewModel.updateStudentInfo(student, name, gradeLevel, contactInfo)
                    Toast.makeText(context, "Student updated", Toast.LENGTH_SHORT).show()
                },
                onDeleteStudent = { student ->
                    viewModel.deleteStudentFromRoster(student)
                    Toast.makeText(context, "Student removed", Toast.LENGTH_SHORT).show()
                },
                onImportCSV = { uri ->
                    scope.launch {
                        try {
                            val result = com.examscanner.premium.utils.CSVImportUtility.importStudentsFromCSV(
                                context, uri, sectionId
                            )
                            
                            if (result.students.isNotEmpty()) {
                                viewModel.bulkImportStudents(result.students)
                            }
                            
                            val message = buildString {
                                append("Import complete: ")
                                append("${result.successCount} added")
                                if (result.failedCount > 0) {
                                    append(", ${result.failedCount} failed")
                                }
                            }
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            
                            if (result.errors.isNotEmpty()) {
                                // Show first few errors
                                val errorMsg = result.errors.take(3).joinToString("\n")
                                Toast.makeText(context, "Errors:\n$errorMsg", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
        
        // New Exam Screen
        composable(
            route = "new_exam/{folderId}",
            arguments = listOf(navArgument("folderId") { 
                type = NavType.LongType
                defaultValue = 0L
            })
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getLong("folderId") ?: 0L
            
            NewExamScreen(
                onBack = { navController.popBackStack() },
                onCreate = { name, questions ->
                    scope.launch {
                        val examId = repository.createExam(name, questions, folderId)
                        Toast.makeText(context, "Exam created!", Toast.LENGTH_SHORT).show()
                        navController.navigate("edit_key/$examId/$name/$questions") {
                            popUpTo("subject_folders")
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
                    studentAnswers = detailState.studentAnswers,
                    questionMelcMappings = detailState.questionMelcMappings,
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
                    onMapMelcs = {
                        navController.navigate("map_melcs/$examId")
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
            
            val allMelcs by viewModel.getAllMelcs().collectAsState(initial = emptyList())
            var currentMelcMappings by remember { mutableStateOf<Map<Int, com.examscanner.premium.data.MelcEntity>>(emptyMap()) }
            
            LaunchedEffect(examId) {
                currentMelcMappings = viewModel.getQuestionMelcMappings(examId)
            }
            
            EditKeyScreen(
                examId = examId,
                examName = examName,
                totalQuestions = totalQuestions,
                currentAnswers = currentAnswers,
                availableMelcs = allMelcs,
                currentMelcMappings = currentMelcMappings,
                onBack = { navController.popBackStack() },
                onSave = { answers ->
                    val keysList = answers.map { (question, answer) -> 
                        question to answer 
                    }
                    viewModel.saveAnswerKey(examId, keysList)
                    Toast.makeText(context, "Answer key saved!", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                },
                onSaveMelcMappings = { mappings ->
                    viewModel.saveQuestionMelcMappings(examId, mappings)
                    Toast.makeText(context, "Answer key and MELCs saved!", Toast.LENGTH_SHORT).show()
                }
            )
        }
        
        // Map Questions to MELCs Screen
        composable(
            route = "map_melcs/{examId}",
            arguments = listOf(navArgument("examId") { type = NavType.LongType })
        ) { backStackEntry ->
            val examId = backStackEntry.arguments?.getLong("examId") ?: return@composable
            
            val allMelcs by viewModel.getAllMelcs().collectAsState(initial = emptyList())
            var existingMappings by remember { mutableStateOf<Map<Int, com.examscanner.premium.data.MelcEntity>>(emptyMap()) }
            
            LaunchedEffect(examId) {
                viewModel.loadExamDetail(examId)
                existingMappings = viewModel.getQuestionMelcMappings(examId)
            }
            
            detailState.exam?.let { exam ->
                MapQuestionsToMelcScreen(
                    examId = examId,
                    totalQuestions = exam.totalQuestions,
                    availableMelcs = allMelcs,
                    existingMappings = existingMappings,
                    onBack = { navController.popBackStack() },
                    onSaveMappings = { mappings ->
                        viewModel.saveQuestionMelcMappings(examId, mappings)
                        Toast.makeText(context, "MELC mappings saved!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
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
