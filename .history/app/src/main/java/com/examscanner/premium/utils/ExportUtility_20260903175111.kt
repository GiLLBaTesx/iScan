package com.examscanner.premium.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.examscanner.premium.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object ExportUtility {
    
    /**
     * Export exam results to CSV format
     */
    suspend fun exportExamToCSV(
        context: Context,
        exam: ExamEntity,
        students: List<StudentScore>,
        answerKeys: List<AnswerKeyEntity>,
        studentAnswersMap: Map<Long, List<StudentAnswerEntity>> = emptyMap()
    ): File = withContext(Dispatchers.IO) {
        val outputDir = File(context.getExternalFilesDir(null), "exports")
        outputDir.mkdirs()
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${exam.name.replace(" ", "_")}_${timestamp}.csv"
        val outputFile = File(outputDir, fileName)
        
        FileWriter(outputFile).use { writer ->
            // Header row
            writer.append("Student ID,Name,Score,Percentage,")
            
            // Add question columns with answer key
            for (i in 1..exam.totalQuestions) {
                val correctAnswer = answerKeys.find { it.questionNumber == i }?.correctAnswer ?: "-"
                writer.append("Q$i (Key: $correctAnswer),")
            }
            writer.append("Date Scanned\n")
            
            // Data rows
            students.forEach { studentScore ->
                val student = studentScore.student
                val studentAnswers = studentAnswersMap[student.id] ?: emptyList()
                
                writer.append("${student.studentId},")
                writer.append("\"${student.name}\",")
                writer.append("${studentScore.score},")
                writer.append("${studentScore.percentage}%,")
                
                // Add actual student answers for each question
                for (i in 1..exam.totalQuestions) {
                    val answer = studentAnswers.find { it.questionNumber == i }?.answer ?: "-"
                    writer.append("$answer,")
                }
                
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                writer.append("${dateFormat.format(Date(student.scannedAt))}\n")
            }
        }
        
        outputFile
    }
    
    /**
     * Export class roster to CSV
     */
    suspend fun exportRosterToCSV(
        context: Context,
        sectionName: String,
        students: List<StudentEntity>
    ): File = withContext(Dispatchers.IO) {
        val outputDir = File(context.getExternalFilesDir(null), "exports")
        outputDir.mkdirs()
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "roster_${sectionName.replace(" ", "_")}_${timestamp}.csv"
        val outputFile = File(outputDir, fileName)
        
        FileWriter(outputFile).use { writer ->
            // Header
            writer.append("Student ID,Name,Grade Level,Contact Info\n")
            
            // Data
            students.forEach { student ->
                writer.append("${student.studentId},")
                writer.append("\"${student.name}\",")
                writer.append("${student.gradeLevel},")
                writer.append("\"${student.contactInfo}\"\n")
            }
        }
        
        outputFile
    }
    
    /**
     * Share exported file
     */
    fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Share Export"))
    }
}
