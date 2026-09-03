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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.examscanner.premium.data.MelcEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MelcSelectorDialog(
    questionNumber: Int,
    availableMelcs: List<MelcEntity>,
    currentlySelected: MelcEntity?,
    onDismiss: () -> Unit,
    onSelect: (MelcEntity?) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf<String?>(null) }
    var selectedQuarter by remember { mutableStateOf<Int?>(null) }
    
    val subjects = remember(availableMelcs) {
        availableMelcs.map { it.subject }.distinct().sorted()
    }
    
    val quarters = remember(availableMelcs) {
        availableMelcs.map { it.quarter }.distinct().sorted()
    }
    
    val filteredMelcs = remember(availableMelcs, searchQuery, selectedSubject, selectedQuarter) {
        availableMelcs.filter { melc ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                melc.code.contains(searchQuery, ignoreCase = true) ||
                melc.description.contains(searchQuery, ignoreCase = true)
            }
            val matchesSubject = selectedSubject == null || melc.subject == selectedSubject
            val matchesQuarter = selectedQuarter == null || melc.quarter == selectedQuarter
            
            matchesSearch && matchesSubject && matchesQuarter
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E88E5))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Select MELC for Q$questionNumber",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Tag competency for this question",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = Color.White)
                    }
                }
                
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("Search by code or description...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, "Clear")
                            }
                        }
                    },
                    singleLine = true
                )
                
                // Filters
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Subject Filter
                    FilterChipGroup(
                        label = "Subject",
                        options = subjects,
                        selected = selectedSubject,
                        onSelect = { selectedSubject = if (it == selectedSubject) null else it }
                    )
                    
                    // Quarter Filter
                    FilterChipGroup(
                        label = "Quarter",
                        options = quarters.map { "Q$it" },
                        selected = selectedQuarter?.let { "Q$it" },
                        onSelect = { 
                            selectedQuarter = if (it == selectedQuarter?.let { "Q$it" }) null 
                            else it.removePrefix("Q").toIntOrNull()
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Result count
                Text(
                    "${filteredMelcs.size} competenc${if (filteredMelcs.size == 1) "y" else "ies"} found",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                
                Divider()
                
                // MELC List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Clear selection option
                    if (currentlySelected != null) {
                        item {
                            MelcCard(
                                melc = null,
                                isSelected = false,
                                onClick = {
                                    onSelect(null)
                                    onDismiss()
                                }
                            )
                        }
                    }
                    
                    items(filteredMelcs) { melc ->
                        MelcCard(
                            melc = melc,
                            isSelected = currentlySelected?.id == melc.id,
                            onClick = {
                                onSelect(melc)
                                onDismiss()
                            }
                        )
                    }
                }
                
                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChipGroup(
    label: String,
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        FilterChip(
            selected = selected != null,
            onClick = { expanded = true },
            label = { 
                Text(
                    if (selected != null) "$label: $selected" else "All ${label}s",
                    fontSize = 12.sp
                ) 
            },
            leadingIcon = {
                Icon(
                    if (selected != null) Icons.Default.Check else Icons.Default.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        )
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All ${label}s") },
                onClick = {
                    onSelect("")
                    expanded = false
                }
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun MelcCard(
    melc: MelcEntity?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1E88E5).copy(alpha = 0.1f) 
            else Color.White
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder() else null,
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        if (melc == null) {
            // Clear selection option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = null,
                    tint = Color(0xFFF44336),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Remove MELC tagging",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFF44336)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            melc.code,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E88E5)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            melc.description,
                            fontSize = 14.sp,
                            color = Color(0xFF212121),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Chip(
                        text = melc.subject,
                        color = Color(0xFF2196F3)
                    )
                    Chip(
                        text = "${melc.gradeLevel}",
                        color = Color(0xFF9C27B0)
                    )
                    Chip(
                        text = "Q${melc.quarter}",
                        color = Color(0xFFFF9800)
                    )
                }
            }
        }
    }
}

@Composable
fun Chip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text,
            fontSize = 11.sp,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
