package com.examscanner.premium.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.examscanner.premium.ui.components.FrostedGlassCard
import com.examscanner.premium.ui.theme.*
import com.examscanner.premium.utils.BackupManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupManagementScreen(
    onBack: () -> Unit,
    onBackupCreated: () -> Unit = {},
    onBackupRestored: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var backups by remember { mutableStateOf<List<BackupManager.BackupInfo>>(emptyList()) }
    var isCreatingBackup by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var lastBackupTime by remember { mutableStateOf<Long?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showRestoreDialog by remember { mutableStateOf<BackupManager.BackupInfo?>(null) }
    var showDeleteDialog by remember { mutableStateOf<BackupManager.BackupInfo?>(null) }
    
    // Load backups function
    val loadBackups: () -> Unit = {
        scope.launch {
            backups = BackupManager.getAvailableBackups(context)
            lastBackupTime = BackupManager.getLastBackupTimestamp(context)
        }
    }
    
    // File picker for restore
    val restoreFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isRestoring = true
                val result = BackupManager.restoreBackup(context, it)
                isRestoring = false
                
                if (result.isSuccess) {
                    successMessage = "Database restored successfully! Please restart the app."
                    onBackupRestored()
                    loadBackups()
                } else {
                    errorMessage = "Restore failed: ${result.exceptionOrNull()?.message}"
                }
            }
        }
    }
    
    // File picker for export
    val exportFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri?.let { destinationUri ->
            scope.launch {
                // Export logic will be added when user selects a backup to export
            }
        }
    }
    
    // Load backups on start
    LaunchedEffect(Unit) {
        loadBackups()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Backup Management",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${backups.size} backups available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = IceWhite
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    scope.launch {
                        isCreatingBackup = true
                        val result = BackupManager.createBackup(context)
                        isCreatingBackup = false
                        
                        if (result.isSuccess) {
                            successMessage = "Backup created successfully!"
                            onBackupCreated()
                            loadBackups()
                        } else {
                            errorMessage = "Backup failed: ${result.exceptionOrNull()?.message}"
                        }
                    }
                },
                icon = { Icon(Icons.Default.Backup, contentDescription = null) },
                text = { Text("Create Backup") },
                containerColor = ElectricBlue,
                contentColor = Color.White
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(IceWhite)
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Last backup info card
                item {
                    FrostedGlassCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(32.dp)
                                )
                                Column {
                                    Text(
                                        text = "Last Backup",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = lastBackupTime?.let {
                                            BackupManager.formatBackupDate(it)
                                        } ?: "Never",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Quick actions
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Restore from file
                        FrostedGlassCard(
                            onClick = { restoreFilePicker.launch("*/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = IcyCyan
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Restore from File",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Select a backup file to restore",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Section header
                item {
                    Text(
                        text = "AVAILABLE BACKUPS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                    )
                }
                
                // Backup list
                if (backups.isEmpty()) {
                    item {
                        FrostedGlassCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No backups yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Create your first backup to protect your data",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(backups) { backup ->
                        BackupItemCard(
                            backup = backup,
                            onRestore = { showRestoreDialog = backup },
                            onDelete = { showDeleteDialog = backup },
                            onExport = {
                                // TODO: Implement export
                            }
                        )
                    }
                }
                
                // Bottom padding for FAB
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
            
            // Loading overlay
            if (isCreatingBackup || isRestoring) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = ElectricBlue)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (isCreatingBackup) "Creating backup..." else "Restoring...",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Restore confirmation dialog
    showRestoreDialog?.let { backup ->
        AlertDialog(
            onDismissRequest = { showRestoreDialog = null },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = WarningAmber,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Restore Backup?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("This will replace all current data with:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Backup: ${backup.file.name}")
                    Text("• Date: ${BackupManager.formatBackupDate(backup.timestamp)}")
                    Text("• Size: ${BackupManager.formatFileSize(backup.size)}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Current data will be backed up automatically before restoring.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isRestoring = true
                            val result = BackupManager.restoreBackup(
                                context,
                                Uri.fromFile(backup.file)
                            )
                            isRestoring = false
                            showRestoreDialog = null
                            
                            if (result.isSuccess) {
                                successMessage = "✓ Restore complete! RESTART the app to see restored data."
                                onBackupRestored()
                            } else {
                                errorMessage = "Restore failed: ${result.exceptionOrNull()?.message}"
                            }
                        }
                    }
                ) {
                    Text("RESTORE", color = ElectricBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = null }) {
                    Text("CANCEL")
                }
            }
        )
    }
    
    // Delete confirmation dialog
    showDeleteDialog?.let { backup ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = ErrorCoral,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text("Delete Backup?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Are you sure you want to delete this backup? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (BackupManager.deleteBackup(backup)) {
                            successMessage = "Backup deleted"
                            loadBackups()
                        } else {
                            errorMessage = "Failed to delete backup"
                        }
                        showDeleteDialog = null
                    }
                ) {
                    Text("DELETE", color = ErrorCoral, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("CANCEL")
                }
            }
        )
    }
    
    // Success message snackbar
    successMessage?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(3000)
            successMessage = null
        }
        
        Snackbar(
            modifier = Modifier.padding(16.dp),
            containerColor = IcyCyan
        ) {
            Text(message, color = Color.White)
        }
    }
    
    // Error message snackbar
    errorMessage?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(5000)
            errorMessage = null
        }
        
        Snackbar(
            modifier = Modifier.padding(16.dp),
            containerColor = ErrorCoral
        ) {
            Text(message, color = Color.White)
        }
    }
}

@Composable
private fun BackupItemCard(
    backup: BackupManager.BackupInfo,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    FrostedGlassCard(
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (backup.isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (backup.isValid) IcyCyan else ErrorCoral
                    )
                    Column {
                        Text(
                            text = backup.file.name,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = BackupManager.formatBackupDate(backup.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Restore") },
                            onClick = {
                                showMenu = false
                                onRestore()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Restore, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                showMenu = false
                                onDelete()
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
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Size: ${BackupManager.formatFileSize(backup.size)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (backup.isValid) "Valid ✓" else "Invalid",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (backup.isValid) IcyCyan else ErrorCoral,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
