package com.examscanner.premium.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * StudentEnrollmentEntity - Associates a student with a section (class group).
 *
 * Join entity implementing the N:1 relationship between students and sections.
 * A student can be enrolled in multiple sections, but only once per section
 * (enforced by the unique index on studentId + sectionId).
 *
 * Cascades on delete of either the parent student or section so enrollments
 * never dangle. Part of the v2 schema expansion (Requirements 7.7, 18.x).
 */
@Entity(
    tableName = "student_enrollments",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["studentId", "sectionId"], unique = true)]
)
data class StudentEnrollmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val sectionId: Long,
    val enrolledAt: Long = System.currentTimeMillis(),
    val status: String = "Active"              // Active, Dropped, Transferred
)
