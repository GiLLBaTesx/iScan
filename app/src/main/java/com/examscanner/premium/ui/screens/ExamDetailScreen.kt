package com.examscanner.premium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.examscanner.premium.data.AnswerKeyEntity
import com.examscanner.premium.data.ExamEntity
import com.examscanner.premium.data.MelcEntity
import com.examscanner.premium.ui.components.FloatingGlassCard
import com.examscanner.premium.ui.components.GlassCard
import com.examscanner.premium.ui.theme.*
import com.examscanner.premium.viewmodel.AnalyticsViewModel

/** Segmented tabs for the exam detail screen (Results / Analytics / Reports). */
private enum class ExamTab { Results, Analytics, Reports }

@OptIn(ExperimentalMaterial3Api::class)
// availableMelcs / onSaveMelcMapping / onDeleteExam are retained for call-site
// (MainActivity) signature compatibility but are no longer invoked in this screen.
@Suppress("UNUSED_PARAMETER")
@Composable
fun ExamDetailScreen(
    exam: ExamEntity,
    answerKeys: List<AnswerKeyEntity>,
    students: List<com.examscanner.premium.data.StudentScore>,
    studentAnswers: List<com.examscanner.premium.data.StudentAnswerEntity> = emptyList(),
    questionMelcMappings: Map<Int, com.examscanner.premium.data.MelcEntity> = emptyMap(),
    availableMelcs: List<MelcEntity> = emptyList(),
    analyticsViewModel: AnalyticsViewModel? = null,
    onBack: () -> Unit,
    onScanClick: () -> Unit,
    onEditKeyClick: () -> Unit,
    onResetClick: () -> Unit,
    onExportClick: () -> Unit,
    onEditExam: (String) -> Unit = {},
    onDeleteExam: () -> Unit = {},
    onSaveMelcMapping: (questionNumber: Int, melc: MelcEntity?) -> Unit = { _, _ -> },
    // Task 11.2: generates a printable answer sheet (QR header + bubble grid) for this exam
    // via AnswerSheetPrettyPrinter and shares/opens the resulting PDF. Defaulted so existing
    // callers keep compiling; wired in MainActivity's exam_detail route.
    onGenerateAnswerSheet: () -> Unit = {},
    // Task 11.3: report generation entry points. Each produces a PDF via ReportViewGenerator
    // and presents the Android system share sheet. Defaulted so existing callers keep
    // compiling; wired in MainActivity's exam_detail route through a ReportViewModel.
    // Individual reports need a selected student (picked via a simple picker below).
    onGenerateIndividualReport: (studentId: Long) -> Unit = {},
    onGenerateClassReport: () -> Unit = {},
    onGenerateSchoolReport: () -> Unit = {}
) {
    // Segmented tab selection for the content area (only shown when there are students).
    var selectedTab by remember { mutableStateOf(ExamTab.Results) }
    // Task 11.3: when true, shows a student picker so the teacher can choose whose
    // individual report to generate.
    var showIndividualReportPicker by remember { mutableStateOf(false) }
    var showOptions by remember { mutableStateOf(false) }
    val hasStudents = students.isNotEmpty()
    val averageScore = if (hasStudents) {
        students.map { it.percentage }.average().toInt()
    } else 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = exam.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "${exam.totalQuestions} questions",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showOptions = !showOptions }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options"
                            )
                        }
                        DropdownMenu(
                            expanded = showOptions,
                            onDismissRequest = { showOptions = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit answer key") },
                                onClick = {
                                    showOptions = false
                                    onEditKeyClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Rename exam") },
                                onClick = {
                                    showOptions = false
                                    onEditExam(exam.name)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                            )
                            // Task 11.2: printable answer sheet with QR header + bubble grid.
                            DropdownMenuItem(
                                text = { Text("Generate answer sheet") },
                                onClick = {
                                    showOptions = false
                                    onGenerateAnswerSheet()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Print,
                                        contentDescription = "Generate a printable answer sheet for this exam",
                                        tint = ElectricBlue
                                    )
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceWhite
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundWhite)
        ) {
            if (!hasStudents) {
                // Empty state - guide the user
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QrCodeScanner,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(120.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Ready to grade!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Tap the big blue button below to start scanning answer sheets",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                    Button(
                        onClick = onScanClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Start Scanning",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Task 11.2: let teachers print an answer sheet before scanning any.
                    OutlinedButton(
                        onClick = onGenerateAnswerSheet,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Generate a printable answer sheet for this exam",
                            tint = ElectricBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Generate Answer Sheet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ElectricBlue
                        )
                    }
                }
            } else {
                // Has data - show segmented tabs + selected tab content.
                Column(modifier = Modifier.fillMaxSize()) {
                    // Segmented tab bar (Results / Analytics / Reports).
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SimpleTabButton(
                            text = "Results",
                            isSelected = selectedTab == ExamTab.Results,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedTab = ExamTab.Results }
                        )
                        SimpleTabButton(
                            text = "Analytics",
                            isSelected = selectedTab == ExamTab.Analytics,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedTab = ExamTab.Analytics }
                        )
                        SimpleTabButton(
                            text = "Reports",
                            isSelected = selectedTab == ExamTab.Reports,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedTab = ExamTab.Reports }
                        )
                    }

                    when (selectedTab) {
                        ExamTab.Results -> ResultsTabContent(
                            students = students,
                            averageScore = averageScore,
                            onScanClick = onScanClick,
                            onExportClick = onExportClick,
                            onResetClick = onResetClick
                        )
                        ExamTab.Analytics -> AnalyticsTabContent(
                            exam = exam,
                            answerKeys = answerKeys,
                            studentAnswers = studentAnswers,
                            students = students,
                            analyticsViewModel = analyticsViewModel
                        )
                        ExamTab.Reports -> ReportsTabContent(
                            onGenerateIndividualReport = { showIndividualReportPicker = true },
                            onGenerateClassReport = onGenerateClassReport,
                            onGenerateSchoolReport = onGenerateSchoolReport
                        )
                    }
                }
            }
        }

        // Task 11.3: pick a student, then generate their individual PDF report.
        if (showIndividualReportPicker && hasStudents) {
            IndividualReportStudentPicker(
                students = students,
                onDismiss = { showIndividualReportPicker = false },
                onSelect = { studentId ->
                    showIndividualReportPicker = false
                    onGenerateIndividualReport(studentId)
                }
            )
        }
    }
}

/**
 * Simple student picker for individual report generation (Task 11.3, Req 5.1/5.7).
 * Lists the exam's students; selecting one triggers PDF report generation for that
 * student. Uses Azure Glass theme colors and per-row content descriptions.
 */
@Composable
private fun IndividualReportStudentPicker(
    students: List<com.examscanner.premium.data.StudentScore>,
    onDismiss: () -> Unit,
    onSelect: (studentId: Long) -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = FrostedWhite)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ElectricBlue)
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "Individual Report",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = FrostedWhite
                        )
                        Text(
                            text = "Select a student to generate their PDF report",
                            fontSize = 13.sp,
                            color = FrostedWhite.copy(alpha = 0.9f)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .background(IceWhite)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(students) { _, studentScore ->
                        val student = studentScore.student
                        Surface(
                            onClick = { onSelect(student.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = SoftIce,
                            border = androidx.compose.foundation.BorderStroke(1.dp, LightGray),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = student.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimaryIce
                                    )
                                    Text(
                                        text = "${studentScore.percentage}%",
                                        fontSize = 13.sp,
                                        color = if (studentScore.percentage >= 75) SuccessAzure else ErrorRed
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = "Generate report for ${student.name}",
                                    tint = IcyCyan
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FrostedWhite)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "CANCEL",
                            color = ElectricBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Results tab: class stats card, Scan More primary action, Export / Clear-all
 * actions, and the ranked student score list. Uses Azure Glass theme colors.
 */
@Composable
private fun ColumnScope.ResultsTabContent(
    students: List<com.examscanner.premium.data.StudentScore>,
    averageScore: Int,
    onScanClick: () -> Unit,
    onExportClick: () -> Unit,
    onResetClick: () -> Unit
) {
    // Class Stats Card
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = students.size.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
                Text(
                    text = "Students",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary
                )
            }
            Divider(
                modifier = Modifier
                    .width(1.dp)
                    .height(80.dp),
                color = LightGray
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$averageScore%",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (averageScore >= 75) SuccessGreen else ErrorRed
                )
                Text(
                    text = "Class Average",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Scan More primary action.
    Button(
        onClick = onScanClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(64.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.QrCodeScanner,
            contentDescription = null,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Scan More",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Export / Clear-all results actions.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onExportClick,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = "Export exam results",
                tint = ElectricBlue,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Export",
                color = ElectricBlue,
                fontWeight = FontWeight.SemiBold
            )
        }
        OutlinedButton(
            onClick = onResetClick,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Clear all results for this exam",
                tint = ErrorRed,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Clear all",
                color = ErrorRed,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Student List
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(students) { index, student ->
            StudentScoreCard(
                rank = index + 1,
                studentScore = student
            )
        }
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

/**
 * Analytics tab: renders the item-analysis experience inline via
 * [ItemAnalysisPanel] when an [AnalyticsViewModel] is available. If no
 * view model is present, a lightweight placeholder message is shown instead.
 */
@Composable
private fun AnalyticsTabContent(
    exam: ExamEntity,
    answerKeys: List<AnswerKeyEntity>,
    studentAnswers: List<com.examscanner.premium.data.StudentAnswerEntity>,
    students: List<com.examscanner.premium.data.StudentScore>,
    analyticsViewModel: AnalyticsViewModel?
) {
    if (analyticsViewModel != null) {
        ItemAnalysisPanel(
            examId = exam.id,
            examName = exam.name,
            answerKeys = answerKeys,
            studentAnswers = studentAnswers,
            students = students,
            analyticsViewModel = analyticsViewModel,
            onBack = { /* tab-hosted: navigation handled by tab bar */ },
            embedded = true
        )
    } else {
        // No analytics view model available: show a graceful placeholder
        // rather than a full-screen fallback (avoids the double-header artifact).
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(IceWhite)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Analytics are unavailable for this exam.",
                color = TextSecondaryIce,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Reports tab: PDF report generation entry points as labeled cards/buttons.
 * Individual reports open the student picker; class/school reports fire directly.
 * Uses Azure Glass theme colors and content descriptions.
 */
@Composable
private fun ColumnScope.ReportsTabContent(
    onGenerateIndividualReport: () -> Unit,
    onGenerateClassReport: () -> Unit,
    onGenerateSchoolReport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ReportActionCard(
            title = "Individual student report",
            subtitle = "Generate a PDF report for a single student",
            tint = IcyCyan,
            onClick = onGenerateIndividualReport
        )
        ReportActionCard(
            title = "Class summary report",
            subtitle = "Generate a class summary PDF report",
            tint = ElectricBlue,
            onClick = onGenerateClassReport
        )
        ReportActionCard(
            title = "School-level report",
            subtitle = "Generate a school-level PDF report (premium)",
            tint = LuminousAzure,
            onClick = onGenerateSchoolReport
        )
    }
}

/** Reusable report action card for the Reports tab. */
@Composable
private fun ReportActionCard(
    title: String,
    subtitle: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = SoftIce,
        border = androidx.compose.foundation.BorderStroke(1.dp, LightGray),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = subtitle,
                tint = tint
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryIce
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = TextSecondaryIce
                )
            }
        }
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
fun SimpleTabButton(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    badge: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) PrimaryBlue else androidx.compose.ui.graphics.Color.Transparent,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (badge != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isSelected) SurfaceWhite.copy(alpha = 0.3f) else WarningOrange
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) SurfaceWhite else SurfaceWhite,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) SurfaceWhite else TextSecondary
                )
            }
        }
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
