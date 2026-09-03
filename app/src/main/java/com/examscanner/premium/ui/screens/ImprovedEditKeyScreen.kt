package com.examscanner.premium.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.examscanner.premium.ui.components.FloatingGlassCard
import com.examscanner.premium.ui.components.GlassmorphicCard
import com.examscanner.premium.ui.theme.*

@Composable
fun ImprovedEditKeyScreen(
    examId: Long,
    examName: String,
    totalQuestions: Int,
    currentAnswers: Map<Int, String>,
    onBack: () -> Unit,
    onSave: (Map<Int, String>) -> Unit
) {
    var answers by remember { mutableStateOf(currentAnswers.toMutableMap()) }
    var showBulkActions by remember { mutableStateOf(false) }
    var showPatternDialog by remember { mutableStateOf(false) }
    var showRangeDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    
    val filledCount = answers.size
    val progress = filledCount.toFloat() / totalQuestions
    
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
                .statusBarsPadding()
        ) {
            // Header with Progress
            FloatingGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFF007AFF)
                                )
                            }
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(
                                    text = "Answer Key",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1C1C1E)
                                )
                                Text(
                                    text = examName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF8E8E93)
                                )
                            }
                        }
                        IconButton(
                            onClick = { onSave(answers) },
                            enabled = filledCount == totalQuestions
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save",
                                tint = if (filledCount == totalQuestions) 
                                    Color(0xFF34C759) 
                                else 
                                    Color(0xFFAEAEB2)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Progress bar
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$filledCount/$totalQuestions completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF8E8E93)
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF007AFF)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF007AFF),
                            trackColor = Color(0xFFE5E5EA)
                        )
                    }
                }
            }
            
            // Bulk Actions Toolbar
            AnimatedVisibility(
                visible = showBulkActions,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                BulkActionsToolbar(
                    onPatternFill = { showPatternDialog = true },
                    onRangeFill = { showRangeDialog = true },
                    onClearAll = { showClearDialog = true },
                    onClose = { showBulkActions = false }
                )
            }
            
            // Quick Actions Card
            if (!showBulkActions) {
                FloatingGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tap a letter to set answer",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF8E8E93)
                        )
                        TextButton(onClick = { showBulkActions = true }) {
                            Icon(
                                Icons.Default.Dashboard,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF007AFF)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Bulk Actions",
                                color = Color(0xFF007AFF),
                                fontSize = MaterialTheme.typography.bodySmall.fontSize
                            )
                        }
                    }
                }
            }
            
            // Answer key list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed((1..totalQuestions).toList()) { index, questionNum ->
                    ImprovedAnswerKeyItem(
                        questionNumber = questionNum,
                        selectedAnswer = answers[questionNum],
                        onAnswerSelected = { answer ->
                            answers = answers.toMutableMap().apply {
                                put(questionNum, answer)
                            }
                        },
                        onClear = {
                            answers = answers.toMutableMap().apply {
                                remove(questionNum)
                            }
                        }
                    )
                }
            }
        }
    }
    
    // Pattern Fill Dialog
    if (showPatternDialog) {
        PatternFillDialog(
            totalQuestions = totalQuestions,
            onDismiss = { showPatternDialog = false },
            onApply = { pattern ->
                answers.clear()
                pattern.forEachIndexed { index, answer ->
                    if (index < totalQuestions) {
                        answers[index + 1] = answer
                    }
                }
                showPatternDialog = false
            }
        )
    }
    
    // Range Fill Dialog
    if (showRangeDialog) {
        RangeFillDialog(
            totalQuestions = totalQuestions,
            onDismiss = { showRangeDialog = false },
            onApply = { start, end, answer ->
                (start..end).forEach { questionNum ->
                    answers[questionNum] = answer
                }
                showRangeDialog = false
            }
        )
    }
    
    // Clear All Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF9500),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Clear All Answers?") },
            text = { Text("This will remove all $filledCount answers. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    answers.clear()
                    showClearDialog = false
                }) {
                    Text("CLEAR", color = Color(0xFFFF3B30))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
fun BulkActionsToolbar(
    onPatternFill: () -> Unit,
    onRangeFill: () -> Unit,
    onClearAll: () -> Unit,
    onClose: () -> Unit
) {
    FloatingGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Bulk Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1C1C1E)
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, "Close", tint = Color(0xFF8E8E93))
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BulkActionButton(
                    icon = Icons.Default.Repeat,
                    label = "Pattern",
                    onClick = onPatternFill,
                    modifier = Modifier.weight(1f)
                )
                BulkActionButton(
                    icon = Icons.Default.Edit,
                    label = "Range",
                    onClick = onRangeFill,
                    modifier = Modifier.weight(1f)
                )
                BulkActionButton(
                    icon = Icons.Default.Clear,
                    label = "Clear",
                    onClick = onClearAll,
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFFF3B30)
                )
            }
        }
    }
}

@Composable
fun BulkActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF007AFF)
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = color
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = MaterialTheme.typography.labelSmall.fontSize)
        }
    }
}

@Composable
fun ImprovedAnswerKeyItem(
    questionNumber: Int,
    selectedAnswer: String?,
    onAnswerSelected: (String) -> Unit,
    onClear: () -> Unit
) {
    FloatingGlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Question number
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selectedAnswer != null)
                            Color(0xFF007AFF).copy(alpha = 0.1f)
                        else
                            Color(0xFFF0F4F8)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$questionNumber",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedAnswer != null)
                        Color(0xFF007AFF)
                    else
                        Color(0xFF8E8E93)
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            // Answer buttons
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("A", "B", "C", "D").forEach { answer ->
                    ImprovedAnswerButton(
                        answer = answer,
                        isSelected = selectedAnswer == answer,
                        onClick = { onAnswerSelected(answer) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Clear button (only show if answered)
            if (selectedAnswer != null) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        "Clear",
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ImprovedAnswerButton(
    answer: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected)
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF007AFF),
                            Color(0xFF0051D5)
                        )
                    )
                else
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.95f),
                            Color.White.copy(alpha = 0.95f)
                        )
                    )
            )
            .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = if (isSelected) Color.Transparent else Color(0xFFE5E5EA),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = answer,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isSelected) Color.White else Color(0xFF1C1C1E)
        )
    }
}

@Composable
fun PatternFillDialog(
    totalQuestions: Int,
    onDismiss: () -> Unit,
    onApply: (List<String>) -> Unit
) {
    var selectedPattern by remember { mutableStateOf("ABCD") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Repeat,
                null,
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(48.dp)
            )
        },
        title = { Text("Pattern Fill") },
        text = {
            Column {
                Text("Select a pattern that will repeat for all questions:")
                
                Spacer(Modifier.height(16.dp))
                
                listOf(
                    "ABCD" to "A, B, C, D, A, B, C, D...",
                    "ABCDABCD" to "A, B, C, D, A, B, C, D...",
                    "AAAA" to "All A",
                    "BBBB" to "All B",
                    "CCCC" to "All C",
                    "DDDD" to "All D"
                ).forEach { (pattern, description) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selectedPattern == pattern)
                                    Color(0xFF007AFF).copy(alpha = 0.1f)
                                else
                                    Color.Transparent
                            )
                            .clickable { selectedPattern = pattern }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPattern == pattern,
                            onClick = { selectedPattern = pattern }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                pattern,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = MaterialTheme.typography.bodyLarge.fontSize
                            )
                            Text(
                                description,
                                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                color = Color(0xFF8E8E93)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val pattern = selectedPattern.toList().map { it.toString() }
                val fullPattern = mutableListOf<String>()
                var patternIndex = 0
                repeat(totalQuestions) {
                    fullPattern.add(pattern[patternIndex % pattern.size])
                    patternIndex++
                }
                onApply(fullPattern)
            }) {
                Text("APPLY")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}

@Composable
fun RangeFillDialog(
    totalQuestions: Int,
    onDismiss: () -> Unit,
    onApply: (Int, Int, String) -> Unit
) {
    var startQuestion by remember { mutableStateOf("1") }
    var endQuestion by remember { mutableStateOf(totalQuestions.toString()) }
    var selectedAnswer by remember { mutableStateOf("A") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Edit,
                null,
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(48.dp)
            )
        },
        title = { Text("Fill Range") },
        text = {
            Column {
                Text("Set the same answer for a range of questions:")
                
                Spacer(Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startQuestion,
                        onValueChange = { startQuestion = it },
                        label = { Text("From") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endQuestion,
                        onValueChange = { endQuestion = it },
                        label = { Text("To") },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                Text("Select answer:", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("A", "B", "C", "D").forEach { answer ->
                        ImprovedAnswerButton(
                            answer = answer,
                            isSelected = selectedAnswer == answer,
                            onClick = { selectedAnswer = answer },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val start = startQuestion.toIntOrNull() ?: 1
                val end = endQuestion.toIntOrNull() ?: totalQuestions
                if (start in 1..totalQuestions && end in 1..totalQuestions && start <= end) {
                    onApply(start, end, selectedAnswer)
                }
            }) {
                Text("APPLY")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}
