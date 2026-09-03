package com.examscanner.premium

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.examscanner.premium.ui.screens.Exam
import com.examscanner.premium.ui.screens.ExamDetailScreen
import com.examscanner.premium.ui.screens.ExamListScreen
import com.examscanner.premium.ui.theme.ExamScannerTheme

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
    val navController = rememberNavController()
    var currentExam by remember { mutableStateOf<Exam?>(null) }

    NavHost(navController = navController, startDestination = "exam_list") {
        composable("exam_list") {
            ExamListScreen(
                onExamClick = { exam ->
                    currentExam = exam
                    navController.navigate("exam_detail") {
                        launchSingleTop = true
                    }
                },
                onNewExamClick = {
                    // TODO: Navigate to new exam screen
                    android.util.Log.d("Navigation", "New Exam clicked")
                }
            )
        }
        
        composable("exam_detail") {
            currentExam?.let { exam ->
                ExamDetailScreen(
                    exam = exam,
                    onBack = { 
                        navController.popBackStack()
                    },
                    onScanClick = {
                        // TODO: Open camera scanner
                        android.util.Log.d("Navigation", "Scan clicked")
                    }
                )
            }
        }
    }
}
