package com.examscanner.premium.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.examscanner.premium.data.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Analytics Export Utility
 * Exports exam analytics to PDF and Excel formats
 */
object AnalyticsExporter {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
    
    /**
     * Export analytics to CSV (Excel-compatible)
     */
    fun exportToCSV(
        context: Context,
        examName: String,
        students: List<StudentEntity>,
        answerKeys: List<AnswerKeyEntity>,
        studentAnswers: List<StudentAnswerEntity>,
        questionMelcMappings: Map<Int, MelcEntity>
    ): Result<File> {
        return try {
            val timestamp = dateFormat.format(Date())
            val sanitizedExamName = InputSanitizer.sanitizeFileName(examName)
            val fileName = "${sanitizedExamName}_Analytics_$timestamp.csv"
            val file = File(context.getExternalFilesDir(null), "exports/$fileName")
            file.parentFile?.mkdirs()
            
            FileOutputStream(file).use { output ->
                val writer = output.bufferedWriter()
                
                // Header
                writer.write("EXAM ANALYTICS REPORT\n")
                writer.write("Exam: $examName\n")
                writer.write("Generated: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())}\n")
                writer.write("Total Students: ${students.size}\n")
                writer.write("Total Questions: ${answerKeys.size}\n\n")
                
                // Student Scores Section
                writer.write("STUDENT SCORES\n")
                writer.write("Student ID,Name,Score,Percentage,Status\n")
                
                students.forEach { student ->
                    val answers = studentAnswers.filter { it.studentEntityId == student.id }
                    val correctCount = answers.count { answer ->
                        val key = answerKeys.find { it.questionNumber == answer.questionNumber }
                        key?.correctAnswer == answer.answer
                    }
                    val percentage = if (answerKeys.isNotEmpty()) {
                        (correctCount.toFloat() / answerKeys.size * 100).toInt()
                    } else 0
                    val status = when {
                        percentage >= 75 -> "Passed"
                        else -> "Needs Improvement"
                    }
                    
                    writer.write("${student.id},\"${student.name}\",$correctCount/${answerKeys.size},$percentage%,$status\n")
                }
                
                writer.write("\n")
                
                // Item Analysis Section
                writer.write("ITEM ANALYSIS\n")
                writer.write("Question,Correct Answer,Difficulty,Discrimination,A Count,B Count,C Count,D Count,E Count\n")
                
                answerKeys.sortedBy { it.questionNumber }.forEach { key ->
                    val questionAnswers = studentAnswers.filter { it.questionNumber == key.questionNumber }
                    val correctCount = questionAnswers.count { it.answer == key.correctAnswer }
                    val difficulty = if (questionAnswers.isNotEmpty()) {
                        (correctCount.toFloat() / questionAnswers.size * 100).toInt()
                    } else 0
                    
                    // Calculate discrimination index
                    val studentScores = students.map { student ->
                        val answers = studentAnswers.filter { it.studentEntityId == student.id }
                        val correct = answers.count { answer ->
                            val k = answerKeys.find { it.questionNumber == answer.questionNumber }
                            k?.correctAnswer == answer.answer
                        }
                        student.id to correct
                    }.sortedByDescending { it.second }
                    
                    val topThird = studentScores.take((studentScores.size * 0.27).toInt())
                    val bottomThird = studentScores.takeLast((studentScores.size * 0.27).toInt())
                    
                    val topCorrect = topThird.count { (studentId, _) ->
                        studentAnswers.find { it.studentEntityId == studentId && it.questionNumber == key.questionNumber }?.answer == key.correctAnswer
                    }
                    val bottomCorrect = bottomThird.count { (studentId, _) ->
                        studentAnswers.find { it.studentEntityId == studentId && it.questionNumber == key.questionNumber }?.answer == key.correctAnswer
                    }
                    
                    val discrimination = if (topThird.isNotEmpty()) {
                        ((topCorrect.toFloat() / topThird.size) - (bottomCorrect.toFloat() / bottomThird.size.coerceAtLeast(1)))
                    } else 0f
                    
                    val discriminationFormatted = "%.2f".format(discrimination)
                    
                    // Count answers
                    val aCount = questionAnswers.count { it.answer == "A" }
                    val bCount = questionAnswers.count { it.answer == "B" }
                    val cCount = questionAnswers.count { it.answer == "C" }
                    val dCount = questionAnswers.count { it.answer == "D" }
                    val eCount = questionAnswers.count { it.answer == "E" }
                    
                    writer.write("Q${key.questionNumber},${key.correctAnswer},$difficulty%,$discriminationFormatted,$aCount,$bCount,$cCount,$dCount,$eCount\n")
                }
                
                writer.write("\n")
                
                // MELC Mapping Section (if available)
                if (questionMelcMappings.isNotEmpty()) {
                    writer.write("MELC COMPETENCY MAPPING\n")
                    writer.write("Question,MELC Code,Competency\n")
                    
                    questionMelcMappings.entries.sortedBy { it.key }.forEach { (questionNum, melc) ->
                        writer.write("Q$questionNum,${melc.code},\"${melc.description}\"\n")
                    }
                }
                
                writer.flush()
            }
            
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Export analytics to PDF
     */
    fun exportToPDF(
        context: Context,
        examName: String,
        students: List<StudentEntity>,
        answerKeys: List<AnswerKeyEntity>,
        studentAnswers: List<StudentAnswerEntity>,
        questionMelcMappings: Map<Int, MelcEntity>
    ): Result<File> {
        // For now, we'll create a text-based report that can be converted to PDF
        // In a production app, you'd use a PDF library like iText or Android PDF APIs
        return try {
            val timestamp = dateFormat.format(Date())
            val sanitizedExamName = InputSanitizer.sanitizeFileName(examName)
            val fileName = "${sanitizedExamName}_Report_$timestamp.txt"
            val file = File(context.getExternalFilesDir(null), "exports/$fileName")
            file.parentFile?.mkdirs()
            
            FileOutputStream(file).use { output ->
                val writer = output.bufferedWriter()
                
                writer.write("═══════════════════════════════════════════\n")
                writer.write("         EXAM ANALYTICS REPORT\n")
                writer.write("═══════════════════════════════════════════\n\n")
                writer.write("Exam: $examName\n")
                writer.write("Date: ${SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())}\n")
                writer.write("Time: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}\n")
                writer.write("Total Students: ${students.size}\n")
                writer.write("Total Questions: ${answerKeys.size}\n\n")
                
                writer.write("───────────────────────────────────────────\n")
                writer.write("STUDENT PERFORMANCE SUMMARY\n")
                writer.write("───────────────────────────────────────────\n\n")
                
                students.forEach { student ->
                    val answers = studentAnswers.filter { it.studentEntityId == student.id }
                    val correctCount = answers.count { answer ->
                        val key = answerKeys.find { it.questionNumber == answer.questionNumber }
                        key?.correctAnswer == answer.answer
                    }
                    val percentage = if (answerKeys.isNotEmpty()) {
                        (correctCount.toFloat() / answerKeys.size * 100).toInt()
                    } else 0
                    
                    writer.write("${student.name}\n")
                    writer.write("  Score: $correctCount/${answerKeys.size} ($percentage%)\n")
                    writer.write("  Status: ${if (percentage >= 75) "✓ Passed" else "✗ Needs Improvement"}\n\n")
                }
                
                writer.write("\n───────────────────────────────────────────\n")
                writer.write("ITEM ANALYSIS\n")
                writer.write("───────────────────────────────────────────\n\n")
                
                answerKeys.sortedBy { it.questionNumber }.forEach { key ->
                    val questionAnswers = studentAnswers.filter { it.questionNumber == key.questionNumber }
                    val correctCount = questionAnswers.count { it.answer == key.correctAnswer }
                    val difficulty = if (questionAnswers.isNotEmpty()) {
                        (correctCount.toFloat() / questionAnswers.size * 100).toInt()
                    } else 0
                    
                    writer.write("Question ${key.questionNumber}\n")
                    writer.write("  Correct Answer: ${key.correctAnswer}\n")
                    writer.write("  Difficulty: $difficulty%\n")
                    writer.write("  Students Answered Correctly: $correctCount/${questionAnswers.size}\n\n")
                }
                
                writer.flush()
            }
            
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Share exported file
     */
    fun shareFile(context: Context, file: File, mimeType: String = "text/csv") {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(intent, "Share Analytics"))
        } catch (e: Exception) {
            SecureLogger.e("AnalyticsExporter", "Error sharing file", e)
        }
    }
}
