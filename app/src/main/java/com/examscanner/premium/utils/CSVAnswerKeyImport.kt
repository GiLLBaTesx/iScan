package com.examscanner.premium.utils

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader

object CSVAnswerKeyImport {
    
    data class ImportResult(
        val answerKey: Map<Int, String>,
        val successCount: Int,
        val failedCount: Int,
        val errors: List<String>
    )
    
    /**
     * Import answer key from CSV file
     * 
     * Expected CSV format:
     * Question,Answer
     * 1,A
     * 2,B
     * 3,C
     * ...
     * 
     * OR simple format (one answer per line):
     * A
     * B
     * C
     * ...
     */
    fun importAnswerKeyFromCSV(
        context: Context,
        uri: Uri,
        totalQuestions: Int
    ): ImportResult {
        val answerKey = mutableMapOf<Int, String>()
        val errors = mutableListOf<String>()
        var successCount = 0
        var failedCount = 0
        
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var lineNumber = 0
                    var hasHeader = false
                    
                    reader.forEachLine { line ->
                        lineNumber++
                        val trimmedLine = line.trim()
                        
                        // Skip empty lines
                        if (trimmedLine.isEmpty()) return@forEachLine
                        
                        // Check if first line is header
                        if (lineNumber == 1 && (trimmedLine.lowercase().contains("question") || 
                            trimmedLine.lowercase().contains("answer"))) {
                            hasHeader = true
                            return@forEachLine
                        }
                        
                        try {
                            // Parse CSV line
                            if (trimmedLine.contains(",")) {
                                // Format: Question,Answer
                                val parts = trimmedLine.split(",").map { it.trim() }
                                if (parts.size >= 2) {
                                    val questionNum = parts[0].toIntOrNull()
                                    val answer = parts[1].uppercase()
                                    
                                    if (questionNum != null && questionNum in 1..totalQuestions) {
                                        if (isValidAnswer(answer)) {
                                            answerKey[questionNum] = answer
                                            successCount++
                                        } else {
                                            errors.add("Line $lineNumber: Invalid answer '$answer' (must be A-E)")
                                            failedCount++
                                        }
                                    } else {
                                        errors.add("Line $lineNumber: Invalid question number '${parts[0]}'")
                                        failedCount++
                                    }
                                }
                            } else {
                                // Simple format: one answer per line
                                val answer = trimmedLine.uppercase()
                                val questionNum = if (hasHeader) lineNumber - 1 else lineNumber
                                
                                if (questionNum in 1..totalQuestions) {
                                    if (isValidAnswer(answer)) {
                                        answerKey[questionNum] = answer
                                        successCount++
                                    } else {
                                        errors.add("Line $lineNumber: Invalid answer '$answer' (must be A-E)")
                                        failedCount++
                                    }
                                } else {
                                    errors.add("Line $lineNumber: Question number exceeds total ($totalQuestions)")
                                    failedCount++
                                }
                            }
                        } catch (e: Exception) {
                            errors.add("Line $lineNumber: ${e.message}")
                            failedCount++
                        }
                    }
                }
            } ?: throw Exception("Cannot open CSV file")
            
        } catch (e: Exception) {
            errors.add("Failed to read CSV: ${e.message}")
            failedCount++
        }
        
        return ImportResult(
            answerKey = answerKey,
            successCount = successCount,
            failedCount = failedCount,
            errors = errors
        )
    }
    
    /**
     * Check if answer is valid (A-E)
     */
    private fun isValidAnswer(answer: String): Boolean {
        return answer.length == 1 && answer[0] in 'A'..'E'
    }
    
    /**
     * Generate sample CSV template
     */
    fun generateSampleCSV(totalQuestions: Int): String {
        val sb = StringBuilder()
        sb.appendLine("Question,Answer")
        for (i in 1..totalQuestions) {
            sb.appendLine("$i,A")
        }
        return sb.toString()
    }
}
