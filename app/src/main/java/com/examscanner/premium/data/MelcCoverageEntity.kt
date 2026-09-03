package com.examscanner.premium.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * MelcCoverageEntity - Tracks manual (non-assessment based) MELC coverage.
 *
 * Records when a teacher marks a competency as covered for a given subject,
 * independent of any exam/assessment. Used by the pacing and curriculum
 * tracking engine (Requirements 2.7, 18.x).
 *
 * Cascades on delete of the referenced MELC or subject folder.
 */
@Entity(
    tableName = "melc_coverage",
    foreignKeys = [
        ForeignKey(
            entity = MelcEntity::class,
            parentColumns = ["id"],
            childColumns = ["melcId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SubjectFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["melcId"]), Index(value = ["subjectId"])]
)
data class MelcCoverageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val melcId: Long,
    val subjectId: Long,
    val coveredAt: Long,
    val notes: String = "",
    val coverageType: String = "Manual"        // "Assessment" or "Manual"
)
