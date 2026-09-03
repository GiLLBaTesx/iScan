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
import com.examscanner.premium.data.ExamWithStats
import com.examscanner.premium.ui.components.FloatingGlassCard
import com.examscanner.premium.ui.components.GlassCard
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderExamListScreen(
    folderName: String,
    exams: List<ExamWithStats>,
    onBack: () -> Unit,
    onExamClick: (ExamWithStats) -> Unit,
    onNewExamClick: () -> Unit,
    onEditExam: (ExamWithStats, String) -> Unit = { _, _ -> },
    onDeleteExam: (ExamWithStats) -> Unit = {}
) {
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
            // Header with Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
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
                        text = folderName,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E)
                    )
                    Text(
                        text = "${exams.size} exam${if (exams.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8E8E93)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Stats Card
            if (exams.isNotEmpty()) {
                val totalScanned = exams.sumOf { it.scannedCount }
                val avgScore = exams.mapNotNull { it.averageScore }.average().takeIf { it.isFinite() }?.toInt()
                
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        FolderStatItem(
                            value = totalScanned.toString(),
                            label = "Scanned",
                            icon = Icons.Default.Scanner
                        )
                        FolderStatItem(
                            value = exams.size.toString(),
                            label = "Exams",
                            icon = Icons.Default.Assignment
                        )
                        FolderStatItem(
                            value = avgScore?.toString() ?: "-",
                            label = "Average",
                            icon = Icons.Default.TrendingUp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // New Exam Button
            FloatingGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNewExamClick)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "NEW EXAM",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF007AFF)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Recent Exams Section
            if (exams.isEmpty()) {
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
                            text = "No exams yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF8E8E93)
                        )
                        Text(
                            text = "Create your first exam",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFAEAEB2)
                        )
                    }
                }
            } else {
                Text(
                    text = "Recent Exams",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1C1C1E)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(exams) { examWithStats ->
                        ExamCard(
                            examWithStats = examWithStats,
                            onClick = { onExamClick(examWithStats) },
                            onEdit = { newName ->
                                onEditExam(examWithStats, newName)
                            },
                            onDelete = {
                                onDeleteExam(examWithStats)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderStatItem(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF007AFF),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1C1C1E)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF8E8E93)
        )
    }
}

@Composable
private fun ExamCard(
    examWithStats: ExamWithStats,
    onClick: () -> Unit,
    onEdit: (String) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val dateStr = dateFormat.format(Date(examWithStats.exam.createdAt))
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    FloatingGlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Exam Icon (clickable for navigation)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF007AFF).copy(alpha = 0.1f))
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Exam Info (clickable for navigation)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick)
            ) {
                Text(
                    text = examWithStats.exam.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1C1C1E)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${examWithStats.exam.totalQuestions} questions • $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8E93)
                )
                if (examWithStats.scannedCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF34C759),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${examWithStats.scannedCount} scanned",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF34C759)
                        )
                        examWithStats.averageScore?.let { avg ->
                            Text(
                                text = " • $avg% avg",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF8E8E93)
                            )
                        }
                    }
                }
            }
            
            // Edit Button
            IconButton(onClick = { showEditDialog = true }) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit exam",
                    tint = Color(0xFF007AFF)
                )
            }
            
            // Delete Button
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete exam",
                    tint = Color(0xFFFF3B30)
                )
            }
        }
    }
    
    // Edit Dialog
    if (showEditDialog) {
        EditExamDialog(
            currentName = examWithStats.exam.name,
            onDismiss = { showEditDialog = false },
            onSave = { newName ->
                showEditDialog = false
                onEdit(newName)
            }
        )
    }
    
    // Delete Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF3B30),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Delete Exam?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to delete \"${examWithStats.exam.name}\"?\n\nAll student results and answer keys will be permanently deleted.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("DELETE", color = Color(0xFFFF3B30))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("CANCEL", color = Color(0xFF8E8E93))
                }
            }
        )
    }
}
