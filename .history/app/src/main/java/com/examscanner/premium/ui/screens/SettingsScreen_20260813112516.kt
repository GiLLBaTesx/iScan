package com.examscanner.premium.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.examscanner.premium.ui.components.FloatingGlassCard
import com.examscanner.premium.ui.components.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onBackupData: () -> Unit,
    onRestoreData: () -> Unit,
    onClearData: () -> Unit
) {
    val context = LocalContext.current
    var showAboutDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1C1E)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // App Section
                SectionHeader("Application")
                
                SettingsGroup {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "About",
                        subtitle = "Version 1.0.0",
                        onClick = { showAboutDialog = true }
                    )
                    
                    Divider(color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                    
                    SettingsItem(
                        icon = Icons.Default.Language,
                        title = "Language",
                        subtitle = "English",
                        onClick = { /* TODO: Language selection */ }
                    )
                }
                
                // Data Section
                SectionHeader("Data Management")
                
                SettingsGroup {
                    SettingsItem(
                        icon = Icons.Default.Backup,
                        title = "Backup Data",
                        subtitle = "Export all data to file",
                        onClick = onBackupData
                    )
                    
                    Divider(color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                    
                    SettingsItem(
                        icon = Icons.Default.Restore,
                        title = "Restore Data",
                        subtitle = "Import from backup file",
                        onClick = onRestoreData
                    )
                    
                    Divider(color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                    
                    SettingsItem(
                        icon = Icons.Default.Delete,
                        title = "Clear All Data",
                        subtitle = "Delete everything (cannot be undone)",
                        onClick = { showClearDataDialog = true },
                        isDestructive = true
                    )
                }
                
                // Storage Section
                SectionHeader("Storage")
                
                SettingsGroup {
                    SettingsItem(
                        icon = Icons.Default.Storage,
                        title = "App Storage",
                        subtitle = getAppSize(context),
                        onClick = { }
                    )
                    
                    Divider(color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                    
                    SettingsItem(
                        icon = Icons.Default.FolderDelete,
                        title = "Recycle Bin",
                        subtitle = "Manage deleted items",
                        onClick = { /* TODO: Recycle bin screen */ }
                    )
                }
                
                // Privacy Section
                SectionHeader("Privacy & Security")
                
                SettingsGroup {
                    SettingsItem(
                        icon = Icons.Default.Security,
                        title = "Privacy Policy",
                        subtitle = "How we handle your data",
                        onClick = { /* TODO: Privacy policy */ }
                    )
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
    
    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Offline Assessment",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text("Version: 1.0.0")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("A comprehensive teacher assessment system for Filipino educators.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Features:",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("• Subject folder organization")
                    Text("• DepEd MELCs integration")
                    Text("• Bubble sheet scanning")
                    Text("• Competency tracking")
                    Text("• Professional reports")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("CLOSE", color = Color(0xFF007AFF))
                }
            }
        )
    }
    
    // Clear Data Confirmation Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
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
                    text = "Clear All Data?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("This will permanently delete:\n\n• All subject folders\n• All exams and answer keys\n• All student records\n• All scan results\n\nThis action cannot be undone!")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDataDialog = false
                        onClearData()
                    }
                ) {
                    Text("DELETE ALL", color = Color(0xFFFF3B30))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("CANCEL", color = Color(0xFF8E8E93))
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = Color(0xFF8E8E93),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    FloatingGlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isDestructive) Color(0xFFFF3B30) else Color(0xFF007AFF),
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDestructive) Color(0xFFFF3B30) else Color(0xFF1C1C1E)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E8E93)
            )
        }
        
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFAEAEB2)
        )
    }
}

private fun getAppSize(context: Context): String {
    val cacheSize = context.cacheDir.walkTopDown().sumOf { it.length() }
    val filesSize = context.filesDir.walkTopDown().sumOf { it.length() }
    val totalBytes = cacheSize + filesSize
    
    return when {
        totalBytes < 1024 -> "$totalBytes B"
        totalBytes < 1024 * 1024 -> "${totalBytes / 1024} KB"
        else -> "${totalBytes / (1024 * 1024)} MB"
    }
}
