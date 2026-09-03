package com.examscanner.premium.utils

import android.content.Context
import android.net.Uri
import com.examscanner.premium.data.StudentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object CSVImportUtility {
    
    data class ImportResult(
        val successCount: Int,
        val failedCount: Int,
        val students: List<StudentEntity>,
        val errors: List<String>
    )
    
    /**
     * Import students from CSV file
     * Expected format: StudentID,FullName,GradeLevel,ContactInfo
     * First row is header (will be skipped)
     */
    suspend fun importStudentsFromCSV(
        context: Context,
        uri: Uri,
        sectionId: Long
    ): ImportResult = withContext(Dispatchers.IO) {
        val students = mutableListOf<StudentEntity>()
        val errors = mutableListOf<String>()
        var lineNumber = 0
        
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    
                    // Skip header line
                    reader.readLine()
                    lineNumber++
                    
                    while (reader.readLine().also { line = it } != null) {
                        lineNumber++
                        val row = line!!
                        
                        if (row.isBlank()) continue
                        
                        try {
                            val columns = parseCSVLine(row)
                            
                            if (columns.size < 2) {
                                errors.add("Line $lineNumber: Insufficient columns (need at least ID and Name)")
                                continue
                            }
                            
                            val studentId = columns[0].trim()
                            val name = columns[1].trim()
                            val gradeLevel = if (columns.size > 2) columns[2].trim() else ""
                            val contactInfo = if (columns.size > 3) columns[3].trim() else ""
                            
                            if (studentId.isEmpty() || name.isEmpty()) {
                                errors.add("Line $lineNumber: Student ID and Name are required")
                                continue
                            }
                            
                            students.add(
                                StudentEntity(
                                    studentId = studentId,
                                    name = name,
                                    gradeLevel = gradeLevel,
                                    contactInfo = contactInfo,
                                    sectionId = sectionId
                                )
                            )
                        } catch (e: Exception) {
                            errors.add("Line $lineNumber: ${e.message}")
                        }
                    }
                }
            } ?: throw Exception("Failed to open file")
        } catch (e: Exception) {
            errors.add("Error reading file: ${e.message}")
        }
        
        ImportResult(
            successCount = students.size,
            failedCount = errors.size,
            students = students,
            errors = errors
        )
    }
    
    /**
     * Parse CSV line handling quoted fields
     */
    private fun parseCSVLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        
        for (i in line.indices) {
            val char = line[i]
            
            when {
                char == '"' -> {
                    if (i < line.length - 1 && line[i + 1] == '"') {
                        // Escaped quote
                        current.append('"')
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> {
                    current.append(char)
                }
            }
        }
        
        result.add(current.toString())
        return result
    }
    
    /**
     * Generate sample CSV template
     */
    fun generateSampleCSV(): String {
        return buildString {
            appendLine("StudentID,FullName,GradeLevel,ContactInfo")
            appendLine("2023001,Juan Dela Cruz,Grade 10,09171234567")
            appendLine("2023002,Maria Santos,Grade 10,maria@example.com")
            appendLine("2023003,Pedro Reyes,Grade 10,")
        }
    }
}
