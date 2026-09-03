package com.examscanner.premium

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.appcompat.app.AppCompatActivity
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
import com.examscanner.premium.qrcode.QRCodeGenerator
import com.examscanner.premium.scanner.BubbleSheetProcessor
import com.examscanner.premium.scanner.CameraScreen
import com.examscanner.premium.ui.screens.*
import com.examscanner.premium.ui.screens.auth.*
import com.examscanner.premium.ui.theme.ExamScannerTheme
import com.examscanner.premium.utils.AnswerSheetPrettyPrinter
import com.examscanner.premium.utils.ExportUtility
import com.examscanner.premium.utils.RootDetector
import com.examscanner.premium.utils.SecureLogger
import com.examscanner.premium.viewmodel.AnalyticsViewModel
import com.examscanner.premium.viewmodel.AnalyticsViewModelFactory
import com.examscanner.premium.viewmodel.ExamViewModel
import com.examscanner.premium.viewmodel.ExamViewModelFactory
import com.examscanner.premium.viewmodel.ReportViewModel
import com.examscanner.premium.viewmodel.ReportViewModelFactory
import kotlinx.coroutines.launch
import java.io.File

// Extends AppCompatActivity (which is itself a FragmentActivity subclass) for two reasons:
//   1. The student-records auth gate (Task 13.2, Req 8.7/13.2) uses androidx.biometric.
//      BiometricPrompt, which requires a FragmentActivity host — still satisfied.
//   2. Per-app locales (Req 20.x) rely on AppCompatDelegate.setApplicationLocales, which only
//      back-ports below API 33 when the host is an AppCompatActivity (it needs the AppCompat
//      delegate). A plain FragmentActivity silently dropped the saved Filipino locale on API
//      26–32. FragmentActivity/ComponentActivity are ancestors, so setContent / activity-compose
//      continue to work unchanged.
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Security Check: Detect rooted devices
        val isRooted = RootDetector.isDeviceRooted()
        if (isRooted) {
            SecureLogger.w("MainActivity", "Root detected: ${RootDetector.getRootDetails()}")
        }
        
        setContent {
            ExamScannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Startup gate ordering (Req 13.1, 13.2, 16.6):
                    //   1. Security/root warning  -> warn before anything sensitive is unlocked
                    //   2. Auth gate (AppLockGate) -> device credential / biometric
                    //   3. Onboarding             -> first-run tutorial
                    //   4. Main app
                    var rootWarningDismissed by remember { mutableStateOf(false) }

                    if (isRooted && !rootWarningDismissed) {
                        // 1. Security warning first, outside the auth gate, so the user is told
                        // the device is compromised before authenticating / exposing any data.
                        SecurityWarningScreen(
                            warningMessage = "Rooted Device Detected",
                            details = "This device appears to be rooted. Student exam data may be at risk.",
                            onProceedAnyway = { rootWarningDismissed = true },
                            onExit = {
                                android.os.Process.killProcess(android.os.Process.myPid())
                            }
                        )
                    } else {
                        // 2. Auth gate (Req 8.7/13.2): require a device credential or biometric on
                        // launch and on every return-to-foreground before any student data shows.
                        AppLockGate {
                            // 3 + 4. Onboarding then the main app.
                            ExamScannerApp()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExamScannerApp() {
    val context = LocalContext.current
    
    // Check onboarding status
    val onboardingPreferences = remember { 
        com.examscanner.premium.data.OnboardingPreferences(context) 
    }
    val hasSeenOnboarding by onboardingPreferences.hasSeenOnboarding.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    
    // Wait for onboarding status to load
    if (hasSeenOnboarding == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    
    // Show onboarding if not seen, otherwise show main app
    if (hasSeenOnboarding == false) {
        OnboardingScreen(
            onComplete = {
                scope.launch {
                    onboardingPreferences.setOnboardingCompleted()
                }
            }
        )
        return
    }
    
    // Main app navigation
    val navController = rememberNavController()
    
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember { ExamRepository(database.examDao()) }
    val viewModel: ExamViewModel = viewModel(
        factory = ExamViewModelFactory(repository)
    )
    
    val detailState by viewModel.detailState.collectAsState()
    
    var currentExamId by remember { mutableStateOf<Long?>(null) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // Firebase Authentication ready (currently disabled)
    // No live sync - all data stays local
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
                onAbout = { navController.navigate("about") },
                onCreateTemplate = {
                    navController.navigate("template_generator")
                },
                onDownloadTemplate = { templateName, totalQuestions ->
                    scope.launch {
                        try {
                            Toast.makeText(context, "Generating $templateName...", Toast.LENGTH_SHORT).show()
                            
                            val file = com.examscanner.premium.utils.TemplatePDFGenerator.generateTemplate(
                                context = context,
                                templateName = templateName,
                                totalQuestions = totalQuestions,
                                choicesPerQuestion = 4  // A-D default
                            )
                            
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        android.content.Intent.FLAG_ACTIVITY_NO_HISTORY
                            }
                            
                            try {
                                context.startActivity(intent)
                                Toast.makeText(context, "✓ $templateName generated!", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Open or Share Template"))
                                Toast.makeText(context, "✓ Template saved: ${file.name}", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onPreviewTemplate = { templateName, totalQuestions ->
                    scope.launch {
                        try {
                            Toast.makeText(context, "Generating preview of $templateName...", Toast.LENGTH_SHORT).show()

                            val file = com.examscanner.premium.utils.TemplatePDFGenerator.generateTemplate(
                                context = context,
                                templateName = templateName,
                                totalQuestions = totalQuestions,
                                choicesPerQuestion = 4  // A-D default
                            )

                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )

                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        android.content.Intent.FLAG_ACTIVITY_NO_HISTORY
                            }

                            try {
                                context.startActivity(intent)
                            } catch (e: android.content.ActivityNotFoundException) {
                                // Preview is view-only: no share fallback
                                Toast.makeText(context, "No PDF viewer installed to preview.", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Preview failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onManageBackups = {
                    navController.navigate("backup_management")
                },
                onClearData = {
                    scope.launch {
                        try {
                            val backupMessage = repository.clearAllData(context)
                            Toast.makeText(
                                context, 
                                "All data cleared. $backupMessage", 
                                Toast.LENGTH_LONG
                            ).show()
                            navController.popBackStack()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Clear data failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onRecycleBin = {
                    navController.navigate("recycle_bin")
                },
                onManageSubscription = {
                    navController.navigate("subscription")
                }
            )
        }
        
        // Recycle Bin Screen
        composable("recycle_bin") {
            val deletedExams by repository.getDeletedExams().collectAsState(initial = emptyList())
            val deletedSubjects by repository.getDeletedSubjectFolders().collectAsState(initial = emptyList())
            val deletedSections by repository.getDeletedSections().collectAsState(initial = emptyList())
            
            RecycleBinScreen(
                deletedExams = deletedExams,
                deletedSubjects = deletedSubjects,
                deletedSections = deletedSections,
                onBack = { navController.popBackStack() },
                onRestore = { exam ->
                    scope.launch {
                        try {
                            repository.restoreExam(exam.id)
                            Toast.makeText(context, "\"${exam.name}\" restored", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onPermanentDelete = { exam ->
                    scope.launch {
                        try {
                            repository.permanentlyDeleteExam(exam)
                            Toast.makeText(context, "\"${exam.name}\" permanently deleted", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onEmptyRecycleBin = {
                    scope.launch {
                        try {
                            repository.emptyRecycleBin()
                            Toast.makeText(context, "Recycle bin emptied", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to empty bin: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onRestoreSubject = { subject ->
                    scope.launch {
                        try {
                            repository.restoreSubjectFolder(subject.id)
                            Toast.makeText(context, "\"${subject.name}\" restored", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onPermanentDeleteSubject = { subject ->
                    scope.launch {
                        try {
                            repository.permanentlyDeleteSubjectFolder(subject.id)
                            Toast.makeText(context, "\"${subject.name}\" permanently deleted", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onRestoreSection = { section ->
                    scope.launch {
                        try {
                            repository.restoreSection(section.id)
                            Toast.makeText(context, "\"${section.name}\" restored", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onPermanentDeleteSection = { section ->
                    scope.launch {
                        try {
                            repository.permanentlyDeleteSection(section.id)
                            Toast.makeText(context, "\"${section.name}\" permanently deleted", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        // Teacher Dashboard Screen (bound to DashboardViewModel)
        composable("dashboard") {
            val dashboardViewModel: com.examscanner.premium.viewmodel.DashboardViewModel = viewModel(
                factory = com.examscanner.premium.viewmodel.DashboardViewModelFactory(repository)
            )
            val dashboardState by dashboardViewModel.uiState.collectAsState()

            TeacherDashboardScreen(
                state = dashboardState,
                onBack = { navController.popBackStack() },
                onRangeSelected = { dashboardViewModel.setDateRange(it) },
                onRefresh = { dashboardViewModel.refresh() },
                onOpenStudentProfile = { studentId ->
                    navController.navigate("student_profile/$studentId")
                },
                onOpenExam = { examId ->
                    navController.navigate("exam_detail/$examId")
                }
            )
        }

        // Student Profile Screen (bound to StudentViewModel)
        composable(
            route = "student_profile/{studentId}",
            arguments = listOf(navArgument("studentId") { type = NavType.LongType })
        ) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getLong("studentId") ?: return@composable
            val studentViewModel: com.examscanner.premium.viewmodel.StudentViewModel = viewModel(
                factory = com.examscanner.premium.viewmodel.StudentViewModelFactory(repository)
            )

            StudentProfileScreen(
                studentId = studentId,
                viewModel = studentViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // Subscription Management Screen (Req 16.2) - wired to the startup-scoped
        // SubscriptionManager (Task 15) through SubscriptionViewModel.
        composable("subscription") {
            val app = context.applicationContext as ExamScannerApplication
            val subscriptionViewModel: com.examscanner.premium.viewmodel.SubscriptionViewModel = viewModel(
                factory = com.examscanner.premium.viewmodel.SubscriptionViewModelFactory(app.subscriptionManager)
            )
            val subscriptionState by subscriptionViewModel.subscriptionState.collectAsState()
            val activity = context as? android.app.Activity

            SubscriptionScreen(
                onBack = { navController.popBackStack() },
                isPremium = subscriptionState is com.examscanner.premium.billing.SubscriptionManager.SubscriptionState.Premium,
                onUpgrade = {
                    // Launch the real Google Play Billing purchase flow. The purchase result is
                    // delivered asynchronously via the PurchasesUpdatedListener configured in
                    // ExamScannerApplication, which re-queries and promotes the state.
                    if (activity != null) {
                        subscriptionViewModel.subscribeToPremium(activity)
                    } else {
                        Toast.makeText(context, "Unable to start purchase flow.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // Privacy Policy Screen
        composable("privacy_policy") {
            PrivacyPolicyScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // Backup Management Screen
        composable("backup_management") {
            BackupManagementScreen(
                onBack = { navController.popBackStack() },
                onBackupCreated = {
                    // Success message already shown in BackupManagementScreen
                },
                onBackupRestored = {
                    // Give user time to see the success message before restarting
                    scope.launch {
                        kotlinx.coroutines.delay(5000) // 5 second delay
                        android.os.Process.killProcess(android.os.Process.myPid())
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
                                "${context.packageName}.fileprovider",
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
                },
                onExportRoster = {
                    scope.launch {
                        try {
                            val file = ExportUtility.exportRosterToCSV(
                                context = context,
                                sectionName = sectionName,
                                students = students
                            )
                            ExportUtility.shareFile(context, file)
                            Toast.makeText(context, "✓ Roster exported successfully!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
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

            // Analytics ViewModel for item-analysis visualizations (Task 11.1).
            val analyticsViewModel: AnalyticsViewModel = viewModel(
                factory = AnalyticsViewModelFactory(repository)
            )
            // Report ViewModel (Task 11.3) orchestrates PDF report generation. Wire the
            // app-scoped SubscriptionManager (Task 15) so ReportViewModel.isSchoolReportAllowed()
            // can actually grant Premium users the paid School-level report; omitting it made the
            // manager null and denied school reports for everyone.
            val reportViewModel: ReportViewModel = viewModel(
                key = "report_$examId",
                factory = ReportViewModelFactory(
                    context,
                    repository,
                    (context.applicationContext as ExamScannerApplication).subscriptionManager
                )
            )
            val reportState by reportViewModel.state.collectAsState()

            // Task 11.3: when a report PDF is produced, present the Android system share
            // sheet. ReportGenerator returns either a MediaStore content:// uri string
            // (API 29+) or an absolute file path (older) — handle both: share content
            // uris directly, wrap file paths via FileProvider.
            LaunchedEffect(reportState.resultPath, reportState.error, reportState.upgradeRequired) {
                when {
                    reportState.resultPath != null -> {
                        val pathOrUri = reportState.resultPath!!
                        try {
                            val uri = if (pathOrUri.startsWith("content://")) {
                                android.net.Uri.parse(pathOrUri)
                            } else {
                                androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    File(pathOrUri)
                                )
                            }
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                android.content.Intent.createChooser(shareIntent, "Share Report")
                            )
                            Toast.makeText(context, "✓ Report generated!", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Report saved but could not be shared: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        reportViewModel.consumeResult()
                    }
                    reportState.upgradeRequired -> {
                        Toast.makeText(
                            context,
                            "School-level reports require a premium subscription.",
                            Toast.LENGTH_LONG
                        ).show()
                        reportViewModel.consumeResult()
                    }
                    reportState.error != null -> {
                        Toast.makeText(
                            context,
                            "Failed to generate report: ${reportState.error}",
                            Toast.LENGTH_LONG
                        ).show()
                        reportViewModel.consumeResult()
                    }
                }
            }
            // Available MELCs for question -> competency mapping.
            val availableMelcs by viewModel.getAllMelcs().collectAsState(initial = emptyList())

            detailState.exam?.let { exam ->
                ExamDetailScreen(
                    exam = exam,
                    answerKeys = detailState.answerKeys,
                    students = detailState.students,
                    studentAnswers = detailState.studentAnswers,
                    questionMelcMappings = detailState.questionMelcMappings,
                    availableMelcs = availableMelcs,
                    analyticsViewModel = analyticsViewModel,
                    onSaveMelcMapping = { questionNumber, melc ->
                        // Merge the single edit into the full mapping set and persist.
                        val merged = detailState.questionMelcMappings
                            .mapValues { it.value.id }
                            .toMutableMap()
                        if (melc != null) {
                            merged[questionNumber] = melc.id
                        } else {
                            merged.remove(questionNumber)
                        }
                        viewModel.saveQuestionMelcMappings(examId, merged)
                        viewModel.loadExamDetail(examId) // Reload to reflect the new tagging
                    },
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
                                // Get student answers for export
                                val studentAnswersMap = detailState.students.associate { studentScore ->
                                    studentScore.student.id to detailState.studentAnswers.filter { 
                                        it.studentEntityId == studentScore.student.id 
                                    }
                                }
                                
                                val file = ExportUtility.exportExamToCSV(
                                    context = context,
                                    exam = exam,
                                    students = detailState.students,
                                    answerKeys = detailState.answerKeys,
                                    studentAnswersMap = studentAnswersMap
                                )
                                ExportUtility.shareFile(context, file)
                                Toast.makeText(context, "✓ Exported successfully!", Toast.LENGTH_SHORT).show()
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
                    },
                    // Task 11.2: build a printable answer sheet (QR header + bubble grid)
                    // for this exam and open/share it, reusing the FileProvider share
                    // pattern from the template_generator route.
                    onGenerateAnswerSheet = {
                        scope.launch {
                            try {
                                Toast.makeText(
                                    context,
                                    "Generating answer sheet...",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Encode exam identity into the QR header (Req 4.1/4.3).
                                val metadata = QRCodeGenerator.ExamMetadata(
                                    examId = exam.id,
                                    examName = exam.name,
                                    totalQuestions = exam.totalQuestions,
                                    subjectId = exam.subjectFolderId,
                                    createdAt = System.currentTimeMillis()
                                )
                                // Default sheet config derived from the exam's question count.
                                val config = AnswerSheetPrettyPrinter.SheetConfig(
                                    examName = exam.name,
                                    totalQuestions = exam.totalQuestions
                                )
                                val printer = AnswerSheetPrettyPrinter(
                                    qrGenerator = QRCodeGenerator(),
                                    context = context
                                )
                                val path = printer.generateAnswerSheet(
                                    exam = exam,
                                    metadata = metadata,
                                    config = config
                                )
                                val file = File(path)

                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )

                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/pdf")
                                    flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                            android.content.Intent.FLAG_ACTIVITY_NO_HISTORY
                                }

                                try {
                                    context.startActivity(intent)
                                    Toast.makeText(
                                        context,
                                        "✓ Answer sheet generated!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } catch (e: Exception) {
                                    // No PDF viewer available — offer the share sheet instead.
                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        android.content.Intent.createChooser(
                                            shareIntent,
                                            "Open or Share Answer Sheet"
                                        )
                                    )
                                    Toast.makeText(
                                        context,
                                        "✓ Answer sheet saved: ${file.name}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Failed to generate answer sheet: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    // Task 11.3: report generation. The ReportViewModel produces the PDF
                    // and the LaunchedEffect above presents the share sheet on success
                    // (or an upgrade/error toast). A "Generating..." toast gives immediate
                    // feedback since generation runs off the UI thread.
                    onGenerateIndividualReport = { studentId ->
                        Toast.makeText(context, "Generating report...", Toast.LENGTH_SHORT).show()
                        reportViewModel.generateIndividualReport(
                            studentId = studentId,
                            examId = exam.id,
                            schoolName = exam.name
                        )
                    },
                    onGenerateClassReport = {
                        Toast.makeText(context, "Generating report...", Toast.LENGTH_SHORT).show()
                        reportViewModel.generateClassReport(
                            examId = exam.id,
                            sectionId = exam.sectionId ?: 0L,
                            schoolName = exam.name
                        )
                    },
                    onGenerateSchoolReport = {
                        Toast.makeText(context, "Generating report...", Toast.LENGTH_SHORT).show()
                        val year = java.util.Calendar.getInstance()
                            .get(java.util.Calendar.YEAR)
                        reportViewModel.generateSchoolReport(
                            subjectId = exam.subjectFolderId,
                            quarter = 1,
                            schoolYear = "$year-${year + 1}",
                            schoolName = exam.name
                        )
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
            var refreshKey by remember { mutableStateOf(0) }
            
            // CSV Import launcher
            val csvImportLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let {
                    scope.launch {
                        try {
                            val result = com.examscanner.premium.utils.CSVAnswerKeyImport.importAnswerKeyFromCSV(
                                context = context,
                                uri = it,
                                totalQuestions = totalQuestions
                            )
                            
                            if (result.successCount > 0) {
                                // Save imported answers
                                val keysList = result.answerKey.map { (question, answer) -> 
                                    question to answer 
                                }
                                viewModel.saveAnswerKey(examId, keysList)
                                viewModel.loadExamDetail(examId) // Reload to show imported answers
                                
                                val message = "✓ Imported ${result.successCount} answers" +
                                    if (result.failedCount > 0) " (${result.failedCount} failed)" else ""
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                
                                if (result.errors.isNotEmpty()) {
                                    // Show first few errors
                                    val errorMsg = result.errors.take(3).joinToString("\n")
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "Import failed: ${result.errors.firstOrNull() ?: "No valid answers found"}", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            
            // Reload MELC mappings whenever this screen appears OR when refreshKey changes
            LaunchedEffect(examId, backStackEntry, refreshKey) {
                currentMelcMappings = viewModel.getQuestionMelcMappings(examId)
            }
            
            EditKeyScreen(
                examId = examId,
                examName = examName,
                totalQuestions = totalQuestions,
                currentAnswers = currentAnswers,
                availableMelcs = allMelcs,
                currentMelcMappings = currentMelcMappings,
                onBack = { 
                    Toast.makeText(context, "Answer key and MELCs saved!", Toast.LENGTH_SHORT).show()
                    navController.popBackStack() 
                },
                onSave = { answers ->
                    val keysList = answers.map { (question, answer) -> 
                        question to answer 
                    }
                    viewModel.saveAnswerKey(examId, keysList)
                },
                onSaveMelcMappings = { mappings ->
                    scope.launch {
                        viewModel.saveQuestionMelcMappings(examId, mappings)
                        // Immediately refresh the mappings after save
                        currentMelcMappings = viewModel.getQuestionMelcMappings(examId)
                        refreshKey++ // Force refresh
                    }
                },
                onImportCSV = { csvImportLauncher.launch("text/csv") }
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
            // Real detection needs the selected exam's layout. Question count comes from
            // the loaded exam (fallback 20); option labels are derived from the answer key's
            // choice set when available, otherwise the A–D default.
            val processingTotalQuestions = detailState.exam?.totalQuestions ?: 20
            val processingOptionLabels = run {
                val maxLabel = detailState.answerKeys
                    .mapNotNull { key -> key.correctAnswer.singleOrNull()?.takeIf { it in 'A'..'G' } }
                    .maxOrNull()
                val count = if (maxLabel != null) (maxLabel - 'A' + 1).coerceIn(2, 7) else 4
                (0 until count).map { ('A' + it).toString() }
            }
            ProcessingScreen(
                imageUri = capturedImageUri,
                totalQuestions = processingTotalQuestions,
                optionLabels = processingOptionLabels,
                // Task 11.2 (Req 4.7/22.3): QR-first auto-association. When the scanned
                // sheet's QR code identifies an exam, retarget the save to that exam. If
                // no QR is found, currentExamId (the manually selected exam) is used —
                // the graceful fallback path.
                onExamDetected = { metadata ->
                    currentExamId = metadata.examId
                    viewModel.loadExamDetail(metadata.examId)
                    Toast.makeText(
                        context,
                        "Matched exam from QR: ${metadata.examName}",
                        Toast.LENGTH_SHORT
                    ).show()
                },
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
        
        // About Screen
        composable("about") {
            AboutScreen(
                onBack = { navController.popBackStack() },
                onPrivacyPolicy = { navController.navigate("privacy_policy") },
                onTermsOfService = { navController.navigate("terms_of_service") },
                onLicenses = {
                    // TODO: Open system licenses screen or create custom one
                    Toast.makeText(context, "Open source licenses", Toast.LENGTH_SHORT).show()
                }
            )
        }
        
        // Terms of Service Screen
        composable("terms_of_service") {
            TermsOfServiceScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
