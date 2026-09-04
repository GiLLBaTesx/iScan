package com.examscanner.premium.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.examscanner.premium.data.SubjectFolderEntity
import com.examscanner.premium.ui.components.*
import com.examscanner.premium.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Main Home Screen - ScanKey Design
 * Clean, ice-white background with electric blue accents
 * Features: Create, Delete, Rename folders
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SubjectFolderListScreen(
    folders: List<SubjectFolderEntity>,
    onFolderClick: (SubjectFolderEntity) -> Unit,
    onNewFolderClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: com.examscanner.premium.viewmodel.ExamViewModel? = null
) {
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<SubjectFolderEntity?>(null) }
    var showRenameDialog by remember { mutableStateOf<SubjectFolderEntity?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Get total exams
    var totalExams by remember { mutableStateOf(0) }
    LaunchedEffect(folders) {
        viewModel?.let { vm ->
            totalExams = vm.getTotalExamsCount()
        }
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Top Bar - ScanKey Style
            Surface(
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Title
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
                    
                    // Right side - Live badge + Options + Settings
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusBadge("LIVE SYNC", IcyCyan, showDot = true)
                        
                        // Options Menu Button
                        Box {
                            IconButton(onClick = { showOptionsMenu = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "More options",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            
                            DropdownMenu(
                                expanded = showOptionsMenu,
                                onDismissRequest = { showOptionsMenu = false }
                            ) {
                                if (folders.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Delete All Folders", color = ErrorCoral) },
                                        onClick = {
                                            showOptionsMenu = false
                                            showDeleteAllDialog = true
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = ErrorCoral
                                            )
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Settings") },
                                    onClick = {
                                        showOptionsMenu = false
                                        onSettingsClick()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Settings, contentDescription = null)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            // FAB for new folder
            FloatingActionButton(
                onClick = { showNewFolderDialog = true },
                containerColor = ElectricBlue,
                contentColor = androidx.compose.ui.graphics.Color.White
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "New Folder",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        value = folders.size.toString(),
                        label = "SUBJECTS",
                        icon = Icons.Default.Folder,
                        modifier = Modifier.weight(1f),
                        iconTint = ElectricBlue
                    )
                    StatCard(
                        value = totalExams.toString(),
                        label = "EXAMS",
                        icon = Icons.Default.Assignment,
                        modifier = Modifier.weight(1f),
                        iconTint = IcyCyan
                    )
                }
            }
            
            // Section Header
            item {
                SectionHeader(
                    title = if (folders.isEmpty()) "NO FOLDERS YET" else "YOUR SUBJECTS",
                    actionText = if (folders.isNotEmpty()) "SORT ▼" else null,
                    onActionClick = if (folders.isNotEmpty()) { {} } else null
                )
            }
            
            // Empty State or Folder List
            if (folders.isEmpty()) {
                item {
                    FrostedGlassCard {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp)
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
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap the + button to create your first subject folder",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(folders.size) { index ->
                    val folder = folders[index]
                    val exams by viewModel?.getFolderExams(folder.id)?.collectAsState(initial = emptyList()) 
                        ?: remember { mutableStateOf(emptyList()) }
                    
                    var showFolderMenu by remember { mutableStateOf(false) }
                    
                    // Folder Card with long-press menu
                    Box {
                        FrostedGlassCard(
                            onClick = { onFolderClick(folder) },
                            modifier = Modifier.combinedClickable(
                                onClick = { onFolderClick(folder) },
                                onLongClick = { showFolderMenu = true }
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconContainer(
                                    icon = Icons.Default.Folder,
                                    backgroundColor = ElectricBlue.copy(alpha = 0.1f),
                                    iconTint = ElectricBlue
                                )
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
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
                                            Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            StatusBadge("Active", IcyCyan, showDot = false)
                                        }
                                    }
                                }
                                
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        
                        // Folder context menu
                        DropdownMenu(
                            expanded = showFolderMenu,
                            onDismissRequest = { showFolderMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = {
                                    showFolderMenu = false
                                    showRenameDialog = folder
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = ErrorCoral) },
                                onClick = {
                                    showFolderMenu = false
                                    showDeleteDialog = folder
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = ErrorCoral
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // New Folder Dialog
    if (showNewFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = {
                Text(
                    "New Subject Folder",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder Name") },
                    placeholder = { Text("e.g., Mathematics") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        focusedLabelColor = ElectricBlue
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            scope.launch {
                                viewModel?.createSubjectFolder(folderName)
                                android.widget.Toast.makeText(
                                    context,
                                    "Folder created!",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                            showNewFolderDialog = false
                        }
                    }
                ) {
                    Text("CREATE", color = ElectricBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
    
    // Rename Dialog
    showRenameDialog?.let { folder ->
        var newName by remember { mutableStateOf(folder.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = {
                Text(
                    "Rename Folder",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        focusedLabelColor = ElectricBlue
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            scope.launch {
                                viewModel?.updateSubjectFolder(folder.id, newName)
                                android.widget.Toast.makeText(
                                    context,
                                    "Folder renamed!",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                            showRenameDialog = null
                        }
                    }
                ) {
                    Text("RENAME", color = ElectricBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) {
                    Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
    
    // Delete Confirmation Dialog
    showDeleteDialog?.let { folder ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = {
                Text(
                    "Delete Folder?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${folder.name}\"? This action cannot be undone.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel?.deleteSubjectFolder(folder.id)
                            android.widget.Toast.makeText(
                                context,
                                "Folder deleted",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                        showDeleteDialog = null
                    }
                ) {
                    Text("DELETE", color = ErrorCoral, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
    
    // Delete All Confirmation Dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = {
                Text(
                    "Delete All Folders?",
                    fontWeight = FontWeight.Bold,
                    color = ErrorCoral
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete ALL ${folders.size} folders? This will also delete all exams inside them. This action cannot be undone.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                    Text("DELETE ALL", color = ErrorCoral, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
    
    // Template Download Dialog
    if (showTemplateDialog) {
        val templates = remember {
            listOf(
                "25 Items (A-D)" to 25,
                "50 Items (A-D)" to 50,
                "60 Items (A-E) - NAT Format" to 60,
                "100 Items (A-D) - 2 Parts" to 100,
                "50 Items (True/False)" to 50
            )
        }
        
        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = {
                Column {
                    Text(
                        text = "Download OMR Templates",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select a template to download as PDF",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                ) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(templates.size) { index ->
                            val (name, questions) = templates[index]
                            com.examscanner.premium.ui.components.FrostedGlassCard(
                                onClick = {
                                    scope.launch {
                                        try {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Generating $name template...",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                            
                                            val file = com.examscanner.premium.utils.TemplatePDFGenerator.generateTemplate(
                                                context = context,
                                                templateName = name,
                                                totalQuestions = questions,
                                                choicesPerQuestion = 4  // A-D default
                                            )
                                            
                                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.provider",
                                                file
                                            )
                                            
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, "application/pdf")
                                                flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                                        android.content.Intent.FLAG_ACTIVITY_NO_HISTORY
                                            }
                                            
                                            try {
                                                context.startActivity(intent)
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "✓ $name template generated!",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            } catch (e: Exception) {
                                                // If no PDF viewer, show share dialog
                                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                    type = "application/pdf"
                                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Open or Share Template"))
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "✓ Template saved: ${file.name}",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Failed to generate template: ${e.message}",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        }
                                        showTemplateDialog = false
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "$questions questions",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        Icons.Default.Download,
                                        contentDescription = "Download",
                                        tint = ElectricBlue
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemplateDialog = false }) {
                    Text("CLOSE", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
