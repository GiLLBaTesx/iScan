package com.examscanner.premium.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * StudentNoteEntity - Teacher notes attached to a student profile.
 *
 * Supports multiple timestamped notes per student. Cascades on delete of the
 * parent student. Part of the v2 schema expansion (Requirements 8.7, 18.x).
 */
@Entity(
    tableName = "student_notes",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["studentId"])]
)
data class StudentNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val note: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
