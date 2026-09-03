package com.examscanner.premium.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.examscanner.premium.data.ExamEntity
import com.examscanner.premium.data.GradingScaleEntity
import com.examscanner.premium.ui.components.GlassmorphicCard
import com.examscanner.premium.ui.components.PrimaryActionCard
import com.examscanner.premium.ui.components.StatCard
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
    val hasChanges = selectedGradingScale != exam.gradingScale ||
            passingGrade.toIntOrNull() != exam.passingGrade ||
            useNegativeMarking != exam.useNegativeMarking ||
            negativeMarkValue.toFloatOrNull() != exam.negativeMarkValue
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8F9FD),
                        Color(0xFFEEF2F8),
                        Color(0xFFE8EEF7)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Enhanced Header
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                cornerRadius = 28.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF007AFF).copy(alpha = 0.1f))
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF007AFF)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Exam Settings",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1C1E)
                        )
                        Text(
                            text = exam.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF8E8E93)
                        )
                    }
                    if (hasChanges) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFF9500))
                        )
                    }
                }
            }
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Grading Scale Section
                item {
                    SectionHeader(
                        icon = Icons.Default.Grade,
                        title = "GRADING SCALE",
                        subtitle = "Choose how students are graded"
                    )
                }
                
                item {
                    GradingScaleCard(
                        currentScale = currentScale,
                        onClick = { showGradingScalePicker = true },
                        onPreview = { showPreview = true }
                    )
                }
                
                // Passing Criteria Section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(
                        icon = Icons.Default.CheckCircle,
                        title = "PASSING CRITERIA",
                        subtitle = "Set minimum grade to pass"
                    )
                }
                
                item {
                    PassingGradeCard(
                        passingGrade = passingGrade,
                        onPassingGradeChange = { 
                            if (it.isEmpty() || (it.toIntOrNull() != null && it.toInt() in 0..100)) {
                                passingGrade = it
                            }
                        }
                    )
                }
                
                // Scoring Options Section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(
                        icon = Icons.Default.Calculate,
                        title = "SCORING OPTIONS",
                        subtitle = "Advanced scoring rules"
                    )
                }
                
                item {
                    NegativeMarkingCard(
                        useNegativeMarking = useNegativeMarking,
                        negativeMarkValue = negativeMarkValue,
                        onToggle = { useNegativeMarking = it },
                        onValueChange = { 
                            if (it.isEmpty() || it.toFloatOrNull() != null) {
                                negativeMarkValue = it
                            }
                        }
                    )
                }
                
                // Spacer for bottom button
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
            
            // Floating Save Button
            AnimatedVisibility(
                visible = hasChanges,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFFF8F9FD).copy(alpha = 0.95f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    PrimaryActionCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple(color = Color.White),
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
                                }
                            )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "SAVE SETTINGS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Dialogs
    if (showGradingScalePicker) {
        GradingScalePickerDialog(
            gradingScales = gradingScales,
            selectedScale = selectedGradingScale,
            onSelect = { selectedGradingScale = it },
            onDismiss = { showGradingScalePicker = false }
        )
    }
    
    if (showPreview) {
        currentScale?.let {
            GradeDistributionPreview(
                scale = it,
                totalQuestions = exam.totalQuestions,
                onDismiss = { showPreview = false }
            )
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF007AFF).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E),
                letterSpacing = 0.5.sp
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E8E93)
            )
        }
    }
}

@Composable
private fun GradingScaleCard(
    currentScale: GradingScaleEntity?,
    onClick: () -> Unit,
    onPreview: () -> Unit
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        withGradient = true
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(),
                        onClick = onClick
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Grading System",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF8E8E93)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentScale?.name ?: "DepEd K-12",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E)
                    )
                    currentScale?.let {
                        Text(
                            text = "${it.minGrade}-${it.maxGrade} scale • ${it.passingGrade}% passing",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF007AFF)
                        )
                    }
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF007AFF)
                )
            }
            
            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color(0xFFE5E5EA)
            )
            
            // Preview Button
            TextButton(
                onClick = onPreview,
                modifier = Modifier.fillMaxWidth()
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
}

@Composable
private fun PassingGradeCard(
    passingGrade: String,
    onPassingGradeChange: (String) -> Unit
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Passing Grade",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1C1C1E)
                )
                Text(
                    text = "${passingGrade.ifBlank { "75" }}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF007AFF)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Slider(
                value = passingGrade.toFloatOrNull() ?: 75f,
                onValueChange = { onPassingGradeChange(it.toInt().toString()) },
                valueRange = 50f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF007AFF),
                    activeTrackColor = Color(0xFF007AFF),
                    inactiveTrackColor = Color(0xFFE5E5EA)
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Students scoring ${passingGrade.ifBlank { "75" }}% or above will pass this exam",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E8E93)
            )
        }
    }
}

@Composable
private fun NegativeMarkingCard(
    useNegativeMarking: Boolean,
    negativeMarkValue: String,
    onToggle: (Boolean) -> Unit,
    onValueChange: (String) -> Unit
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = null,
                    tint = if (useNegativeMarking) Color(0xFFFF3B30) else Color(0xFF8E8E93),
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
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF007AFF),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFE5E5EA)
                    )
                )
            }
            
            AnimatedVisibility(
                visible = useNegativeMarking,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(20.dp))
                    Divider(color = Color(0xFFE5E5EA))
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    OutlinedTextField(
                        value = negativeMarkValue,
                        onValueChange = onValueChange,
                        label = { Text("Deduction per wrong answer") },
                        placeholder = { Text("0.25") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF007AFF),
                            focusedLabelColor = Color(0xFF007AFF),
                            cursorColor = Color(0xFF007AFF)
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFF9500).copy(alpha = 0.1f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFFF9500),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Common: -0.25 for NAT format exams",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8E8E93)
                        )
                    }
                }
            }
        }
    }
}

// Keep existing dialog composables but improve styling
@Composable
private fun GradingScalePickerDialog(
    gradingScales: List<GradingScaleEntity>,
    selectedScale: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Grading Scale",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            LazyColumn {
                items(gradingScales.filter { it.isBuiltIn }) { scale ->
                    GlassmorphicCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                onSelect(scale.scaleType)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedScale == scale.scaleType,
                                onClick = {
                                    onSelect(scale.scaleType)
                                    onDismiss()
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
                                    fontWeight = FontWeight.SemiBold,
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
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

// Keep GradeDistributionPreview but optimize
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
