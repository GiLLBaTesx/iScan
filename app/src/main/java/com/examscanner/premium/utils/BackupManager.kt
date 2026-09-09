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
import com.examscanner.premium.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
     * IMPORTANT: This should be called from a background thread/coroutine
     */
    suspend fun createBackup(context: Context): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "Starting backup creation...")
                
                // CRITICAL: Checkpoint database before backup
                try {
                    Log.i(TAG, "Attempting to checkpoint database via Room...")
                    val database = AppDatabase.getDatabase(context)
                    // Force Room to checkpoint the WAL file using SupportSQLiteDatabase API
                    val supportDb = database.openHelper.writableDatabase
                    supportDb.query("PRAGMA wal_checkpoint(FULL)").use { cursor ->
                        cursor.moveToFirst()
                    }
                    // Give it a moment to complete
                    kotlinx.coroutines.delay(200)
                    Log.i(TAG, "Database checkpoint via Room completed")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not checkpoint via Room, trying direct approach", e)
                    // Fallback: try direct checkpoint
                    try {
                        val dbFile = context.getDatabasePath(DB_NAME)
                        val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                            dbFile.absolutePath,
                            null,
                            android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                        )
                        db.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
                        db.close()
                    } catch (e2: Exception) {
                        Log.w(TAG, "Direct checkpoint also failed, continuing anyway", e2)
                    }
                }
                
                // Get database file
                val dbFile = context.getDatabasePath(DB_NAME)
                Log.i(TAG, "Database path: ${dbFile.absolutePath}")
                
                if (!dbFile.exists()) {
                    Log.e(TAG, "Database file not found at: ${dbFile.absolutePath}")
                    return@withContext Result.failure(Exception("Database file not found"))
                }
                
                Log.i(TAG, "Database file size: ${dbFile.length()} bytes")
                
                // Create backup directory
                val externalFilesDir = context.getExternalFilesDir(null)
                if (externalFilesDir == null) {
                    Log.e(TAG, "External files directory is null - storage not available")
                    return@withContext Result.failure(Exception("Storage not available. Please check if external storage is mounted."))
                }
                
                val backupDir = File(externalFilesDir, "backups")
                Log.i(TAG, "Backup directory: ${backupDir.absolutePath}")
                
                if (!backupDir.exists()) {
                    val created = backupDir.mkdirs()
                    if (!created) {
                        Log.e(TAG, "Failed to create backup directory")
                        return@withContext Result.failure(Exception("Could not create backup directory"))
                    }
                    Log.i(TAG, "Backup directory created successfully")
                }
                
                // Generate backup filename with timestamp
                val timestamp = System.currentTimeMillis()
                val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val backupFileName = "backup_${dateFormat.format(Date(timestamp))}.db"
                val backupFile = File(backupDir, backupFileName)
                Log.i(TAG, "Creating backup file: ${backupFile.absolutePath}")
                
                // Copy database file to backup (including WAL files if they exist)
                try {
                    Log.i(TAG, "Starting file copy...")
                    
                    // Copy main database file
                    dbFile.inputStream().use { input ->
                        backupFile.outputStream().use { output ->
                            val bytesCopied = input.copyTo(output)
                            output.flush()
                            Log.i(TAG, "Copied $bytesCopied bytes to backup file")
                        }
                    }
                    
                    // Also copy WAL and SHM files if they exist (for complete backup)
                    val walFile = File(dbFile.parentFile, "${dbFile.name}-wal")
                    val shmFile = File(dbFile.parentFile, "${dbFile.name}-shm")
                    
                    if (walFile.exists()) {
                        val backupWal = File(backupFile.parentFile, "${backupFile.name}-wal")
                        walFile.copyTo(backupWal, overwrite = true)
                        Log.i(TAG, "Copied WAL file")
                    }
                    if (shmFile.exists()) {
                        val backupShm = File(backupFile.parentFile, "${backupFile.name}-shm")
                        shmFile.copyTo(backupShm, overwrite = true)
                        Log.i(TAG, "Copied SHM file")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to copy database file", e)
                    // Clean up partial backup
                    backupFile.delete()
                    return@withContext Result.failure(Exception("Failed to copy database: ${e.message}"))
                }
                
                Log.i(TAG, "Verifying backup...")
                
                // Verify backup
                if (!verifyBackup(backupFile)) {
                    Log.e(TAG, "Backup verification failed")
                    backupFile.delete()
                    return@withContext Result.failure(Exception("Backup verification failed - file may be corrupted"))
                }
                
                Log.i(TAG, "Backup verified successfully")
                
                // Note: Not updating timestamp to DataStore to avoid potential database issues
                // The backup file timestamp itself serves as the last backup indicator
                
                // Clean up old backups
                cleanupOldBackups(backupDir)
                
                Log.i(TAG, "Backup created successfully: ${backupFile.absolutePath}")
                return@withContext Result.success(backupFile)
                
            } catch (e: Exception) {
                Log.e(TAG, "Backup failed", e)
                return@withContext Result.failure(e)
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
     * Get last backup timestamp from the most recent backup file
     */
    suspend fun getLastBackupTimestamp(context: Context): Long? {
        return withContext(Dispatchers.IO) {
            try {
                val backups = getAvailableBackups(context)
                backups.firstOrNull()?.timestamp
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get last backup timestamp", e)
                null
            }
        }
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
     * Delete all backups
     */
    fun deleteAllBackups(context: Context): Result<Int> {
        return try {
            val backupDir = File(context.getExternalFilesDir(null), "backups")
            if (!backupDir.exists() || !backupDir.isDirectory) {
                return Result.success(0)
            }
            
            var deletedCount = 0
            backupDir.listFiles()?.forEach { file ->
                if (file.delete()) {
                    deletedCount++
                    Log.i(TAG, "Deleted backup: ${file.name}")
                }
            }
            
            Result.success(deletedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete all backups", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get total size of all backups
     */
    fun getTotalBackupSize(context: Context): Long {
        val backupDir = File(context.getExternalFilesDir(null), "backups")
        if (!backupDir.exists() || !backupDir.isDirectory) {
            return 0L
        }
        
        return backupDir.listFiles()?.sumOf { it.length() } ?: 0L
    }
    
    /**
     * Cleanup backups older than specified days, keeping at least minToKeep backups
     */
    fun cleanupOldBackups(context: Context, olderThanDays: Int, minToKeep: Int = 3): Result<Int> {
        return try {
            val backupDir = File(context.getExternalFilesDir(null), "backups")
            if (!backupDir.exists() || !backupDir.isDirectory) {
                return Result.success(0)
            }
            
            val backups = backupDir.listFiles { file ->
                file.isFile && file.extension == "db" && file.name.startsWith("backup_")
            }?.sortedByDescending { it.lastModified() } ?: return Result.success(0)
            
            // Keep at least minToKeep backups regardless of age
            val backupsToConsider = backups.drop(minToKeep)
            
            val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
            var deletedCount = 0
            
            backupsToConsider.forEach { file ->
                if (file.lastModified() < cutoffTime) {
                    if (file.delete()) {
                        deletedCount++
                        Log.i(TAG, "Cleaned up old backup: ${file.name}")
                    }
                }
            }
            
            Result.success(deletedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old backups", e)
            Result.failure(e)
        }
    }
}
