package com.examscanner.premium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.examscanner.premium.data.MelcEntity
import com.examscanner.premium.ui.components.FloatingGlassCard
import com.examscanner.premium.ui.theme.*

/**
 * Map Questions to MELCs Screen
 * 
 * Allows teachers to tag each question with a MELC competency
 * This enables competency-based analysis after scanning
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapQuestionsToMelcScreen(
    examId: Long,
    totalQuestions: Int,
    availableMelcs: List<MelcEntity>,
    existingMappings: Map<Int, MelcEntity>,
    onBack: () -> Unit,
    onSaveMappings: (Map<Int, Long>) -> Unit
) {
    var mappings by remember { mutableStateOf(existingMappings.toMutableMap()) }
    var showMelcPicker by remember { mutableStateOf(false) }
    var selectedQuestion by remember { mutableStateOf<Int?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredMelcs = remember(availableMelcs, searchQuery) {
        if (searchQuery.isBlank()) {
            availableMelcs
        } else {
            availableMelcs.filter {
                it.description.contains(searchQuery, ignoreCase = true) ||
                it.code.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    val mappedCount = mappings.size
    val progress = if (totalQuestions > 0) mappedCount.toFloat() / totalQuestions else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Map to Competencies",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$mappedCount of $totalQuestions mapped",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val melcMappings = mappings.mapValues { it.value.id }
                            onSaveMappings(melcMappings)
                            onBack()
                        },
                        enabled = mappings.isNotEmpty()
                    ) {
                        Text(
                            "SAVE",
                            color = if (mappings.isNotEmpty()) PrimaryBlue else TextTertiary,
                            fontWeight = FontWeight.Bold
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
        ) {
            // Progress Indicator
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (progress == 1f) LightBlue else SurfaceWhite
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (progress == 1f) "All questions mapped! 🎉" else "Progress",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (progress == 1f) SuccessGreen else TextPrimary
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (progress == 1f) SuccessGreen else PrimaryBlue
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = if (progress == 1f) SuccessGreen else PrimaryBlue
                    )
                }
            }

            // Question List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed((1..totalQuestions).toList()) { _, questionNumber ->
                    QuestionMappingCard(
                        questionNumber = questionNumber,
                        mappedMelc = mappings[questionNumber],
                        onMapClick = {
                            selectedQuestion = questionNumber
                            showMelcPicker = true
                        },
                        onClearClick = {
                            mappings = mappings.toMutableMap().apply { remove(questionNumber) }
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // MELC Picker Dialog
        if (showMelcPicker && selectedQuestion != null) {
            AlertDialog(
                onDismissRequest = { showMelcPicker = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = SurfaceWhite
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Select Competency for Q${selectedQuestion}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { showMelcPicker = false }) {
                                Text("CANCEL")
                            }
                        }

                        // Search
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            placeholder = { Text("Search by code or description...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            },
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // MELC List
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            items(filteredMelcs.groupBy { "${it.gradeLevel} - ${it.subject}" }.entries.toList()) { (header, melcs) ->
                                // Group Header
                                Text(
                                    text = header,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )

                                // MELCs in this group
                                melcs.forEach { melc ->
                                    MelcListItem(
                                        melc = melc,
                                        isSelected = mappings[selectedQuestion]?.id == melc.id,
                                        onClick = {
                                            mappings = mappings.toMutableMap().apply {
                                                put(selectedQuestion!!, melc)
                                            }
                                            // Save immediately to database
                                            val melcMappingsToSave = mappings.mapValues { it.value.id }
                                            onSaveMappings(melcMappingsToSave)
                                            
                                            showMelcPicker = false
                                            selectedQuestion = null
                                            searchQuery = ""
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuestionMappingCard(
    questionNumber: Int,
    mappedMelc: MelcEntity?,
    onMapClick: () -> Unit,
    onClearClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (mappedMelc != null) LightBlue else SurfaceWhite
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (mappedMelc != null) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "Question $questionNumber",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                if (mappedMelc != null) {
                    TextButton(onClick = onClearClick) {
                        Text("CLEAR", color = ErrorRed)
                    }
                } else {
                    Button(
                        onClick = onMapClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue
                        )
                    ) {
                        Text("Map to MELC")
                    }
                }
            }

            if (mappedMelc != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = LightGray)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = mappedMelc.code,
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = mappedMelc.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text(
                    text = "${mappedMelc.gradeLevel} • ${mappedMelc.subject} • Q${mappedMelc.quarter}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun MelcListItem(
    melc: MelcEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PrimaryBlue.copy(alpha = 0.1f) else SurfaceWhite
        ),
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = melc.code,
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = melc.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
                Text(
                    text = "Quarter ${melc.quarter}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = SuccessGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
