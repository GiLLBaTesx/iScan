package com.examscanner.premium.ui.screens

import android.net.Uri
import android.util.Log
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
import com.examscanner.premium.ui.components.*
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
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showCleanupDialog by remember { mutableStateOf(false) }
    var totalBackupSize by remember { mutableStateOf(0L) }
    
    // Load backups function
    val loadBackups: () -> Unit = {
        scope.launch {
            backups = BackupManager.getAvailableBackups(context)
            lastBackupTime = BackupManager.getLastBackupTimestamp(context)
            totalBackupSize = BackupManager.getTotalBackupSize(context)
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
                    successMessage = "✓ Database restored! Restarting in 5 seconds..."
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
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show success messages
    LaunchedEffect(successMessage) {
        successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            successMessage = null
        }
    }
    
    // Show error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
            errorMessage = null
        }
    }
    
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (data.visuals.message.startsWith("✓")) IcyCyan else ErrorCoral,
                    contentColor = Color.White
                )
            }
        },
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
            FloatingActionButton(
                onClick = {
                    Log.d("BackupScreen", "FAB clicked!")
                    scope.launch {
                        try {
                            isCreatingBackup = true
                            errorMessage = null
                            successMessage = null
                            
                            Log.d("BackupScreen", "Starting backup creation...")
                            val result = BackupManager.createBackup(context)
                            isCreatingBackup = false
                            
                            Log.d("BackupScreen", "Backup result: ${result.isSuccess}")
                            
                            if (result.isSuccess) {
                                successMessage = "✓ Backup created successfully!"
                                Log.d("BackupScreen", "Calling loadBackups...")
                                loadBackups()
                                Log.d("BackupScreen", "Calling onBackupCreated...")
                                onBackupCreated()
                                Log.d("BackupScreen", "Backup creation complete")
                            } else {
                                errorMessage = "Backup failed: ${result.exceptionOrNull()?.message}"
                                Log.e("BackupScreen", "Backup failed", result.exceptionOrNull())
                            }
                        } catch (e: Exception) {
                            isCreatingBackup = false
                            errorMessage = "Backup failed: ${e.message}"
                            Log.e("BackupScreen", "Backup creation crashed", e)
                        }
                    }
                },
                containerColor = ElectricBlue,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Backup, contentDescription = "Create Backup")
            }
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
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            
                            // Tip for users
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        IcyCyan.copy(alpha = 0.1f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.TipsAndUpdates,
                                    contentDescription = null,
                                    tint = IcyCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Tip: Use 'Share Latest Backup' to save backups to Google Drive or other cloud storage",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
                        
                        // Share latest backup
                        if (backups.isNotEmpty()) {
                            FrostedGlassCard(
                                onClick = { 
                                    backupToExport = backups.first()
                                    exportFilePicker.launch(backups.first().file.name)
                                },
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
                                        Icons.Default.Share,
                                        contentDescription = null,
                                        tint = ElectricBlue
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Share Latest Backup",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Export backup to save or share",
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
                        
                        // Backup Management Actions (if backups exist)
                        if (backups.isNotEmpty()) {
                            FrostedGlassCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    // Cleanup old backups
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showCleanupDialog = true }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AutoDelete,
                                            contentDescription = null,
                                            tint = WarningAmber
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Cleanup Old Backups",
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Remove backups older than 30 days",
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
                                    
                                    Divider(color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                                    
                                    // Delete all backups
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showDeleteAllDialog = true }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteForever,
                                            contentDescription = null,
                                            tint = ErrorCoral
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Clear All Backups",
                                                fontWeight = FontWeight.SemiBold,
                                                color = ErrorCoral
                                            )
                                            Text(
                                                text = "Delete all backups (${BackupManager.formatFileSize(totalBackupSize)})",
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
                                backupToExport = backup
                                exportFilePicker.launch(backup.file.name)
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
        UnifiedDialog(
            onDismissRequest = { showRestoreDialog = null },
            title = "Restore Backup?",
            icon = Icons.Default.Restore,
            confirmText = "RESTORE",
            dismissText = "CANCEL",
            onConfirm = {
                scope.launch {
                    isRestoring = true
                    val result = BackupManager.restoreBackup(
                        context,
                        Uri.fromFile(backup.file)
                    )
                    isRestoring = false
                    showRestoreDialog = null
                    
                    if (result.isSuccess) {
                        successMessage = "✓ Restore complete! Restarting in 5 seconds..."
                        onBackupRestored()
                    } else {
                        errorMessage = "Restore failed: ${result.exceptionOrNull()?.message}"
                    }
                }
            },
            onDismiss = { showRestoreDialog = null },
            content = {
                Text("This will replace all current data with:")
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("• Backup: ${backup.file.name}", fontWeight = FontWeight.Medium, color = TextPrimaryIce)
                Text("• Date: ${BackupManager.formatBackupDate(backup.timestamp)}", color = TextSecondaryIce)
                Text("• Size: ${BackupManager.formatFileSize(backup.size)}", color = TextSecondaryIce)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "⚠️ Current data will be backed up first.",
                    color = WarningAmber
                )
                Text(
                    "📱 App will restart automatically to load the restored data.",
                    color = TextSecondaryIce
                )
            }
        )
    }
    
    // Delete confirmation dialog
    showDeleteDialog?.let { backup ->
        UnifiedConfirmDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = "Delete Backup?",
            message = "Are you sure you want to delete this backup? This action cannot be undone.",
            icon = Icons.Default.Delete,
            confirmText = "DELETE",
            isDangerous = true,
            onConfirm = {
                if (BackupManager.deleteBackup(backup)) {
                    successMessage = "Backup deleted"
                    loadBackups()
                } else {
                    errorMessage = "Failed to delete backup"
                }
                showDeleteDialog = null
            },
            onDismiss = { showDeleteDialog = null }
        )
    }
    
    // Delete all backups confirmation dialog
    if (showDeleteAllDialog) {
        UnifiedConfirmDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = "Delete All Backups?",
            message = "Are you sure you want to delete ALL ${backups.size} backups? This action cannot be undone and you will not be able to restore your data.",
            icon = Icons.Default.DeleteForever,
            confirmText = "DELETE ALL",
            isDangerous = true,
            onConfirm = {
                scope.launch {
                    val result = BackupManager.deleteAllBackups(context)
                    if (result.isSuccess) {
                        successMessage = "All backups deleted"
                        loadBackups()
                    } else {
                        errorMessage = "Failed to delete all backups"
                    }
                }
                showDeleteAllDialog = false
            },
            onDismiss = { showDeleteAllDialog = false }
        )
    }
    
    // Cleanup old backups dialog
    if (showCleanupDialog) {
        AlertDialog(
            onDismissRequest = { showCleanupDialog = false },
            icon = {
                Icon(
                    Icons.Default.AutoDelete,
                    contentDescription = null,
                    tint = WarningAmber,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Cleanup Old Backups?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("This will delete backups older than 30 days.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Keeps at least 3 most recent backups")
                    Text("• Safe and automatic cleanup")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "💡 Tip: Export important backups to cloud storage before cleaning up.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val result = BackupManager.cleanupOldBackups(context, olderThanDays = 30, minToKeep = 3)
                            if (result.isSuccess) {
                                val count = result.getOrNull() ?: 0
                                if (count > 0) {
                                    successMessage = "✓ Cleaned up $count old backup${if (count > 1) "s" else ""}"
                                } else {
                                    successMessage = "✓ No old backups to clean up"
                                }
                                loadBackups()
                            } else {
                                errorMessage = "Cleanup failed: ${result.exceptionOrNull()?.message}"
                            }
                        }
                        showCleanupDialog = false
                    }
                ) {
                    Text("CLEANUP", color = WarningAmber, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleanupDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
private fun BackupItemCard(
    backup: BackupManager.BackupInfo,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit
) {
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
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
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
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = BackupManager.formatBackupDate(backup.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Action buttons row
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
                            tint = IcyCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Export button
                    IconButton(
                        onClick = onExport,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Export",
                            tint = ElectricBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Delete button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = ErrorCoral,
                            modifier = Modifier.size(20.dp)
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
