package com.examscanner.premium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.examscanner.premium.data.AnswerKeyEntity
import com.examscanner.premium.data.ExamEntity
import com.examscanner.premium.ui.components.FloatingGlassCard
import com.examscanner.premium.ui.components.GlassCard
import com.examscanner.premium.ui.theme.*

@Composable
fun ExamDetailScreen(
    exam: ExamEntity,
    answerKeys: List<AnswerKeyEntity>,
    students: List<com.examscanner.premium.data.StudentScore>,
    studentAnswers: List<com.examscanner.premium.data.StudentAnswerEntity> = emptyList(),
    questionMelcMappings: Map<Int, com.examscanner.premium.data.MelcEntity> = emptyMap(),
    onBack: () -> Unit,
    onScanClick: () -> Unit,
    onEditKeyClick: () -> Unit,
    onResetClick: () -> Unit,
    onExportClick: () -> Unit,
    onEditExam: (String) -> Unit = {},
    onDeleteExam: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf("SCORES") }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val averageScore = if (students.isNotEmpty()) {
        students.map { it.percentage }.average().toInt()
    } else 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Compact Header
            FloatingGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = PrimaryBlue
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = exam.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = "${exam.totalQuestions}Q · ${students.size} Students",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Row {
                        IconButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = androidx.compose.ui.graphics.Color(0xFFFF3B30),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Compact Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Scanned Count
                FloatingGlassCard(modifier = Modifier.weight(1f)) {
                    StatBoxCompact(
                        value = students.size.toString(),
                        label = "Scanned",
                        color = PrimaryBlue
                    )
                }
                // Average Score
                FloatingGlassCard(modifier = Modifier.weight(1f)) {
                    StatBoxCompact(
                        value = "$averageScore%",
                        label = "Average",
                        color = if (averageScore >= 75) SuccessGreen else WarningOrange
                    )
                }
                // Questions
                FloatingGlassCard(modifier = Modifier.weight(1f)) {
                    StatBoxCompact(
                        value = exam.totalQuestions.toString(),
                        label = "Questions",
                        color = TextPrimary
                    )
                }
            }

            // Compact Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Prominent Scan Button
                GlassCard(
                    modifier = Modifier.weight(1.6f),
                    backgroundColor = PrimaryBlue,
                    cornerRadius = 12.dp
                ) {
                    Button(
                        onClick = onScanClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        elevation = null
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.QrCodeScanner,
                                contentDescription = "Scan",
                                tint = SurfaceWhite,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SCAN",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SurfaceWhite
                            )
                        }
                    }
                }
                
                // Quick Actions
                FloatingGlassCard(modifier = Modifier.weight(1f)) {
                    Button(
                        onClick = onEditKeyClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        elevation = null
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Key",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "KEY",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary
                            )
                        }
                    }
                }
                
                FloatingGlassCard(modifier = Modifier.weight(1f)) {
                    Button(
                        onClick = onExportClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        elevation = null
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning, // Using Warning as placeholder - should be Share
                                contentDescription = "Export",
                                tint = SuccessGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "EXPORT",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Enhanced Content Tab Bar with SMART Badge
            FloatingGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    EnhancedContentTabButton(
                        text = "SCORES",
                        isSelected = selectedTab == "SCORES",
                        modifier = Modifier.weight(1f),
                        badge = null
                    ) { selectedTab = "SCORES" }
                    
                    EnhancedContentTabButton(
                        text = "ITEM",
                        isSelected = selectedTab == "ITEM ANALYSIS",
                        modifier = Modifier.weight(1f),
                        badge = null
                    ) { selectedTab = "ITEM ANALYSIS" }
                    
                    EnhancedContentTabButton(
                        text = "MELC",
                        isSelected = selectedTab == "COMPETENCY",
                        modifier = Modifier.weight(1f),
                        badge = null
                    ) { selectedTab = "COMPETENCY" }
                    
                    EnhancedContentTabButton(
                        text = "SMART",
                        isSelected = selectedTab == "SMART",
                        modifier = Modifier.weight(1f),
                        badge = "AI",
                        highlightColor = AccentOrange
                    ) { selectedTab = "SMART" }
                }
            }

            // Content based on selected tab
            when (selectedTab) {
                "SCORES" -> {
                    // Student List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(students) { index, student ->
                            StudentScoreCard(
                                rank = index + 1,
                                studentScore = student
                            )
                        }
                    }
                }
                "ITEM ANALYSIS" -> {
                    // Item Analysis Content
                    ItemAnalysisContent(
                        answerKeys = answerKeys,
                        allStudentAnswers = studentAnswers
                    )
                }
                "COMPETENCY" -> {
                    // Competency Analysis - MELC-based performance
                    CompetencyAnalysisScreen(
                        examId = exam.id,
                        sectionName = null,
                        questionMelcMappings = questionMelcMappings,
                        answerKeys = answerKeys,
                        studentAnswers = studentAnswers,
                        onBack = { selectedTab = "SCORES" }
                    )
                }
                "SMART" -> {
                    // Smart Dashboard MVP - AI-powered analysis
                    val studentEntities = students.map { it.student }
                    SmartDashboardMVP(
                        examId = exam.id,
                        examName = exam.name,
                        answerKeys = answerKeys,
                        studentAnswers = studentAnswers,
                        students = studentEntities,
                        questionMelcMappings = questionMelcMappings,
                        onExportPDF = {
                            // TODO: Implement PDF export using SmartReportPDFGenerator
                        },
                        onBack = { selectedTab = "SCORES" }
                    )
                }
            }
        }
    }
    
    // Edit Dialog
    if (showEditDialog) {
        var examName by remember { mutableStateOf(exam.name) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = "Rename Exam",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter new name for this exam",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = examName,
                        onValueChange = { if (it.length <= 100) examName = it },
                        label = { Text("Exam Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (examName.isNotBlank() && examName != exam.name) {
                            onEditExam(examName)
                            showEditDialog = false
                        }
                    },
                    enabled = examName.isNotBlank() && examName != exam.name
                ) {
                    Text("SAVE", color = PrimaryBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }
    
    // Delete Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color(0xFFFF3B30),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Delete Exam?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to delete \"${exam.name}\"?\n\nAll student results and answer keys will be permanently deleted.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteExam()
                    }
                ) {
                    Text("DELETE", color = androidx.compose.ui.graphics.Color(0xFFFF3B30))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun StatBox(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
fun StatBoxCompact(
    value: String, 
    label: String,
    color: androidx.compose.ui.graphics.Color = TextPrimary
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(40.dp)
            .padding(2.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) PrimaryBlue else androidx.compose.ui.graphics.Color.Transparent,
            contentColor = if (isSelected) SurfaceWhite else TextSecondary
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = null,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun ContentTabButton(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(40.dp)
            .padding(2.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) SurfaceWhite else androidx.compose.ui.graphics.Color.Transparent,
            contentColor = if (isSelected) TextPrimary else TextSecondary
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = if (isSelected) ButtonDefaults.buttonElevation(2.dp) else null,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun EnhancedContentTabButton(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    badge: String? = null,
    highlightColor: androidx.compose.ui.graphics.Color = PrimaryBlue,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .padding(2.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = when {
                isSelected && badge != null -> highlightColor
                isSelected -> PrimaryBlue
                else -> androidx.compose.ui.graphics.Color.Transparent
            },
            contentColor = if (isSelected) SurfaceWhite else TextSecondary
        ),
        shape = RoundedCornerShape(10.dp),
        elevation = if (isSelected) ButtonDefaults.buttonElevation(3.dp) else null,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (badge != null) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) SurfaceWhite.copy(alpha = 0.9f) else highlightColor
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = if (badge != null) 11.sp else 12.sp
            )
        }
    }
}

@Composable
fun StudentScoreCard(rank: Int, studentScore: com.examscanner.premium.data.StudentScore, onClick: () -> Unit = {}) {
    FloatingGlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = String.format("%02d", rank),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextTertiary,
                    modifier = Modifier.width(32.dp)
                )
                Column {
                    Text(
                        text = "ID ${studentScore.student.studentId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = studentScore.student.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${studentScore.score}/${studentScore.total}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Text(
                    text = "${studentScore.percentage}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (studentScore.percentage >= 50) SuccessGreen else ErrorRed
                )
            }
        }
    }
}

// Note: The edit and delete dialogs are handled within ExamDetailScreen composable function above
// They are already included in the function implementation with showEditDialog and showDeleteDialog states
