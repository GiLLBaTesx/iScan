package com.examscanner.premium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.ui.unit.offset
import com.examscanner.premium.data.SubjectFolderEntity
import com.examscanner.premium.ui.components.FloatingGlassCard
import com.examscanner.premium.ui.components.GlassCard
import com.examscanner.premium.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectFolderListScreen(
    folders: List<SubjectFolderEntity>,
    onFolderClick: (SubjectFolderEntity) -> Unit,
    onNewFolderClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: com.examscanner.premium.viewmodel.ExamViewModel? = null
) {
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Dynamic stats
    var totalExams by remember { mutableStateOf(0) }
    
    LaunchedEffect(folders) {
        viewModel?.let { vm ->
            totalExams = vm.getTotalExamsCount()
        }
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // ScanKey-style Top Bar
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "EXAM SCANNER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ElectricBlue,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Subject Folders",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusBadge("LIVE SYNC", IcyCyan, true)
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewFolderDialog = true },
                containerColor = ElectricBlue,
                contentColor = androidx.compose.ui.graphics.Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Folder")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Modern Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "EXAM SCANNER",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ElectricBlue,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Subject Folders",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Live Sync Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = IcyCyan.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(IcyCyan, androidx.compose.foundation.shape.CircleShape)
                            )
                            Text(
                                text = "LIVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = IcyCyan,
                                fontSize = 10.sp
                            )
                        }
                    }
                    
                    // Menu Button
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Download Templates") },
                        onClick = {
                            showMenu = false
                            showTemplateDialog = true
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Download, contentDescription = null)
                        }
                    )
                    if (folders.isNotEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Delete All Folders", color = DarkErrorRed) },
                            onClick = {
                                showMenu = false
                                showDeleteAllDialog = true
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = DarkErrorRed
                                )
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = {
                            showMenu = false
                            onSettingsClick()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Settings, contentDescription = null)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Stats Cards Row (ScanKey style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Subjects Card
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = folders.size.toString(),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "SUBJECTS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
                
                // Exams Card
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Icon(
                            Icons.Default.Assignment,
                            contentDescription = null,
                            tint = DarkInfoBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = totalExams.toString(),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "EXAMS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // New Folder Button (Prominent)
            Button(
                onClick = { showNewFolderDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricBlue
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "NEW SUBJECT FOLDER",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Section Header
            Text(
                text = if (folders.isEmpty()) "NO FOLDERS YET" else "YOUR SUBJECTS",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Folders List
            if (folders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "No subject folders yet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Create folders to organize your exams by subject",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "📚 Examples:\nMathematics • Science • English\nFilipino • Araling Panlipunan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(folders) { folder ->
                        SubjectFolderCard(
                            folder = folder,
                            onClick = { onFolderClick(folder) },
                            onDelete = {
                                scope.launch {
                                    viewModel?.let { vm ->
                                        vm.deleteSubjectFolder(folder.id)
                                        android.widget.Toast.makeText(
                                            context,
                                            "Folder deleted",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            onEdit = { newName ->
                                scope.launch {
                                    viewModel?.let { vm ->
                                        vm.updateSubjectFolder(folder.id, newName)
                                        android.widget.Toast.makeText(
                                            context,
                                            "Folder renamed",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
    
    // New Folder Dialog
    if (showNewFolderDialog) {
        NewSubjectFolderDialog(
            onDismiss = { showNewFolderDialog = false },
            onCreate = { name ->
                scope.launch {
                    viewModel?.createSubjectFolder(name)
                    android.widget.Toast.makeText(context, "Folder created!", android.widget.Toast.LENGTH_SHORT).show()
                }
                showNewFolderDialog = false
            }
        )
    }
    
    // Template Download Dialog
    if (showTemplateDialog) {
        TemplateDownloadDialog(
            onDismiss = { showTemplateDialog = false },
            context = context
        )
    }
    
    // Delete All Folders Confirmation
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
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
                    text = "Delete All Folders?",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF3B30)
                )
            },
            text = {
                Column {
                    Text(
                        text = "This will permanently delete ALL ${folders.size} folder${if (folders.size != 1) "s" else ""} and their contents:",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("• All exams in all folders")
                    Text("• All answer keys")
                    Text("• All scanned student results")
                    Text("• All sections")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "⚠️ This action CANNOT be undone!",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF3B30)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            folders.forEach { folder ->
                                viewModel?.deleteSubjectFolder(folder.id)
                            }
                            android.widget.Toast.makeText(
                                context,
                                "All folders deleted",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                        showDeleteAllDialog = false
                    }
                ) {
                    Text("DELETE ALL", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("CANCEL", color = Color(0xFF8E8E93))
                }
            }
        )
    }
}

@Composable
fun StatItem(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
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
fun SubjectFolderCard(
    folder: SubjectFolderEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (String) -> Unit = {},
    viewModel: com.examscanner.premium.viewmodel.ExamViewModel? = null
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    
    // Get folder-specific exam count
    val exams by viewModel?.getFolderExams(folder.id)?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Folder Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ElectricBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = ElectricBlue,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Folder Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${exams.size} exams",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (exams.isNotEmpty()) {
                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = IcyCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            
            // More Menu Button
            Box {
                IconButton(
                    onClick = { showMoreMenu = true }
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            showMoreMenu = false
                            showEditDialog = true
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = DarkErrorRed) },
                        onClick = {
                            showMoreMenu = false
                            showDeleteDialog = true
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = DarkErrorRed
                            )
                        }
                    )
                }
            }
        }
    }
    
    // Edit Dialog
    if (showEditDialog) {
        EditSubjectFolderDialog(
            currentName = folder.name,
            onDismiss = { showEditDialog = false },
            onSave = { newName ->
                showEditDialog = false
                onEdit(newName)
            }
        )
    }
    
    // Delete Confirmation Dialog
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
                    text = "Delete Folder?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to delete \"${folder.name}\"?\n\nAll exams in this folder will also be deleted.")
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

@Composable
fun NewSubjectFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Subject Folder",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter subject name (e.g., Mathematics, Science, English)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8E8E93)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { if (it.length <= 100) folderName = it },
                    label = { Text("Subject Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (folderName.isNotBlank()) onCreate(folderName) },
                enabled = folderName.isNotBlank()
            ) {
                Text("CREATE", color = Color(0xFF007AFF))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color(0xFF8E8E93))
            }
        }
    )
}

@Composable
fun EditSubjectFolderDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var folderName by remember { mutableStateOf(currentName) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Rename Folder",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter new name for this subject folder",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8E8E93)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { if (it.length <= 100) folderName = it },
                    label = { Text("Subject Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (folderName.isNotBlank() && folderName != currentName) onSave(folderName) },
                enabled = folderName.isNotBlank() && folderName != currentName
            ) {
                Text("SAVE", color = Color(0xFF007AFF))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color(0xFF8E8E93))
            }
        }
    )
}

@Composable
fun TemplateDownloadDialog(
    onDismiss: () -> Unit,
    context: android.content.Context
) {
    val templates = remember {
        listOf(
            TemplateInfo("25 Items", "answer_sheet_25.pdf", 25),
            TemplateInfo("50 Items", "answer_sheet_50.pdf", 50),
            TemplateInfo("60 Items", "answer_sheet_60.pdf", 60),
            TemplateInfo("100 Items", "answer_sheet_100.pdf", 100),
            TemplateInfo("120 Items", "answer_sheet_120.pdf", 120)
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Download,
                contentDescription = null,
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Download Answer Sheet Templates",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Select a template to download:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8E8E93)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                templates.forEach { template ->
                    TemplateDownloadItem(
                        template = template,
                        onDownload = {
                            downloadTemplate(context, template)
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "📝 How to use templates:\n" +
                           "1. Download template (saves as HTML file)\n" +
                           "2. Open HTML file in Chrome browser\n" +
                           "3. Print to PDF or print directly\n" +
                           "4. Students fill bubbles with pen\n" +
                           "5. Scan with app camera to grade\n\n" +
                           "✓ Print on A4 or Letter size paper\n" +
                           "✓ Use black or blue pen\n" +
                           "✓ Fill bubbles completely",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8E93)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = Color(0xFF007AFF))
            }
        }
    )
}

data class TemplateInfo(
    val name: String,
    val filename: String,
    val items: Int
)

@Composable
fun TemplateDownloadItem(
    template: TemplateInfo,
    onDownload: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF0F4F8))
            .clickable(onClick = onDownload)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1C1C1E)
                )
                Text(
                    text = "${template.items} questions • PDF",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8E93)
                )
            }
        }
        
        Icon(
            Icons.Default.Download,
            contentDescription = "Download",
            tint = Color(0xFF007AFF),
            modifier = Modifier.size(20.dp)
        )
    }
}

fun downloadTemplate(context: android.content.Context, template: TemplateInfo) {
    try {
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        )
        
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        
        val file = java.io.File(downloadsDir, template.filename)
        
        // Generate HTML template
        generateAnswerSheetPDF(context, template, file)
        
        // Get the HTML file path
        val htmlFile = java.io.File(downloadsDir, template.filename.replace(".pdf", ".html"))
        
        // Show success message
        android.widget.Toast.makeText(
            context,
            "✓ Template saved!\nOpen ${htmlFile.name} in browser to print",
            android.widget.Toast.LENGTH_LONG
        ).show()
        
        // Try to open the HTML file in browser
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                htmlFile
            )
            
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/html")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(intent)
        } catch (e: Exception) {
            // If can't open, show instructions
            android.widget.Toast.makeText(
                context,
                "Template saved to Downloads folder\nOpen the HTML file in Chrome to print",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    } catch (e: Exception) {
        android.widget.Toast.makeText(
            context,
            "Error: ${e.message}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}

fun generateAnswerSheetPDF(
    context: android.content.Context,
    template: TemplateInfo,
    file: java.io.File
) {
    // Create HTML-based template that can be printed
    // This creates a proper scannable bubble sheet layout
    val html = buildString {
        appendLine("<!DOCTYPE html>")
        appendLine("<html>")
        appendLine("<head>")
        appendLine("<meta charset='UTF-8'>")
        appendLine("<title>Answer Sheet - ${template.name}</title>")
        appendLine("<style>")
        appendLine("@page { size: A4; margin: 1cm; }")
        appendLine("body { font-family: Arial, sans-serif; margin: 0; padding: 20px; }")
        appendLine(".header { text-align: center; margin-bottom: 20px; border-bottom: 2px solid #000; padding-bottom: 10px; }")
        appendLine(".title { font-size: 24px; font-weight: bold; margin-bottom: 5px; }")
        appendLine(".subtitle { font-size: 14px; color: #666; }")
        appendLine(".info-section { margin: 20px 0; padding: 15px; border: 1px solid #000; }")
        appendLine(".info-row { margin: 10px 0; }")
        appendLine(".info-label { font-weight: bold; display: inline-block; width: 100px; }")
        appendLine(".info-line { display: inline-block; width: 300px; border-bottom: 1px solid #000; }")
        appendLine(".instructions { margin: 15px 0; padding: 10px; background: #f5f5f5; border-left: 4px solid #007AFF; }")
        appendLine(".instructions ul { margin: 5px 0; padding-left: 20px; }")
        appendLine(".answer-grid { margin-top: 20px; }")
        appendLine(".grid-row { display: flex; margin: 8px 0; }")
        appendLine(".question-num { width: 40px; font-weight: bold; text-align: right; padding-right: 10px; }")
        appendLine(".bubbles { display: flex; gap: 15px; }")
        appendLine(".bubble-container { display: flex; align-items: center; gap: 3px; }")
        appendLine(".bubble { width: 20px; height: 20px; border: 2px solid #000; border-radius: 50%; display: inline-block; }")
        appendLine(".bubble-label { font-size: 14px; font-weight: bold; }")
        appendLine(".footer { margin-top: 20px; text-align: center; font-size: 10px; color: #666; border-top: 1px solid #ccc; padding-top: 10px; }")
        appendLine("@media print { .no-print { display: none; } }")
        appendLine("</style>")
        appendLine("</head>")
        appendLine("<body>")
        
        // Header
        appendLine("<div class='header'>")
        appendLine("<div class='title'>OFFLINE ASSESSMENT - ANSWER SHEET</div>")
        appendLine("<div class='subtitle'>Template: ${template.name} (${template.items} Questions)</div>")
        appendLine("</div>")
        
        // Student Info Section
        appendLine("<div class='info-section'>")
        appendLine("<div class='info-row'><span class='info-label'>Name:</span> <span class='info-line'></span></div>")
        appendLine("<div class='info-row'><span class='info-label'>ID/LRN:</span> <span class='info-line'></span></div>")
        appendLine("<div class='info-row'><span class='info-label'>Date:</span> <span class='info-line'></span></div>")
        appendLine("<div class='info-row'><span class='info-label'>Section:</span> <span class='info-line'></span></div>")
        appendLine("</div>")
        
        // Instructions
        appendLine("<div class='instructions'>")
        appendLine("<strong>INSTRUCTIONS:</strong>")
        appendLine("<ul>")
        appendLine("<li>Use a BLACK or BLUE pen to fill the bubbles COMPLETELY</li>")
        appendLine("<li>Fill only ONE bubble per question</li>")
        appendLine("<li>Make sure your marks are dark and within the circle</li>")
        appendLine("<li>Do not make any stray marks on this sheet</li>")
        appendLine("</ul>")
        appendLine("</div>")
        
        // Answer Grid
        appendLine("<div class='answer-grid'>")
        
        val questionsPerColumn = 25
        val columns = (template.items + questionsPerColumn - 1) / questionsPerColumn
        
        // Create columns
        for (col in 0 until columns) {
            appendLine("<table style='float: left; margin-right: 20px; margin-bottom: 20px;'>")
            
            for (row in 1..questionsPerColumn) {
                val questionNum = row + (col * questionsPerColumn)
                if (questionNum <= template.items) {
                    appendLine("<tr>")
                    appendLine("<td class='question-num'>${questionNum}.</td>")
                    appendLine("<td class='bubbles'>")
                    appendLine("<div class='bubble-container'><span class='bubble-label'>A</span><span class='bubble'></span></div>")
                    appendLine("<div class='bubble-container'><span class='bubble-label'>B</span><span class='bubble'></span></div>")
                    appendLine("<div class='bubble-container'><span class='bubble-label'>C</span><span class='bubble'></span></div>")
                    appendLine("<div class='bubble-container'><span class='bubble-label'>D</span><span class='bubble'></span></div>")
                    appendLine("</td>")
                    appendLine("</tr>")
                }
            }
            
            appendLine("</table>")
        }
        
        appendLine("<div style='clear: both;'></div>")
        appendLine("</div>")
        
        // Footer
        appendLine("<div class='footer'>")
        appendLine("Generated by Offline Assessment App | Scan this sheet using the app camera to grade automatically")
        appendLine("</div>")
        
        appendLine("</body>")
        appendLine("</html>")
    }
    
    // Save as HTML file (can be opened in browser and printed/saved as PDF)
    val htmlFile = java.io.File(file.parent, template.filename.replace(".pdf", ".html"))
    htmlFile.writeText(html)
    
    // Also save a simple text version for reference
    file.writeText("Open the HTML file to print: ${htmlFile.name}\n\nThis HTML file can be:\n1. Opened in any browser\n2. Printed to PDF\n3. Printed directly\n\nThe HTML file is in the same folder as this file.")
}



@Composable
fun AnimatedMenuButton(
    showMenu: Boolean,
    onClick: () -> Unit
) {
    // Animated offset when menu is open
    val offsetY by animateFloatAsState(
        targetValue = if (showMenu) 8f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "offsetY"
    )
    
    IconButton(onClick = onClick) {
        Icon(
            Icons.Default.MoreVert,
            contentDescription = "Menu",
            tint = Color(0xFF007AFF),
            modifier = Modifier.offset(y = offsetY.dp)
        )
    }
}
