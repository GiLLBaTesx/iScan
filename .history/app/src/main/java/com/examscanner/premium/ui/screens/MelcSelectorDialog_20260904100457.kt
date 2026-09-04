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
import com.examscanner.premium.ui.theme.*

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
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Electric Blue Header (matches your screenshot)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ElectricBlue)
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Select Competency",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Question $questionNumber",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close, 
                                "Close", 
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                
                // Content area with light background
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF5F5F5))
                        .padding(16.dp)
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { 
                            Text(
                                "Search by code or description...",
                                color = Color.Gray,
                                fontSize = 14.sp
                            ) 
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search, 
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, "Clear", tint = Color.Gray)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = Color.LightGray,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Filters
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Subject Filter
                        FilterButton(
                            label = if (selectedSubject != null) selectedSubject!! else "All Subjects",
                            isActive = selectedSubject != null,
                            onClick = { selectedSubject = null }
                        )
                        
                        // Quarter Filter  
                        FilterButton(
                            label = if (selectedQuarter != null) "Q$selectedQuarter" else "All Quarters",
                            isActive = selectedQuarter != null,
                            onClick = { selectedQuarter = null }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Result count
                    Text(
                        "${filteredMelcs.size} competencies found",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // MELC List
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Clear selection option (red X)
                        if (currentlySelected != null) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelect(null)
                                            onDismiss()
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFFFF3F3)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = null,
                                            tint = Color(0xFFEF5350),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            "Remove MELC tagging",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFEF5350)
                                        )
                                    }
                                }
                            }
                        }
                        
                        items(filteredMelcs) { melc ->
                            MelcItemCard(
                                melc = melc,
                                isSelected = currentlySelected?.id == melc.id,
                                onClick = {
                                    onSelect(melc)
                                    onDismiss()
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Cancel Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                "CANCEL",
                                color = ElectricBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterButton(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isActive) ElectricBlue.copy(alpha = 0.1f) else Color.White,
            contentColor = if (isActive) ElectricBlue else Color.Gray
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isActive) ElectricBlue else Color.LightGray
            )
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            Icons.Default.FilterList,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 12.sp)
    }
}

@Composable
fun MelcItemCard(
    melc: MelcEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE3F2FD) else Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) 
            androidx.compose.foundation.BorderStroke(2.dp, ElectricBlue) 
            else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    melc.code,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricBlue
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    melc.description,
                    fontSize = 13.sp,
                    color = Color(0xFF424242),
                    lineHeight = 18.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Tags (matches your screenshot colors)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CompetencyChip(melc.subject, Color(0xFF2196F3)) // Blue
                    CompetencyChip("Grade ${melc.gradeLevel}", Color(0xFF9C27B0)) // Purple
                    CompetencyChip("Q${melc.quarter}", Color(0xFFFF9800)) // Orange
                }
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
    }
}

@Composable
fun CompetencyChip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text,
            fontSize = 10.sp,
            color = color,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}
