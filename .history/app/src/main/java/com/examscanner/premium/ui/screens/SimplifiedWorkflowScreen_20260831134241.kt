package com.examscanner.premium.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.examscanner.premium.data.MelcEntity

/**
 * SIMPLIFIED MVP WORKFLOW
 * 
 * 4-Step Flow:
 * STEP 1: Upload/Scan sheets
 * STEP 2: Set answer key + tag competencies
 * STEP 3: Smart dashboard (3 tabs: Reteach / Analysis / Intervention)
 * STEP 4: Export PDF
 * 
 * NO login, NO classes, NO profiles - ONE test at a time
 */
@Composable
fun SimplifiedWorkflowScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    // Workflow state
    var currentStep by remember { mutableStateOf(WorkflowStep.UPLOAD) }
    var scannedSheets by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var answerKey by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var competencyMappings by remember { mutableStateOf<Map<Int, MelcEntity>>(emptyMap()) }
    var questionCount by remember { mutableStateOf(20) } // Default 20 questions
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4F8))
    ) {
        when (currentStep) {
            WorkflowStep.UPLOAD -> {
                // STEP 1: Upload/Scan
                UploadStepScreen(
                    onSheetsUploaded = { sheets, count ->
                        scannedSheets = sheets
                        questionCount = count
                        currentStep = WorkflowStep.SET_ANSWERS
                    },
                    onBack = onBack
                )
            }
            
            WorkflowStep.SET_ANSWERS -> {
                // STEP 2: Set Answers + Tag Competencies
                // Reuse your existing EditKeyScreen with MELC tagging
                SetAnswersStepScreen(
                    questionCount = questionCount,
                    currentAnswerKey = answerKey,
                    currentCompetencies = competencyMappings,
                    onSave = { answers, competencies ->
                        answerKey = answers
                        competencyMappings = competencies
                        currentStep = WorkflowStep.DASHBOARD
                    },
                    onBack = {
                        currentStep = WorkflowStep.UPLOAD
                    }
                )
            }
            
            WorkflowStep.DASHBOARD -> {
                // STEP 3: Smart Dashboard (3 tabs)
                SmartDashboardScreen(
                    scannedSheets = scannedSheets,
                    answerKey = answerKey,
                    competencyMappings = competencyMappings,
                    onExport = {
                        currentStep = WorkflowStep.EXPORT
                    },
                    onBack = {
                        currentStep = WorkflowStep.SET_ANSWERS
                    }
                )
            }
            
            WorkflowStep.EXPORT -> {
                // STEP 4: Export PDF
                ExportStepScreen(
                    onExported = {
                        onComplete()
                    },
                    onBack = {
                        currentStep = WorkflowStep.DASHBOARD
                    }
                )
            }
        }
    }
}

enum class WorkflowStep {
    UPLOAD,       // Step 1: Camera scan or CSV upload
    SET_ANSWERS,  // Step 2: Answer key + competency tagging
    DASHBOARD,    // Step 3: Smart analysis (3 tabs)
    EXPORT        // Step 4: PDF generation
}

// ============================================================================
// STEP 1: Upload/Scan
// ============================================================================
@Composable
fun UploadStepScreen(
    onSheetsUploaded: (sheets: List<Uri>, questionCount: Int) -> Unit,
    onBack: () -> Unit
) {
    // TODO: Replace with your CameraScreen or add CSV upload option
    // For now, showing placeholder
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("STEP 1: Upload/Scan")
        Text("Use your CameraScreen.kt here")
        
        // Mock button for testing
        Button(onClick = {
            // Simulate scan completion
            onSheetsUploaded(emptyList(), 20)
        }) {
            Text("Continue (Mock)")
        }
    }
}

// ============================================================================
// STEP 2: Set Answers + Tag Competencies
// ============================================================================
@Composable
fun SetAnswersStepScreen(
    questionCount: Int,
    currentAnswerKey: Map<Int, String>,
    currentCompetencies: Map<Int, MelcEntity>,
    onSave: (answers: Map<Int, String>, competencies: Map<Int, MelcEntity>) -> Unit,
    onBack: () -> Unit
) {
    // TODO: Reuse your EditKeyScreen.kt + MelcSelectorDialog.kt here
    // This is the screen where teachers:
    // 1. Tap correct answer per question (A/B/C/D/E)
    // 2. Tap 📚 to tag competency per question
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("STEP 2: Set Answer Key + Tag Competencies")
        Text("Integrate EditKeyScreen.kt here")
        
        // Mock button
        Button(onClick = {
            onSave(emptyMap(), emptyMap())
        }) {
            Text("Save & Continue (Mock)")
        }
    }
}

// ============================================================================
// STEP 3: Smart Dashboard (3 Tabs)
// ============================================================================
@Composable
fun SmartDashboardScreen(
    scannedSheets: List<Uri>,
    answerKey: Map<Int, String>,
    competencyMappings: Map<Int, MelcEntity>,
    onExport: () -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(DashboardTab.RETEACH) }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Tab Row
        TabRow(selectedTabIndex = selectedTab.ordinal) {
            Tab(
                selected = selectedTab == DashboardTab.RETEACH,
                onClick = { selectedTab = DashboardTab.RETEACH },
                text = { Text("What to Reteach") }
            )
            Tab(
                selected = selectedTab == DashboardTab.ANALYSIS,
                onClick = { selectedTab = DashboardTab.ANALYSIS },
                text = { Text("Item Analysis") }
            )
            Tab(
                selected = selectedTab == DashboardTab.INTERVENTION,
                onClick = { selectedTab = DashboardTab.INTERVENTION },
                text = { Text("Interventions") }
            )
        }
        
        // Tab Content
        when (selectedTab) {
            DashboardTab.RETEACH -> {
                ReteachTab(competencyMappings = competencyMappings)
            }
            DashboardTab.ANALYSIS -> {
                AnalysisTab(answerKey = answerKey)
            }
            DashboardTab.INTERVENTION -> {
                InterventionTab()
            }
        }
        
        // Export Button
        Button(
            onClick = onExport,
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Export PDF Report")
        }
    }
}

enum class DashboardTab {
    RETEACH,      // Top 3 weakest competencies
    ANALYSIS,     // Difficulty Index, Discrimination Index
    INTERVENTION  // Student grouping by weak competencies
}

@Composable
fun ReteachTab(competencyMappings: Map<Int, MelcEntity>) {
    // TODO: Show top 3 weakest competencies
    // Use your CompetencyAnalysisScreen logic here
    Column(modifier = Modifier.padding(16.dp)) {
        Text("🎯 What to Reteach Now")
        Text("Top 3 weakest competencies:")
        // List competencies sorted by lowest mastery %
    }
}

@Composable
fun AnalysisTab(answerKey: Map<Int, String>) {
    // TODO: Show Item Analysis with Difficulty & Discrimination Index
    // Use your ItemAnalysisScreen logic + add:
    // - Difficulty Index = % of students who got it right
    // - Discrimination Index = (High scorers' % correct) - (Low scorers' % correct)
    Column(modifier = Modifier.padding(16.dp)) {
        Text("📊 Scores & Item Analysis")
        Text("Showing Difficulty Index, Discrimination Index")
        // Color-code: 🟢 Easy (>70%), 🟡 Moderate (30-70%), 🔴 Difficult (<30%)
    }
}

@Composable
fun InterventionTab() {
    // TODO: Group students by weakest competency
    // Show: "Students struggling with [Competency X]: John, Mary, Peter"
    Column(modifier = Modifier.padding(16.dp)) {
        Text("👥 Student Intervention Groups")
        Text("Students grouped by competency gaps:")
        // List intervention groups
    }
}

// ============================================================================
// STEP 4: Export PDF
// ============================================================================
@Composable
fun ExportStepScreen(
    onExported: () -> Unit,
    onBack: () -> Unit
) {
    // TODO: One-tap PDF generation
    // Include: Scores, Item Analysis, Action Plan, Parent Reports
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("STEP 4: Export PDF")
        Text("Generating comprehensive report...")
        
        Button(onClick = onExported) {
            Text("Done")
        }
    }
}
