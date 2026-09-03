package com.examscanner.premium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.examscanner.premium.data.ExamEntity
import com.examscanner.premium.data.SectionEntity
import com.examscanner.premium.data.SubjectFolderEntity
import com.examscanner.premium.ui.components.FloatingGlassCard
import com.examscanner.premium.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    deletedExams: List<ExamEntity>,
    onBack: () -> Unit,
    onRestore: (ExamEntity) -> Unit,
    onPermanentDelete: (ExamEntity) -> Unit,
    onEmptyRecycleBin: () -> Unit,
    deletedSubjects: List<SubjectFolderEntity> = emptyList(),
    deletedSections: List<SectionEntity> = emptyList(),
    onRestoreSubject: (SubjectFolderEntity) -> Unit = {},
    onPermanentDeleteSubject: (SubjectFolderEntity) -> Unit = {},
    onRestoreSection: (SectionEntity) -> Unit = {},
    onPermanentDeleteSection: (SectionEntity) -> Unit = {}
) {
    var showEmptyDialog by remember { mutableStateOf(false) }
    var examToDelete by remember { mutableStateOf<ExamEntity?>(null) }
    var subjectToDelete by remember { mutableStateOf<SubjectFolderEntity?>(null) }
    var sectionToDelete by remember { mutableStateOf<SectionEntity?>(null) }

    val totalDeletedCount = deletedExams.size + deletedSubjects.size + deletedSections.size
    val isEmpty = totalDeletedCount == 0
    
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
                            text = "$totalDeletedCount deleted items",
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
                                contentDescription = "Permanently delete all deleted exams",
                                tint = ErrorCoral
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFAFAFC)
                )
            )
            
            if (isEmpty) {
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
                            tint = TextTertiaryIce,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Recycle Bin is Empty",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryIce
                        )
                        Text(
                            text = "Deleted subjects, sections, and exams will appear here.\nYou have 30 days to restore them.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiaryIce
                        )
                    }
                }
            } else {
                // Info card + grouped lists
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = WarningAmber.copy(alpha = 0.1f)
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
                                tint = WarningAmber
                            )
                            Text(
                                text = "Items are kept for 30 days, then automatically deleted.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimaryIce
                            )
                        }
                    }

                    // Grouped list of deleted items: subjects, sections, then exams
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (deletedSubjects.isNotEmpty()) {
                            item { RecycleBinSectionLabel("Subjects (${deletedSubjects.size})") }
                            items(deletedSubjects) { subject ->
                                DeletedItemCard(
                                    icon = Icons.Default.Folder,
                                    title = subject.name,
                                    subtitle = "Subject folder",
                                    deletedAt = subject.deletedAt,
                                    restoreDescription = "Restore subject ${subject.name}",
                                    deleteDescription = "Permanently delete subject ${subject.name}",
                                    onRestore = { onRestoreSubject(subject) },
                                    onPermanentDelete = { subjectToDelete = subject }
                                )
                            }
                        }

                        if (deletedSections.isNotEmpty()) {
                            item { RecycleBinSectionLabel("Sections (${deletedSections.size})") }
                            items(deletedSections) { section ->
                                DeletedItemCard(
                                    icon = Icons.Default.Groups,
                                    title = section.name,
                                    subtitle = "Section • capacity ${section.capacity}",
                                    deletedAt = section.deletedAt,
                                    restoreDescription = "Restore section ${section.name}",
                                    deleteDescription = "Permanently delete section ${section.name}",
                                    onRestore = { onRestoreSection(section) },
                                    onPermanentDelete = { sectionToDelete = section }
                                )
                            }
                        }

                        if (deletedExams.isNotEmpty()) {
                            item { RecycleBinSectionLabel("Exams (${deletedExams.size})") }
                            items(deletedExams) { exam ->
                                DeletedItemCard(
                                    icon = Icons.Default.Assignment,
                                    title = exam.name,
                                    subtitle = "${exam.totalQuestions} questions",
                                    deletedAt = exam.deletedAt,
                                    restoreDescription = "Restore exam ${exam.name}",
                                    deleteDescription = "Permanently delete exam ${exam.name}",
                                    onRestore = { onRestore(exam) },
                                    onPermanentDelete = { examToDelete = exam }
                                )
                            }
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
                Text("This will permanently delete all ${deletedExams.size} deleted exams. This action cannot be undone!")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEmptyDialog = false
                        onEmptyRecycleBin()
                    }
                ) {
                    Text("DELETE ALL", color = ErrorCoral, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
    
    // Permanent delete confirmation - exam
    examToDelete?.let { exam ->
        PermanentDeleteDialog(
            itemName = exam.name,
            onConfirm = {
                onPermanentDelete(exam)
                examToDelete = null
            },
            onDismiss = { examToDelete = null }
        )
    }

    // Permanent delete confirmation - subject
    subjectToDelete?.let { subject ->
        PermanentDeleteDialog(
            itemName = subject.name,
            detail = "Deleting this subject also permanently removes its sections and exams.",
            onConfirm = {
                onPermanentDeleteSubject(subject)
                subjectToDelete = null
            },
            onDismiss = { subjectToDelete = null }
        )
    }

    // Permanent delete confirmation - section
    sectionToDelete?.let { section ->
        PermanentDeleteDialog(
            itemName = section.name,
            detail = "Deleting this section also permanently removes its students and results.",
            onConfirm = {
                onPermanentDeleteSection(section)
                sectionToDelete = null
            },
            onDismiss = { sectionToDelete = null }
        )
    }
}

@Composable
private fun PermanentDeleteDialog(
    itemName: String,
    detail: String? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.DeleteForever,
                contentDescription = null,
                tint = ErrorCoral,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(text = "Delete Permanently?", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("\"$itemName\" will be permanently deleted. This action cannot be undone!")
                detail?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondaryIce)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("DELETE", color = ErrorCoral, fontWeight = FontWeight.Bold)
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
private fun RecycleBinSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondaryIce,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun DeletedItemCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    deletedAt: Long?,
    restoreDescription: String,
    deleteDescription: String,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
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
            Icon(
                icon,
                contentDescription = null,
                tint = ElectricBlue,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryIce
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiaryIce
                )
                deletedAt?.let { timestamp ->
                    Text(
                        text = "Deleted ${formatRelativeTime(timestamp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorCoral
                    )
                }
            }

            // Action buttons - 48dp touch targets
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onRestore,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Restore,
                        contentDescription = restoreDescription,
                        tint = SuccessAzure,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = onPermanentDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = deleteDescription,
                        tint = ErrorCoral,
                        modifier = Modifier.size(22.dp)
                    )
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
