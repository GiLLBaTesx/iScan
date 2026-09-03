package com.examscanner.premium.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * SyncLogEntity - Records cloud sync operations for debugging and auditing.
 *
 * Standalone log table (no foreign keys) capturing each upload/download/conflict
 * event, the affected entity, and its outcome. Part of the v2 schema expansion
 * (Requirement 18.x). Consumed by the sync manager (Requirement 17.6 integration).
 */
@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operation: String,                     // "upload", "download", "conflict"
    val entityType: String,                    // "exam", "student", etc.
    val entityId: Long,
    val status: String,                        // "success", "failed", "conflict"
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
