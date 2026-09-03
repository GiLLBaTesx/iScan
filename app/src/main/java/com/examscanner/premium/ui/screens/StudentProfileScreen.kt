package com.examscanner.premium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.examscanner.premium.ui.components.FloatingGlassCard
import com.examscanner.premium.ui.theme.*
import com.examscanner.premium.viewmodel.StudentViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * StudentProfileScreen - Binds [StudentViewModel] to the comprehensive student
 * profile (Requirement 8.1).
 *
 * Shows the student's profile header (name / id / grade / photo path), their
 * performance history across scanned exams, a competency mastery summary +
 * matrix, and timestamped teacher notes with an add-note action. The screen
 * calls [StudentViewModel.loadStudentProfile] on first composition for the given
 * [studentId] and observes [StudentViewModel.uiState].
 *
 * Uses Azure Glass theme colors and content descriptions per workspace
 * conventions. The ViewModel is provided by the caller so the screen stays
 * free of DI wiring.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentProfileScreen(
    studentId: Long,
    viewModel: StudentViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(studentId) {
        viewModel.loadStudentProfile(studentId)
    }

    var showAddNote by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = IceWhite,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Student Profile",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryIce
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = ElectricBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IceWhite)
            )
        },
        floatingActionButton = {
            if (state.profile != null) {
                FloatingActionButton(
                    onClick = { showAddNote = true },
                    containerColor = ElectricBlue,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.NoteAdd, contentDescription = "Add note")
                }
            }
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding).background(IceWhite),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ElectricBlue)
                }
            }

            state.profile == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding).background(IceWhite),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        state.error ?: "Student not found",
                        color = TextSecondaryIce
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).background(IceWhite),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { ProfileHeaderCard(state) }
                    item { MasterySummaryCard(state) }
                    item { PerformanceHistoryCard(state) }
                    item { NotesCard(state) }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    if (showAddNote) {
        AddNoteDialog(
            onDismiss = { showAddNote = false },
            onConfirm = { note ->
                viewModel.addNote(note)
                showAddNote = false
            }
        )
    }
}

@Composable
private fun ProfileHeaderCard(state: StudentViewModel.StudentUiState) {
    val profile = state.profile ?: return
    FloatingGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(ElectricBlue, RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    profile.name.take(1).uppercase().ifBlank { "?" },
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    profile.name.ifBlank { "Unnamed student" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryIce
                )
                Text("ID: ${profile.studentId}", fontSize = 13.sp, color = TextSecondaryIce)
                if (profile.gradeLevel.isNotBlank()) {
                    Text("Grade: ${profile.gradeLevel}", fontSize = 13.sp, color = TextSecondaryIce)
                }
                Text(
                    "Overall average: ${state.overallAverage.toInt()}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ElectricBlue
                )
            }
        }
    }
}

@Composable
private fun MasterySummaryCard(state: StudentViewModel.StudentUiState) {
    val summary = state.masterySummary
    FloatingGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Competency Mastery", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimaryIce)
            Spacer(modifier = Modifier.height(8.dp))
            if (summary == null || summary.totalMelcsAssessed == 0) {
                Text("No mastery data yet.", fontSize = 13.sp, color = TextSecondaryIce)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MasteryStat("Advanced", summary.advanced, SuccessAzure)
                    MasteryStat("Proficient", summary.proficient, ElectricBlue)
                    MasteryStat("Approaching", summary.approaching, WarningAmber)
                    MasteryStat("Developing", summary.developing, ErrorCoral)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Assessed ${summary.totalMelcsAssessed} competencies • avg ${summary.averagePercentage.toInt()}%",
                    fontSize = 12.sp,
                    color = TextSecondaryIce
                )

                if (state.masteryMatrix.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = IceBlue)
                    Spacer(modifier = Modifier.height(8.dp))
                    state.masteryMatrix.take(20).forEach { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                entry.melc?.code ?: "MELC #${entry.mastery.melcId}",
                                fontSize = 12.sp,
                                color = TextPrimaryIce,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${entry.mastery.percentage.toInt()}% • ${entry.mastery.masteryLevel}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = masteryColor(entry.mastery.masteryLevel)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MasteryStat(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 10.sp, color = TextSecondaryIce)
    }
}

private fun masteryColor(level: String): Color = when (level.lowercase()) {
    "advanced" -> SuccessAzure
    "proficient" -> ElectricBlue
    "approaching" -> WarningAmber
    else -> ErrorCoral
}

@Composable
private fun PerformanceHistoryCard(state: StudentViewModel.StudentUiState) {
    FloatingGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Performance History", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimaryIce)
            Spacer(modifier = Modifier.height(8.dp))
            if (state.performanceHistory.isEmpty()) {
                Text("No exams scanned yet.", fontSize = 13.sp, color = TextSecondaryIce)
            } else {
                state.performanceHistory.forEach { record ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(record.examName, fontSize = 14.sp, color = TextPrimaryIce)
                            Text(
                                formatDate(record.scannedAt),
                                fontSize = 11.sp,
                                color = TextTertiaryIce
                            )
                        }
                        Text(
                            "${record.percentage}%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (record.percentage >= 75) SuccessAzure else ErrorCoral
                        )
                    }
                    Divider(color = IceBlue)
                }
            }
        }
    }
}

@Composable
private fun NotesCard(state: StudentViewModel.StudentUiState) {
    FloatingGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Teacher Notes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimaryIce)
            Spacer(modifier = Modifier.height(8.dp))
            if (state.notes.isEmpty()) {
                Text("No notes yet. Tap the + button to add one.", fontSize = 13.sp, color = TextSecondaryIce)
            } else {
                state.notes.forEach { note ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(note.note, fontSize = 14.sp, color = TextPrimaryIce)
                        Text(
                            formatDate(note.createdAt),
                            fontSize = 11.sp,
                            color = TextTertiaryIce
                        )
                    }
                    Divider(color = IceBlue)
                }
            }
        }
    }
}

@Composable
private fun AddNoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Note", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 500) note = it },
                label = { Text("Note (max 500 chars)") },
                supportingText = { Text("${note.length}/500") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 5
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(note.trim()) },
                enabled = note.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) {
                Text("SAVE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(timestamp))
}
