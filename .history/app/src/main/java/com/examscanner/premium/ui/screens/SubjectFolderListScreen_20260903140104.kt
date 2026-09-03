package com.examscanner.premium.ui.screens

import androidx.compose.foundation.background
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
    var showDeleteDialog by remember { mutableStateOf<SubjectFolderEntity?>(null) }
    var showRenameDialog by remember { mutableStateOf<SubjectFolderEntity?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
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
                                DropdownMenuItem(
                                    text = { Text("Download Templates") },
                                    onClick = {
                                        showOptionsMenu = false
                                        showTemplateDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Download, contentDescription = null)
                                    }
                                )
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
                    
                    // Folder Card
                    FrostedGlassCard(onClick = { onFolderClick(folder) }) {
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
                }
            }
        }
    }
    
    // New Folder Dialog
    if (showNewFolderDialog) {
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
                var folderName by remember { mutableStateOf("") }
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
                        // Create folder logic
                        scope.launch {
                            // viewModel?.createSubjectFolder(folderName)
                            android.widget.Toast.makeText(
                                context,
                                "Feature coming soon!",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                        showNewFolderDialog = false
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
}
