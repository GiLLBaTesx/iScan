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
            .background(BackgroundWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            FloatingGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = PrimaryBlue
                            )
                        }
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = "Edit Answer Key",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = examName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                    IconButton(
                        onClick = { 
                            onSave(answers)
                            // Save MELC mappings
                            val mappingsToSave = melcMappings.mapValues { it.value.id }
                            onSaveMelcMappings(mappingsToSave)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save",
                            tint = SuccessGreen
                        )
                    }
                }
            }
            
            // Info card
            FloatingGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Tap a letter to set the answer · ${answers.size}/$totalQuestions keyed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    if (availableMelcs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 📖 to tag MELC competency · ${melcMappings.size} tagged",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
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
    FloatingGlassCard(
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
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
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
                            .padding(start = 60.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selectedMelc != null) androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                    else androidx.compose.ui.graphics.Color(0xFFE3F2FD)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selectedMelc != null) androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                    else PrimaryBlue,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onMelcClick() }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Book,
                                    contentDescription = "Tag MELC",
                                    tint = if (selectedMelc != null) androidx.compose.ui.graphics.Color.White
                                    else PrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (selectedMelc != null) "MELC Tagged" else "Tag MELC",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selectedMelc != null) androidx.compose.ui.graphics.Color.White
                                    else PrimaryBlue,
                                    fontWeight = FontWeight.Medium
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
                            androidx.compose.ui.graphics.Color(0xFF4CAF50).copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedMelc.code,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
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
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) PrimaryBlue else SurfaceWhite)
            .border(
                width = 1.dp,
                color = if (isSelected) PrimaryBlue else LightGray,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = answer,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) SurfaceWhite else TextPrimary
        )
    }
}
