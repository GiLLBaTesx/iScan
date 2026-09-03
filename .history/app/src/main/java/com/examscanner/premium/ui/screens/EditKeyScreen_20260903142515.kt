package com.examscanner.premium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.examscanner.premium.data.MelcEntity
import com.examscanner.premium.ui.components.FloatingGlassCard
import com.examscanner.premium.ui.theme.*

@Composable
fun EditKeyScreen(
    examId: Long,
    examName: String,
    totalQuestions: Int,
    currentAnswers: Map<Int, String>,
    availableMelcs: List<MelcEntity> = emptyList(),
    currentMelcMappings: Map<Int, MelcEntity> = emptyMap(),
    onBack: () -> Unit,
    onSave: (Map<Int, String>) -> Unit,
    onSaveMelcMappings: (Map<Int, Long>) -> Unit = {}
) {
    var answers by remember { mutableStateOf(currentAnswers.toMutableMap()) }
    var melcMappings by remember { mutableStateOf(currentMelcMappings.toMutableMap()) }
    var showMelcDialog by remember { mutableStateOf<Int?>(null) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IceWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header - ScanKey Style
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = IceWhite,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = ElectricBlue
                            )
                        }
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = "EDIT ANSWER KEY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ElectricBlue,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = examName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    IconButton(
                        onClick = { 
                            onSave(answers)
                            val mappingsToSave = melcMappings.mapValues { it.value.id }
                            onSaveMelcMappings(mappingsToSave)
                            onBack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save",
                            tint = IcyCyan
                        )
                    }
                }
            }
            
            // Info card - ScanKey Style
            com.examscanner.premium.ui.components.FrostedGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Tap a letter to set the answer · ${answers.size}/$totalQuestions keyed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    if (availableMelcs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 📖 to tag MELC competency · ${melcMappings.size} tagged",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Answer key list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed((1..totalQuestions).toList()) { index, questionNum ->
                    AnswerKeyItem(
                        questionNumber = questionNum,
                        selectedAnswer = answers[questionNum],
                        selectedMelc = melcMappings[questionNum],
                        hasMelcs = availableMelcs.isNotEmpty(),
                        onAnswerSelected = { answer ->
                            answers = answers.toMutableMap().apply {
                                put(questionNum, answer)
                            }
                        },
                        onMelcClick = {
                            showMelcDialog = questionNum
                        }
                    )
                }
            }
        }
        
        // MELC Selector Dialog
        showMelcDialog?.let { questionNum ->
            MelcSelectorDialog(
                questionNumber = questionNum,
                availableMelcs = availableMelcs,
                currentlySelected = melcMappings[questionNum],
                onDismiss = { showMelcDialog = null },
                onSelect = { melc ->
                    melcMappings = melcMappings.toMutableMap().apply {
                        if (melc != null) {
                            put(questionNum, melc)
                        } else {
                            remove(questionNum)
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun AnswerKeyItem(
    questionNumber: Int,
    selectedAnswer: String?,
    selectedMelc: MelcEntity? = null,
    hasMelcs: Boolean = false,
    onAnswerSelected: (String) -> Unit,
    onMelcClick: () -> Unit = {}
) {
    com.examscanner.premium.ui.components.FrostedGlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Question number and answer buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Q$questionNumber",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(60.dp)
                    )
                    
                    // Answer buttons row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("A", "B", "C", "D", "E").forEach { answer ->
                            AnswerButton(
                                answer = answer,
                                isSelected = selectedAnswer == answer,
                                onClick = { onAnswerSelected(answer) }
                            )
                        }
                    }
                }
                
                // MELC button row (below answer buttons)
                if (hasMelcs) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (selectedMelc != null) {
                                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            colors = listOf(
                                                IcyCyan,
                                                IcyCyan.copy(alpha = 0.8f)
                                            )
                                        )
                                    } else {
                                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                                            colors = listOf(
                                                GlassBase,
                                                GlassBase
                                            )
                                        )
                                    }
                                )
                                .border(
                                    width = if (selectedMelc != null) 0.dp else 1.dp,
                                    color = if (selectedMelc != null) androidx.compose.ui.graphics.Color.Transparent else ElectricBlue.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { onMelcClick() }
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Book,
                                    contentDescription = "Tag MELC",
                                    tint = if (selectedMelc != null) 
                                        androidx.compose.ui.graphics.Color.White
                                    else 
                                        ElectricBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (selectedMelc != null) "MELC Tagged ✓" else "Tag MELC",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selectedMelc != null) 
                                        androidx.compose.ui.graphics.Color.White
                                    else 
                                        ElectricBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
            
            // Show selected MELC
            if (selectedMelc != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            IcyCyan.copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedMelc.code,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = IcyCyan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun AnswerButton(
    answer: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Multi-color answer buttons (ScanKey style)
    val buttonColor = when (answer) {
        "A" -> AnswerA
        "B" -> AnswerB
        "C" -> AnswerC
        "D" -> AnswerD
        "E" -> AnswerE
        else -> ElectricBlue
    }
    
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) buttonColor else GlassBase
            )
            .border(
                width = 2.dp,
                color = if (isSelected) buttonColor else buttonColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = answer,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) androidx.compose.ui.graphics.Color.White else buttonColor
        )
    }
}
