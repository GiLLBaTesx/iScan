package com.examscanner.premium.testing

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

/**
 * Utility for testing scanner accuracy
 * Generates test answer sheets and validates scanning results
 */
object ScannerTestUtility {
    
    data class TestAnswerSheet(
        val examName: String,
        val studentId: String,
        val studentName: String,
        val answers: List<String>, // Expected answers (A, B, C, D, etc.)
        val questionCount: Int = 20
    )
    
    data class ScannerTestResult(
        val totalQuestions: Int,
        val correctDetections: Int,
        val incorrectDetections: Int,
        val missedDetections: Int,
        val accuracy: Float, // Percentage
        val detectionDetails: List<QuestionResult>
    )
    
    data class QuestionResult(
        val questionNumber: Int,
        val expectedAnswer: String,
        val detectedAnswer: String?,
        val isCorrect: Boolean
    )
    
    /**
     * Generate a test answer sheet PDF with known answers
     * This creates a simple bubble sheet format for testing
     */
    fun generateTestAnswerSheet(
        context: Context,
        testSheet: TestAnswerSheet
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            isAntiAlias = true
        }
        
        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        
        // Draw header
        canvas.drawText("TEST ANSWER SHEET", 50f, 50f, boldPaint)
        canvas.drawText("Exam: ${testSheet.examName}", 50f, 80f, paint)
        canvas.drawText("Student ID: ${testSheet.studentId}", 50f, 100f, paint)
        canvas.drawText("Name: ${testSheet.studentName}", 50f, 120f, paint)
        
        // Draw line
        canvas.drawLine(50f, 130f, 545f, 130f, paint)
        
        // Draw instructions
        paint.textSize = 12f
        canvas.drawText("Instructions: Fill the circles completely with black pen", 50f, 150f, paint)
        canvas.drawText("✓ = Filled (this is what scanner should detect)", 50f, 170f, paint)
        
        // Draw questions with bubbles
        var yPosition = 200f
        val bubbleRadius = 10f
        val bubbleSpacing = 40f
        val options = listOf("A", "B", "C", "D", "E")
        
        paint.textSize = 14f
        
        for (i in 0 until testSheet.questionCount) {
            val questionNum = i + 1
            val expectedAnswer = if (i < testSheet.answers.size) testSheet.answers[i] else "A"
            
            // Draw question number
            canvas.drawText("$questionNum.", 50f, yPosition, boldPaint)
            
            // Draw bubbles for each option
            var xPosition = 100f
            for (option in options) {
                val circlePaint = Paint().apply {
                    color = Color.BLACK
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                    isAntiAlias = true
                }
                
                // Draw circle
                canvas.drawCircle(xPosition, yPosition - 5f, bubbleRadius, circlePaint)
                
                // Fill circle if it's the expected answer
                if (option == expectedAnswer) {
                    val fillPaint = Paint().apply {
                        color = Color.BLACK
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    canvas.drawCircle(xPosition, yPosition - 5f, bubbleRadius - 2f, fillPaint)
                    // Add checkmark indicator
                    canvas.drawText("✓", xPosition + 15f, yPosition, paint)
                }
                
                // Draw option letter below circle
                canvas.drawText(option, xPosition - 5f, yPosition + 20f, paint)
                
                xPosition += bubbleSpacing
            }
            
            yPosition += 50f
            
            // New page if needed
            if (yPosition > 750f && questionNum < testSheet.questionCount) {
                pdfDocument.finishPage(page)
                val newPage = pdfDocument.startPage(pageInfo)
                canvas.apply { 
                    // Reset to new page
                }
                yPosition = 50f
            }
        }
        
        pdfDocument.finishPage(page)
        
        // Save to file
        val outputDir = File(context.getExternalFilesDir(null), "test_sheets")
        outputDir.mkdirs()
        val outputFile = File(outputDir, "test_sheet_${System.currentTimeMillis()}.pdf")
        
        FileOutputStream(outputFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        
        pdfDocument.close()
        
        return outputFile
    }
    
    /**
     * Compare scanner results with expected answers
     */
    fun validateScanResults(
        expectedAnswers: List<String>,
        detectedAnswers: List<Pair<Int, String>>
    ): ScannerTestResult {
        val detectionDetails = mutableListOf<QuestionResult>()
        var correctDetections = 0
        var incorrectDetections = 0
        var missedDetections = 0
        
        for (i in expectedAnswers.indices) {
            val questionNum = i + 1
            val expectedAnswer = expectedAnswers[i]
            val detected = detectedAnswers.find { it.first == questionNum }
            
            val result = when {
                detected == null -> {
                    missedDetections++
                    QuestionResult(questionNum, expectedAnswer, null, false)
                }
                detected.second == expectedAnswer -> {
                    correctDetections++
                    QuestionResult(questionNum, expectedAnswer, detected.second, true)
                }
                else -> {
                    incorrectDetections++
                    QuestionResult(questionNum, expectedAnswer, detected.second, false)
                }
            }
            
            detectionDetails.add(result)
        }
        
        val totalQuestions = expectedAnswers.size
        val accuracy = if (totalQuestions > 0) {
            (correctDetections.toFloat() / totalQuestions.toFloat()) * 100f
        } else {
            0f
        }
        
        return ScannerTestResult(
            totalQuestions = totalQuestions,
            correctDetections = correctDetections,
            incorrectDetections = incorrectDetections,
            missedDetections = missedDetections,
            accuracy = accuracy,
            detectionDetails = detectionDetails
        )
    }
    
    /**
     * Generate a formatted test report
     */
    fun generateTestReport(result: ScannerTestResult): String {
        val sb = StringBuilder()
        
        sb.appendLine("═══════════════════════════════════════")
        sb.appendLine("    SCANNER ACCURACY TEST REPORT")
        sb.appendLine("═══════════════════════════════════════")
        sb.appendLine()
        sb.appendLine("Overall Results:")
        sb.appendLine("  Total Questions: ${result.totalQuestions}")
        sb.appendLine("  ✓ Correct Detections: ${result.correctDetections}")
        sb.appendLine("  ✗ Incorrect Detections: ${result.incorrectDetections}")
        sb.appendLine("  ○ Missed Detections: ${result.missedDetections}")
        sb.appendLine()
        sb.appendLine("  ACCURACY: ${String.format("%.2f", result.accuracy)}%")
        sb.appendLine()
        sb.appendLine("═══════════════════════════════════════")
        sb.appendLine()
        sb.appendLine("Detailed Results:")
        sb.appendLine()
        
        result.detectionDetails.forEach { detail ->
            val status = when {
                detail.isCorrect -> "✓"
                detail.detectedAnswer == null -> "○"
                else -> "✗"
            }
            
            val detectedStr = detail.detectedAnswer ?: "NOT DETECTED"
            sb.appendLine(
                "  Q${detail.questionNumber}: $status Expected=[${detail.expectedAnswer}] " +
                "Detected=[$detectedStr]"
            )
        }
        
        sb.appendLine()
        sb.appendLine("═══════════════════════════════════════")
        sb.appendLine()
        
        if (result.accuracy >= 95f) {
            sb.appendLine("✓ EXCELLENT: Scanner is highly accurate!")
        } else if (result.accuracy >= 85f) {
            sb.appendLine("✓ GOOD: Scanner performance is acceptable")
        } else if (result.accuracy >= 70f) {
            sb.appendLine("⚠ FAIR: Scanner needs improvement")
        } else {
            sb.appendLine("✗ POOR: Scanner requires significant fixes")
        }
        
        sb.appendLine()
        
        return sb.toString()
    }
    
    /**
     * Pre-defined test cases
     */
    object TestCases {
        fun allA() = TestAnswerSheet(
            examName = "Test Case: All A",
            studentId = "TEST001",
            studentName = "Test Student",
            answers = List(20) { "A" }
        )
        
        fun allB() = TestAnswerSheet(
            examName = "Test Case: All B",
            studentId = "TEST002",
            studentName = "Test Student",
            answers = List(20) { "B" }
        )
        
        fun alternating() = TestAnswerSheet(
            examName = "Test Case: Alternating",
            studentId = "TEST003",
            studentName = "Test Student",
            answers = List(20) { i -> if (i % 2 == 0) "A" else "B" }
        )
        
        fun random() = TestAnswerSheet(
            examName = "Test Case: Random Pattern",
            studentId = "TEST004",
            studentName = "Test Student",
            answers = listOf(
                "A", "B", "C", "D", "A",
                "C", "B", "D", "A", "C",
                "D", "A", "B", "C", "D",
                "B", "A", "C", "D", "B"
            )
        )
        
        fun diagonal() = TestAnswerSheet(
            examName = "Test Case: Diagonal Pattern",
            studentId = "TEST005",
            studentName = "Test Student",
            answers = listOf(
                "A", "B", "C", "D", "E",
                "A", "B", "C", "D", "E",
                "A", "B", "C", "D", "E",
                "A", "B", "C", "D", "E"
            )
        )
    }
}
