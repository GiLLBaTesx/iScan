package com.examscanner.premium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.examscanner.premium.ui.components.FloatingGlassCard
import com.examscanner.premium.ui.theme.*

/**
 * Template Generator Screen - ZipGrade-style
 * 
 * Features:
 * - Custom question count (auto-splits into 2 columns)
 * - Editable choices per question (2-6: A-F)
 * - Compact layout like ZipGrade
 * - PDF generation
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateGeneratorScreen(
    onBack: () -> Unit,
    onGenerate: (totalQuestions: Int, choicesPerQuestion: Int, templateName: String) -> Unit
) {
    var templateName by remember { mutableStateOf("Custom Template") }
    var totalQuestions by remember { mutableStateOf("20") }
    var choicesPerQuestion by remember { mutableStateOf("4") }
    var showPreview by remember { mutableStateOf(false) }
    
    val questionsInt = totalQuestions.toIntOrNull() ?: 20
    val choicesInt = choicesPerQuestion.toIntOrNull() ?: 4
    val questionsPerColumn = (questionsInt + 1) / 2 // Ceiling division for even split
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Create Answer Sheet",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
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
                .verticalScroll(rememberScrollState())
        ) {
            // Instructions Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = LightBlue
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning, // Use Info icon
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Compact Design",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Questions are split evenly into 2 columns. Fits more on one page!",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Settings Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "Template Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    // Template Name
                    OutlinedTextField(
                        value = templateName,
                        onValueChange = { if (it.length <= 50) templateName = it },
                        label = { Text("Template Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Number of Questions
                    OutlinedTextField(
                        value = totalQuestions,
                        onValueChange = { 
                            if (it.isEmpty() || (it.toIntOrNull() != null && it.toInt() in 1..100)) {
                                totalQuestions = it
                            }
                        },
                        label = { Text("Number of Questions") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        supportingText = {
                            Text("1-100 questions (splits into 2 columns automatically)")
                        }
                    )

                    // Choices per Question
                    Column {
                        Text(
                            text = "Choices per Question",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("2" to "A-B", "3" to "A-C", "4" to "A-D", "5" to "A-E", "6" to "A-F").forEach { (num, label) ->
                                FilterChip(
                                    selected = choicesPerQuestion == num,
                                    onClick = { choicesPerQuestion = num },
                                    label = { Text(label) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Layout Info
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = questionsPerColumn.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                        Text(
                            text = "Questions per Column",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                    Divider(
                        modifier = Modifier
                            .width(1.dp)
                            .height(60.dp),
                        color = LightGray
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "2",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                        Text(
                            text = "Columns",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preview Toggle
            Button(
                onClick = { showPreview = !showPreview },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showPreview) WarningOrange else PrimaryBlue.copy(alpha = 0.1f),
                    contentColor = if (showPreview) SurfaceWhite else PrimaryBlue
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (showPreview) "Hide Preview" else "Show Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Preview
            if (showPreview) {
                Spacer(modifier = Modifier.height(16.dp))
                TemplatePreviewCard(
                    questionsPerColumn = questionsPerColumn,
                    totalQuestions = questionsInt,
                    choicesPerQuestion = choicesInt
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Generate Button
            Button(
                onClick = { onGenerate(questionsInt, choicesInt, templateName) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                shape = RoundedCornerShape(16.dp),
                enabled = templateName.isNotBlank() && questionsInt in 1..100
            ) {
                Text(
                    text = "Generate PDF Template",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun TemplatePreviewCard(
    questionsPerColumn: Int,
    totalQuestions: Int,
    choicesPerQuestion: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Preview (Scaled Down)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Two-column layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Column 1
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (q in 1..minOf(questionsPerColumn, totalQuestions)) {
                        QuestionPreviewRow(q, choicesPerQuestion)
                    }
                }

                // Divider
                Divider(
                    modifier = Modifier
                        .width(1.dp)
                        .height(200.dp),
                    color = LightGray
                )

                // Column 2
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (q in (questionsPerColumn + 1)..totalQuestions) {
                        QuestionPreviewRow(q, choicesPerQuestion)
                    }
                }
            }
        }
    }
}

@Composable
fun QuestionPreviewRow(questionNumber: Int, choicesPerQuestion: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = String.format("%2d", questionNumber),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.width(20.dp),
            fontSize = 10.sp
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            repeat(choicesPerQuestion) { index ->
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .border(1.dp, LightGray, RoundedCornerShape(2.dp))
                        .background(SurfaceWhite, RoundedCornerShape(2.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ('A' + index).toString(),
                        fontSize = 8.sp,
                        color = TextTertiary
                    )
                }
            }
        }
    }
}
