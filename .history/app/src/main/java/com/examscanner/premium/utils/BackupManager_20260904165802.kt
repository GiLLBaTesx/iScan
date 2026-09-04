package com.examscanner.premium.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

private val Context.backupDataStore: DataStore<Preferences> by preferencesDataStore(name = "backup_prefs")

object BackupManager {
    private const val TAG = "BackupManager"
    private const val MAX_BACKUPS = 7 // Keep last 7 backups
    private const val DB_NAME = "exam_scanner_database"
    
    private val LAST_BACKUP_KEY = longPreferencesKey("last_backup_timestamp")
    private val BACKUP_COUNT_KEY = stringPreferencesKey("backup_count")
    
    data class BackupInfo(
        val file: File,
        val timestamp: Long,
        val size: Long,
        val isValid: Boolean
    )
    
    /**
     * Create a backup of the database
     */
    suspend fun createBackup(context: Context): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                // Get database file
                val dbFile = context.getDatabasePath(DB_NAME)
                if (!dbFile.exists()) {
                    return@withContext Result.failure(Exception("Database file not found"))
                }
                
                // Create backup directory
                val backupDir = File(context.getExternalFilesDir(null), "backups")
                if (!backupDir.exists()) {
                    backupDir.mkdirs()
                }
                
                // Generate backup filename with timestamp
                val timestamp = System.currentTimeMillis()
                val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val backupFileName = "backup_${dateFormat.format(Date(timestamp))}.db"
                val backupFile = File(backupDir, backupFileName)
                
                // Force WAL checkpoint to ensure all data is flushed
                try {
                    val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                        dbFile.absolutePath,
                        null,
                        android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
                    )
                    db.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
                    db.close()
                } catch (e: Exception) {
                    Log.w(TAG, "Could not checkpoint database: ${e.message}")
                }
                
                // Copy database file to backup
                try {
                    dbFile.inputStream().use { input ->
                        backupFile.outputStream().use { output ->
                            input.copyTo(output)
                            output.flush()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to copy database file", e)
                    return@withContext Result.failure(Exception("Failed to copy database: ${e.message}"))
                }
                
                // Verify backup
                if (!verifyBackup(backupFile)) {
                    backupFile.delete()
                    return@withContext Result.failure(Exception("Backup verification failed"))
                }
                
                // Update last backup timestamp
                try {
                    context.backupDataStore.edit { prefs ->
                        prefs[LAST_BACKUP_KEY] = timestamp
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not update backup timestamp", e)
                }
                
                // Clean up old backups
                cleanupOldBackups(backupDir)
                
                Log.i(TAG, "Backup created successfully: ${backupFile.absolutePath}")
                Result.success(backupFile)
                
            } catch (e: Exception) {
                Log.e(TAG, "Backup failed", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Restore database from backup file and restart app
     * The app will automatically restart to load the restored database
     */
    suspend fun restoreBackup(context: Context, backupUri: Uri): Result<Boolean> {
        return try {
            // Get current database file
            val dbFile = context.getDatabasePath(DB_NAME)
            val shmFile = context.getDatabasePath("$DB_NAME-shm")
            val walFile = context.getDatabasePath("$DB_NAME-wal")
            
            // Create temporary file to validate backup
            val tempFile = File(context.cacheDir, "temp_restore.db")
            
            // Copy backup to temp file
            context.contentResolver.openInputStream(backupUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return Result.failure(Exception("Could not open backup file"))
            
            // Verify backup is valid
            if (!verifyBackup(tempFile)) {
                tempFile.delete()
                return Result.failure(Exception("Invalid or corrupted backup file"))
            }
            
            // Create backup of current database before replacing
            if (dbFile.exists()) {
                val currentBackupDir = File(context.getExternalFilesDir(null), "backups")
                currentBackupDir.mkdirs()
                val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val currentBackupFile = File(
                    currentBackupDir, 
                    "backup_before_restore_${dateFormat.format(Date())}.db"
                )
                try {
                    dbFile.copyTo(currentBackupFile, overwrite = true)
                    Log.i(TAG, "Created safety backup: ${currentBackupFile.name}")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not create safety backup", e)
                }
            }
            
            // Close database by killing the app process
            // This ensures clean restore without file locks
            try {
                // Force checkpoint first
                val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                    dbFile.absolutePath,
                    null,
                    android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
                )
                db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).close()
                db.close()
            } catch (e: Exception) {
                Log.w(TAG, "Could not checkpoint database", e)
            }
            
            // Delete WAL and SHM files
            shmFile.delete()
            walFile.delete()
            
            // Replace current database with backup
            tempFile.copyTo(dbFile, overwrite = true)
            tempFile.delete()
            
            Log.i(TAG, "Database restored successfully - app restart required")
            
            Result.success(true)
            
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Verify backup file integrity
     */
    private fun verifyBackup(backupFile: File): Boolean {
        return try {
            // Check file size
            if (backupFile.length() == 0L) {
                Log.e(TAG, "Backup file is empty")
                return false
            }
            
            // Try to open as SQLite database
            val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                backupFile.absolutePath,
                null,
                android.database.sqlite.SQLiteDatabase.OPEN_READONLY
            )
            
            // Check if required tables exist
            val cursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name IN ('exams', 'subject_folders', 'answer_keys')",
                null
            )
            
            val tableCount = cursor.count
            cursor.close()
            db.close()
            
            if (tableCount < 3) {
                Log.e(TAG, "Backup file is missing required tables")
                return false
            }
            
            Log.i(TAG, "Backup verification passed")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Backup verification failed", e)
            false
        }
    }
    
    /**
     * Get list of all available backups
     */
    fun getAvailableBackups(context: Context): List<BackupInfo> {
        val backupDir = File(context.getExternalFilesDir(null), "backups")
        if (!backupDir.exists() || !backupDir.isDirectory) {
            return emptyList()
        }
        
        return backupDir.listFiles { file ->
            file.isFile && file.extension == "db" && file.name.startsWith("backup_")
        }?.map { file ->
            BackupInfo(
                file = file,
                timestamp = file.lastModified(),
                size = file.length(),
                isValid = verifyBackup(file)
            )
        }?.sortedByDescending { it.timestamp } ?: emptyList()
    }
    
    /**
     * Delete old backups, keeping only the most recent MAX_BACKUPS
     */
    private fun cleanupOldBackups(backupDir: File) {
        try {
            val backups = backupDir.listFiles { file ->
                file.isFile && file.extension == "db" && file.name.startsWith("backup_")
            }?.sortedByDescending { it.lastModified() } ?: return
            
            // Delete backups beyond MAX_BACKUPS
            backups.drop(MAX_BACKUPS).forEach { file ->
                if (file.delete()) {
                    Log.i(TAG, "Deleted old backup: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old backups", e)
        }
    }
    
    /**
     * Get last backup timestamp
     */
    suspend fun getLastBackupTimestamp(context: Context): Long? {
        return context.backupDataStore.data.map { prefs ->
            prefs[LAST_BACKUP_KEY]
        }.first()
    }
    
    /**
     * Check if backup is needed (more than 7 days since last backup)
     */
    suspend fun isBackupNeeded(context: Context): Boolean {
        val lastBackup = getLastBackupTimestamp(context) ?: return true
        val daysSinceBackup = (System.currentTimeMillis() - lastBackup) / (1000 * 60 * 60 * 24)
        return daysSinceBackup >= 7
    }
    
    /**
     * Get formatted date from timestamp
     */
    fun formatBackupDate(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
    
    /**
     * Get formatted file size
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        }
    }
    
    /**
     * Delete a specific backup
     */
    fun deleteBackup(backupInfo: BackupInfo): Boolean {
        return try {
            backupInfo.file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete backup", e)
            false
        }
    }
    
    /**
     * Export backup to external location
     */
    suspend fun exportBackup(context: Context, backupInfo: BackupInfo, destinationUri: Uri): Result<Boolean> {
        return try {
            context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                FileInputStream(backupInfo.file).use { input ->
                    input.copyTo(output)
                }
            } ?: return Result.failure(Exception("Could not open destination"))
            
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Restart the app after restore
     */
    private fun restartApp(context: Context) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            
            // Kill the current process to ensure clean restart
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(0)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restart app", e)
        }
    }
}
