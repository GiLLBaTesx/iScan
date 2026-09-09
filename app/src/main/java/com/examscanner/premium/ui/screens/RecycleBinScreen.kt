package com.examscanner.premium.ui.screens

import androidx.compose.foundation.background
import com.examscanner.premium.ui.theme.*
import androidx.compose.foundation.clickable
import com.examscanner.premium.ui.theme.*
import androidx.compose.foundation.layout.*
import com.examscanner.premium.ui.theme.*
import androidx.compose.foundation.lazy.LazyColumn
import com.examscanner.premium.ui.theme.*
import androidx.compose.foundation.lazy.items
import com.examscanner.premium.ui.theme.*
import androidx.compose.material.icons.Icons
import com.examscanner.premium.ui.theme.*
import androidx.compose.material.icons.filled.*
import com.examscanner.premium.ui.theme.*
import androidx.compose.material3.*
import com.examscanner.premium.ui.theme.*
import androidx.compose.runtime.*
import com.examscanner.premium.ui.theme.*
import androidx.compose.ui.Alignment
import com.examscanner.premium.ui.theme.*
import androidx.compose.ui.Modifier
import com.examscanner.premium.ui.theme.*
import androidx.compose.ui.graphics.Brush
import com.examscanner.premium.ui.theme.*
import androidx.compose.ui.graphics.Color
import com.examscanner.premium.ui.theme.*
import androidx.compose.ui.text.font.FontWeight
import com.examscanner.premium.ui.theme.*
import androidx.compose.ui.unit.dp
import com.examscanner.premium.ui.theme.*
import com.examscanner.premium.data.ExamEntity
import com.examscanner.premium.ui.components.FloatingGlassCard
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    deletedExams: List<ExamEntity>,
    onBack: () -> Unit,
    onRestore: (ExamEntity) -> Unit,
    onPermanentDelete: (ExamEntity) -> Unit,
    onEmptyRecycleBin: () -> Unit
) {
    var showEmptyDialog by remember { mutableStateOf(false) }
    var examToDelete by remember { mutableStateOf<ExamEntity?>(null) }
    
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
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Recycle Bin",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${deletedExams.size} deleted items",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = ElectricBlue
                        )
                    }
                },
                actions = {
                    if (deletedExams.isNotEmpty()) {
                        IconButton(onClick = { showEmptyDialog = true }) {
                            Icon(
                                Icons.Default.DeleteForever,
                                contentDescription = "Empty Recycle Bin",
                                tint = Color(0xFFFF3B30)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFAFAFC)
                )
            )
            
            if (deletedExams.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFF8E8E93),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Recycle Bin is Empty",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1C1E)
                        )
                        Text(
                            text = "Deleted exams will appear here.\nYou have 30 days to restore them.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF8E8E93)
                        )
                    }
                }
            } else {
                // Info card
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFF9500).copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFFF9500)
                            )
                            Text(
                                text = "Items are kept for 30 days, then automatically deleted.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1C1C1E)
                            )
                        }
                    }
                    
                    // List of deleted items
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(deletedExams) { exam ->
                            DeletedExamCard(
                                exam = exam,
                                onRestore = { onRestore(exam) },
                                onPermanentDelete = { examToDelete = exam }
                            )
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                }
            }
        }
    }
    
    // Empty recycle bin confirmation
    if (showEmptyDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyDialog = false },
            icon = {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = Color(0xFFFF3B30),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Empty Recycle Bin?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("This will permanently delete all ${deletedExams.size} items. This action cannot be undone!")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEmptyDialog = false
                        onEmptyRecycleBin()
                    }
                ) {
                    Text("DELETE ALL", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
    
    // Permanent delete confirmation
    examToDelete?.let { exam ->
        AlertDialog(
            onDismissRequest = { examToDelete = null },
            icon = {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = Color(0xFFFF3B30),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Delete Permanently?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("\"${exam.name}\" will be permanently deleted. This action cannot be undone!")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onPermanentDelete(exam)
                        examToDelete = null
                    }
                ) {
                    Text("DELETE", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { examToDelete = null }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
private fun DeletedExamCard(
    exam: ExamEntity,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    FloatingGlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exam.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1C1C1E)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${exam.totalQuestions} questions",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8E8E93)
                    )
                    exam.deletedAt?.let { timestamp ->
                        Text(
                            text = "Deleted ${formatRelativeTime(timestamp)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF3B30)
                        )
                    }
                }
                
                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Restore button
                    IconButton(
                        onClick = onRestore,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Restore,
                            contentDescription = "Restore",
                            tint = Color(0xFF34C759),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Delete permanently button
                    IconButton(
                        onClick = onPermanentDelete,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteForever,
                            contentDescription = "Delete Permanently",
                            tint = Color(0xFFFF3B30),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
        hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
        minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
        else -> "Just now"
    }
}
