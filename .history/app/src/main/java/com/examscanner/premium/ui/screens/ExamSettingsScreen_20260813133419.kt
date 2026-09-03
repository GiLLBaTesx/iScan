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
import com.examscanner.premium.data.GradingScaleEntity
import com.examscanner.premium.ui.components.FloatingGlassCard
import com.examscanner.premium.ui.components.GlassCard
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamSettingsScreen(
    exam: ExamEntity,
    gradingScales: List<GradingScaleEntity>,
    onBack: () -> Unit,
    onSave: (ExamEntity) -> Unit
) {
    var selectedGradingScale by remember { mutableStateOf(exam.gradingScale) }
    var passingGrade by remember { mutableStateOf(exam.passingGrade.toString()) }
    var useNegativeMarking by remember { mutableStateOf(exam.useNegativeMarking) }
    var negativeMarkValue by remember { mutableStateOf(exam.negativeMarkValue.toString()) }
    var showGradingScalePicker by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    
    val currentScale = gradingScales.find { it.scaleType == selectedGradingScale }
    
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
                        text = "Exam Settings",
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
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Grading Scale Section
                item {
                    Text(
                        text = "GRADING SCALE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                
                item {
                    FloatingGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showGradingScalePicker = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Grade,
                                contentDescription = null,
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Grading System",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF8E8E93)
                                )
                                Text(
                                    text = currentScale?.name ?: "DepEd K-12",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1C1C1E)
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFFAEAEB2)
                            )
                        }
                    }
                }
                
                // Preview Button
                item {
                    FloatingGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPreview = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Preview Grade Distribution",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF007AFF)
                            )
                        }
                    }
                }
                
                // Passing Grade
                item {
                    Text(
                        text = "PASSING CRITERIA",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                
                item {
                    FloatingGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Passing Grade",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1C1C1E)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = passingGrade,
                                onValueChange = { 
                                    if (it.isEmpty() || it.toIntOrNull() != null) {
                                        passingGrade = it
                                    }
                                },
                                label = { Text("Passing Grade (%)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF007AFF),
                                    focusedLabelColor = Color(0xFF007AFF)
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Students scoring ${passingGrade.ifBlank { "75" }}% or above will pass",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF8E8E93)
                            )
                        }
                    }
                }
                
                // Scoring Options
                item {
                    Text(
                        text = "SCORING OPTIONS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                
                item {
                    FloatingGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            // Negative Marking Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Remove,
                                    contentDescription = null,
                                    tint = Color(0xFF007AFF),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Negative Marking",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1C1C1E)
                                    )
                                    Text(
                                        text = "Deduct points for wrong answers",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF8E8E93)
                                    )
                                }
                                Switch(
                                    checked = useNegativeMarking,
                                    onCheckedChange = { useNegativeMarking = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF007AFF)
                                    )
                                )
                            }
                            
                            // Negative Mark Value (shown if enabled)
                            if (useNegativeMarking) {
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = negativeMarkValue,
                                    onValueChange = { 
                                        if (it.isEmpty() || it.toFloatOrNull() != null) {
                                            negativeMarkValue = it
                                        }
                                    },
                                    label = { Text("Deduction per wrong answer") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    placeholder = { Text("0.25") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF007AFF),
                                        focusedLabelColor = Color(0xFF007AFF)
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Common: -0.25 (NAT format)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF8E8E93)
                                )
                            }
                        }
                    }
                }
            }
            
            // Save Button
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    val updatedExam = exam.copy(
                        gradingScale = selectedGradingScale,
                        passingGrade = passingGrade.toIntOrNull() ?: 75,
                        useNegativeMarking = useNegativeMarking,
                        negativeMarkValue = if (useNegativeMarking) 
                            negativeMarkValue.toFloatOrNull() ?: 0.25f 
                        else 0f
                    )
                    onSave(updatedExam)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF007AFF)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "SAVE SETTINGS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
    
    // Grading Scale Picker Dialog
    if (showGradingScalePicker) {
        AlertDialog(
            onDismissRequest = { showGradingScalePicker = false },
            title = {
                Text(
                    text = "Select Grading Scale",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    gradingScales.filter { it.isBuiltIn }.forEach { scale ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedGradingScale = scale.scaleType
                                    showGradingScalePicker = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedGradingScale == scale.scaleType,
                                onClick = {
                                    selectedGradingScale = scale.scaleType
                                    showGradingScalePicker = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF007AFF)
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = scale.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1C1C1E)
                                )
                                Text(
                                    text = "${scale.minGrade}-${scale.maxGrade} scale • ${scale.passingGrade}% passing",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF8E8E93)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGradingScalePicker = false }) {
                    Text("CLOSE", color = Color(0xFF007AFF))
                }
            }
        )
    }
    
    // Grade Distribution Preview Dialog
    if (showPreview) {
        currentScale?.let { scale ->
            GradeDistributionPreview(
                scale = scale,
                totalQuestions = exam.totalQuestions,
                onDismiss = { showPreview = false }
            )
        }
    }
}

@Composable
fun GradeDistributionPreview(
    scale: GradingScaleEntity,
    totalQuestions: Int,
    onDismiss: () -> Unit
) {
    val transmutation = try {
        JSONArray(scale.transmutationJson)
    } catch (e: Exception) {
        JSONArray()
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Grade Distribution",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = scale.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8E8E93)
                )
            }
        },
        text = {
            LazyColumn {
                items(transmutation.length()) { index ->
                    val bracket = transmutation.getJSONObject(index)
                    val min = bracket.getInt("min")
                    val max = bracket.getInt("max")
                    val grade = bracket.getString("grade")
                    val level = bracket.getString("level")
                    val colorHex = bracket.getString("color")
                    val color = android.graphics.Color.parseColor(colorHex)
                    
                    // Calculate score range for this bracket
                    val minScore = (min * totalQuestions) / 100
                    val maxScore = (max * totalQuestions) / 100
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(color))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = grade,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1C1C1E)
                            )
                            Text(
                                text = "$min-$max% ($minScore-$maxScore/${totalQuestions})",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF8E8E93)
                            )
                        }
                        Text(
                            text = level,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(color)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = Color(0xFF007AFF))
            }
        }
    )
}
