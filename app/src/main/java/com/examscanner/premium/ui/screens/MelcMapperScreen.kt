package com.examscanner.premium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.examscanner.premium.data.ExamEntity
import com.examscanner.premium.data.MelcEntity
import com.examscanner.premium.ui.components.FloatingGlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MelcMapperScreen(
    exam: ExamEntity,
    availableMelcs: List<MelcEntity>,
    questionMelcMap: Map<Int, Long>, // Question number to MELC ID
    onBack: () -> Unit,
    onMapQuestion: (Int, Long) -> Unit,
    onSave: () -> Unit
) {
    var selectedQuestionRange by remember { mutableStateOf(1 to 1) }
    var showMelcPicker by remember { mutableStateOf(false) }
    var filterSubject by remember { mutableStateOf("") }
    var filterGrade by remember { mutableStateOf("") }
    var filterQuarter by remember { mutableStateOf(0) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFAFAFC),
                        Color(0xFFF0F4F8)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF007AFF)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MELC Mapping",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E)
                    )
                    Text(
                        text = exam.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8E8E93)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Progress Card
            FloatingGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${questionMelcMap.size}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF007AFF)
                        )
                        Text(
                            text = "Mapped",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8E8E93)
                        )
                    }
                    Divider(
                        modifier = Modifier
                            .height(50.dp)
                            .width(1.dp),
                        color = Color(0xFFE5E5EA)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${exam.totalQuestions - questionMelcMap.size}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8E8E93)
                        )
                        Text(
                            text = "Remaining",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8E8E93)
                        )
                    }
                    Divider(
                        modifier = Modifier
                            .height(50.dp)
                            .width(1.dp),
                        color = Color(0xFFE5E5EA)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val percentage = (questionMelcMap.size * 100) / exam.totalQuestions
                        Text(
                            text = "$percentage%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (percentage == 100) Color(0xFF34C759) else Color(0xFFFF9500)
                        )
                        Text(
                            text = "Complete",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8E8E93)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Quick Map Section
            Text(
                text = "QUICK MAP",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            FloatingGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showMelcPicker = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LibraryAdd,
                        contentDescription = null,
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Map Questions to MELC",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1C1C1E)
                        )
                        Text(
                            text = "Select MELC competency",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8E8E93)
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFFAEAEB2)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mapped Questions List
            Text(
                text = "MAPPED QUESTIONS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (questionMelcMap.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Assignment,
                            contentDescription = null,
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No questions mapped yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF8E8E93)
                        )
                        Text(
                            text = "Start mapping questions to MELCs",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFAEAEB2)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(questionMelcMap.entries.sortedBy { it.key }) { entry ->
                        val melc = availableMelcs.find { it.id == entry.value }
                        melc?.let {
                            MappedQuestionCard(
                                questionNumber = entry.key,
                                melc = it
                            )
                        }
                    }
                }
            }
            
            // Save Button
            if (questionMelcMap.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007AFF)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "SAVE MAPPINGS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
    
    // MELC Picker Dialog
    if (showMelcPicker) {
        MelcPickerDialog(
            availableMelcs = availableMelcs,
            examTotalQuestions = exam.totalQuestions,
            onDismiss = { showMelcPicker = false },
            onSelect = { questionRange, melcId ->
                // Map all questions in range to this MELC
                for (q in questionRange.first..questionRange.second) {
                    onMapQuestion(q, melcId)
                }
                showMelcPicker = false
            }
        )
    }
}

@Composable
fun MappedQuestionCard(
    questionNumber: Int,
    melc: MelcEntity
) {
    FloatingGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Question Number Badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF007AFF).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$questionNumber",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF007AFF)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // MELC Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = melc.code,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF007AFF)
                )
                Text(
                    text = melc.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1C1C1E),
                    maxLines = 2
                )
                Text(
                    text = "${melc.subject} • Grade ${melc.gradeLevel} • Q${melc.quarter}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8E93)
                )
            }
        }
    }
}

@Composable
fun MelcPickerDialog(
    availableMelcs: List<MelcEntity>,
    examTotalQuestions: Int,
    onDismiss: () -> Unit,
    onSelect: (Pair<Int, Int>, Long) -> Unit
) {
    var startQuestion by remember { mutableStateOf("1") }
    var endQuestion by remember { mutableStateOf("10") }
    var selectedMelc by remember { mutableStateOf<MelcEntity?>(null) }
    var showMelcList by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Map Questions to MELC",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                // Question Range
                Text(
                    text = "Question Range",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF8E8E93)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startQuestion,
                        onValueChange = { if (it.isEmpty() || it.toIntOrNull() != null) startQuestion = it },
                        label = { Text("From") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = endQuestion,
                        onValueChange = { if (it.isEmpty() || it.toIntOrNull() != null) endQuestion = it },
                        label = { Text("To") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // MELC Selection
                Text(
                    text = "Select MELC",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF8E8E93)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMelcList = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (selectedMelc != null) {
                                Text(
                                    text = selectedMelc!!.code,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF007AFF)
                                )
                                Text(
                                    text = selectedMelc!!.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2
                                )
                            } else {
                                Text(
                                    text = "Choose MELC competency",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF8E8E93)
                                )
                            }
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF8E8E93)
                        )
                    }
                }
                
                if (selectedMelc != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "This will map questions $startQuestion-$endQuestion to this MELC",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8E8E93)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val start = startQuestion.toIntOrNull() ?: 1
                    val end = endQuestion.toIntOrNull() ?: 1
                    selectedMelc?.let { melc ->
                        onSelect(Pair(start, end), melc.id)
                    }
                },
                enabled = selectedMelc != null && 
                    startQuestion.toIntOrNull() != null && 
                    endQuestion.toIntOrNull() != null
            ) {
                Text("MAP", color = Color(0xFF007AFF))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color(0xFF8E8E93))
            }
        }
    )
    
    // MELC List Dialog
    if (showMelcList) {
        AlertDialog(
            onDismissRequest = { showMelcList = false },
            title = { Text("Select MELC") },
            text = {
                LazyColumn {
                    items(availableMelcs.take(20)) { melc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedMelc = melc
                                    showMelcList = false
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            Column {
                                Text(
                                    text = melc.code,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF007AFF)
                                )
                                Text(
                                    text = melc.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2
                                )
                                Text(
                                    text = "${melc.subject} • Grade ${melc.gradeLevel} • Q${melc.quarter}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF8E8E93)
                                )
                            }
                        }
                        Divider(color = Color(0xFFE5E5EA))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMelcList = false }) {
                    Text("CLOSE")
                }
            }
        )
    }
}
