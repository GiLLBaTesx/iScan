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
    var backupToExport by remember { mutableStateOf<BackupManager.BackupInfo?>(null) }
    
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
                    successMessage = "✓ Restore complete! RESTART the app."
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
            backupToExport?.let { backup ->
                scope.launch {
                    try {
                        val result = BackupManager.exportBackup(context, backup, destinationUri)
                        if (result.isSuccess) {
                            successMessage = "✓ Backup exported successfully!"
                        } else {
                            errorMessage = "Export failed: ${result.exceptionOrNull()?.message}"
                        }
                    } catch (e: Exception) {
                        errorMessage = "Export failed: ${e.message}"
                    }
                    backupToExport = null
                }
            }
        }
    }
    
    // Load backups on start
    LaunchedEffect(Unit) {
        loadBackups()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IceWhite)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Custom Top Bar with gradient effect
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = ElectricBlue
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Backup & Restore",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = ElectricBlue
                        )
                        Text(
                            text = "${backups.size} backup${if (backups.size != 1) "s" else ""} available",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                </Row>
            }
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stats Card
                item {
                    FrostedGlassCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Backup,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${backups.size}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricBlue
                                )
                                Text(
                                    text = "Backups",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = IcyCyan,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = lastBackupTime?.let {
                                        val days = (System.currentTimeMillis() - it) / (1000 * 60 * 60 * 24)
                                        when {
                                            days < 1 -> "Today"
                                            days < 7 -> "${days}d ago"
                                            else -> "${days / 7}w ago"
                                        }
                                    } ?: "Never",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = IcyCyan
                                )
                                Text(
                                    text = "Last Backup",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
                
                // Action Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Create Backup
                        FrostedGlassCard(
                            onClick = {
                                scope.launch {
                                    isCreatingBackup = true
                                    val result = BackupManager.createBackup(context)
                                    isCreatingBackup = false
                                    
                                    if (result.isSuccess) {
                                        successMessage = "✓ Backup created!"
                                        onBackupCreated()
                                        loadBackups()
                                    } else {
                                        errorMessage = "Failed: ${result.exceptionOrNull()?.message}"
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Create",
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricBlue
                                )
                            }
                        }
                        
                        // Restore from File
                        FrostedGlassCard(
                            onClick = { restoreFilePicker.launch("*/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = IcyCyan,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Import",
                                    fontWeight = FontWeight.Bold,
                                    color = IcyCyan
                                )
                            }
                        }
                    }
                }
                
                // Section Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MY BACKUPS",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = ElectricBlue,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        if (backups.isNotEmpty()) {
                            Text(
                                text = "Keep last 7",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
                
                // Backup List
                if (backups.isEmpty()) {
                    item {
                        FrostedGlassCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No Backups Yet",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Protect your data by creating regular backups",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                                backupToExport = backup
                                exportFilePicker.launch(backup.file.name)
                            }
                        )
                    }
                }
                
                // Bottom padding
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
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
                FrostedGlassCard {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = ElectricBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isCreatingBackup) "Creating backup..." else "Restoring...",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        // Success/Error Snackbars
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            successMessage?.let { message ->
                LaunchedEffect(message) {
                    kotlinx.coroutines.delay(3000)
                    successMessage = null
                }
                
                FrostedGlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = IcyCyan
                        )
                        Text(
                            text = message,
                            color = IcyCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            
            errorMessage?.let { message ->
                LaunchedEffect(message) {
                    kotlinx.coroutines.delay(5000)
                    errorMessage = null
                }
                
                FrostedGlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = ErrorCoral
                        )
                        Text(
                            text = message,
                            color = ErrorCoral,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
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
                        "⚠️ Current data will be backed up first. You MUST restart the app after restore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarningAmber,
                        fontWeight = FontWeight.SemiBold
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
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Status Icon with Background
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (backup.isValid) 
                            IcyCyan.copy(alpha = 0.1f) 
                        else 
                            ErrorCoral.copy(alpha = 0.1f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (backup.isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (backup.isValid) IcyCyan else ErrorCoral,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = BackupManager.formatBackupDate(backup.timestamp),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = BackupManager.formatFileSize(backup.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = if (backup.isValid) "Valid" else "Invalid",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (backup.isValid) IcyCyan else ErrorCoral,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = ElectricBlue
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Restore",
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            onClick = {
                                showMenu = false
                                onRestore()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Restore,
                                    contentDescription = null,
                                    tint = IcyCyan
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Export",
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            onClick = {
                                showMenu = false
                                onExport()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null,
                                    tint = ElectricBlue
                                )
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Delete",
                                    fontWeight = FontWeight.SemiBold,
                                    color = ErrorCoral
                                )
                            },
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
        }
    }
}
