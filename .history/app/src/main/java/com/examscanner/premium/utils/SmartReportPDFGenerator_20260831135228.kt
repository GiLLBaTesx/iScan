package com.examscanner.premium.utils

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.Typeface
import com.examscanner.premium.ui.screens.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * ONE-TAP PDF EXPORT - Your MVP's Final Step
 * 
 * Generates comprehensive report with:
 * 1. Executive Summary (What to Reteach)
 * 2. Item Analysis (Difficulty + Discrimination)
 * 3. Action Plan (Intervention Groups)
 * 4. Parent-Friendly Reports (Simple language)
 */
object SmartReportPDFGenerator {
    
    private const val PAGE_WIDTH = 595  // A4 width in points
    private const val PAGE_HEIGHT = 842 // A4 height in points
    private const val MARGIN = 40
    private const val LINE_HEIGHT = 20
    
    suspend fun generateSmartReport(
        context: Context,
        examName: String,
        reteachPriorities: List<ReteachPriority>,
        itemAnalysis: List<ItemAnalysisData>,
        interventionGroups: List<InterventionGroup>,
        studentCount: Int
    ): File {
        val pdfDocument = PdfDocument()
        var currentY = MARGIN + 20
        
        // PAGE 1: Executive Summary - What to Reteach
        val page1 = pdfDocument.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        val canvas1 = page1.canvas
        
        currentY = drawHeader(canvas1, examName, currentY)
        currentY += 20
        
        currentY = drawSection(canvas1, "🎯 WHAT TO RETEACH NOW", currentY, bold = true, size = 18f)
        currentY += 10
        
        currentY = drawText(
            canvas1,
            "AI-Suggested Priorities for $studentCount Students",
            currentY,
            size = 12f,
            color = android.graphics.Color.GRAY
        )
        currentY += 30
        
        // Draw top 3 priorities
        reteachPriorities.take(3).forEachIndexed { index, priority ->
            currentY = drawReteachPriority(canvas1, index + 1, priority, currentY)
            currentY += 25
            
            // Start new page if needed
            if (currentY > PAGE_HEIGHT - 100) {
                pdfDocument.finishPage(page1)
                val newPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create())
                currentY = MARGIN + 20
            }
        }
        
        pdfDocument.finishPage(page1)
        
        // PAGE 2: Item Analysis
        val page2 = pdfDocument.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create())
        val canvas2 = page2.canvas
        currentY = MARGIN + 20
        
        currentY = drawSection(canvas2, "📊 ITEM ANALYSIS", currentY, bold = true, size = 18f)
        currentY += 10
        
        currentY = drawText(
            canvas2,
            "Difficulty Index: % who got it right | Discrimination Index: High vs Low performers",
            currentY,
            size = 10f,
            color = android.graphics.Color.GRAY
        )
        currentY += 25
        
        // Draw item analysis table header
        currentY = drawItemAnalysisHeader(canvas2, currentY)
        currentY += 5
        
        // Draw items
        itemAnalysis.take(20).forEach { item ->
            currentY = drawItemAnalysisRow(canvas2, item, currentY)
            
            if (currentY > PAGE_HEIGHT - 100) {
                pdfDocument.finishPage(page2)
                val newPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 3).create())
                currentY = MARGIN + 20
                currentY = drawItemAnalysisHeader(newPage.canvas, currentY)
                currentY += 5
            }
        }
        
        pdfDocument.finishPage(page2)
        
        // PAGE 3: Action Plan - Intervention Groups
        val page3 = pdfDocument.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 3).create())
        val canvas3 = page3.canvas
        currentY = MARGIN + 20
        
        currentY = drawSection(canvas3, "👥 INTERVENTION GROUPS", currentY, bold = true, size = 18f)
        currentY += 10
        
        currentY = drawText(
            canvas3,
            "Students grouped by competency gaps for targeted remediation",
            currentY,
            size = 12f,
            color = android.graphics.Color.GRAY
        )
        currentY += 30
        
        interventionGroups.forEach { group ->
            currentY = drawInterventionGroup(canvas3, group, currentY)
            currentY += 20
            
            if (currentY > PAGE_HEIGHT - 150) {
                pdfDocument.finishPage(page3)
                val newPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 4).create())
                currentY = MARGIN + 20
            }
        }
        
        // Action Plan Summary
        currentY += 20
        currentY = drawSection(canvas3, "📅 RECOMMENDED SCHEDULE", currentY, bold = true)
        currentY += 15
        
        reteachPriorities.take(3).forEachIndexed { index, priority ->
            val week = when (priority.priority) {
                Priority.URGENT -> "This Week"
                Priority.SOON -> "Next 2 Weeks"
                Priority.MONITOR -> "Monitor"
            }
            currentY = drawText(
                canvas3,
                "${priority.priority.icon} Week ${index + 1}: Reteach ${priority.competency.code} to ${priority.affectedStudentCount} students",
                currentY,
                size = 11f
            )
            currentY += 15
        }
        
        pdfDocument.finishPage(page3)
        
        // Save PDF
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "SmartReport_${examName.replace(" ", "_")}_$timestamp.pdf"
        val file = File(context.getExternalFilesDir(null), fileName)
        
        FileOutputStream(file).use { outputStream ->
            pdfDocument.writeTo(outputStream)
        }
        
        pdfDocument.close()
        return file
    }
    
    private fun drawHeader(canvas: android.graphics.Canvas, examName: String, startY: Int): Int {
        var y = startY
        
        // App name
        val paint = Paint().apply {
            color = android.graphics.Color.parseColor("#007AFF")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Grade Smart - AI Report", MARGIN.toFloat(), y.toFloat(), paint)
        y += 30
        
        // Exam name
        paint.textSize = 20f
        paint.color = android.graphics.Color.BLACK
        canvas.drawText(examName, MARGIN.toFloat(), y.toFloat(), paint)
        y += 20
        
        // Date
        paint.textSize = 12f
        paint.color = android.graphics.Color.GRAY
        paint.typeface = Typeface.DEFAULT
        val dateStr = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
        canvas.drawText("Generated: $dateStr", MARGIN.toFloat(), y.toFloat(), paint)
        y += 10
        
        // Divider line
        paint.color = android.graphics.Color.LTGRAY
        paint.strokeWidth = 2f
        canvas.drawLine(
            MARGIN.toFloat(),
            y.toFloat(),
            (PAGE_WIDTH - MARGIN).toFloat(),
            y.toFloat(),
            paint
        )
        
        return y
    }
    
    private fun drawSection(
        canvas: android.graphics.Canvas,
        title: String,
        startY: Int,
        bold: Boolean = false,
        size: Float = 14f
    ): Int {
        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = size
            if (bold) typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(title, MARGIN.toFloat(), startY.toFloat(), paint)
        return startY + 20
    }
    
    private fun drawText(
        canvas: android.graphics.Canvas,
        text: String,
        startY: Int,
        size: Float = 12f,
        color: Int = android.graphics.Color.BLACK,
        bold: Boolean = false
    ): Int {
        val paint = Paint().apply {
            this.color = color
            textSize = size
            if (bold) typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        
        // Handle text wrapping
        val maxWidth = PAGE_WIDTH - (MARGIN * 2)
        val words = text.split(" ")
        var currentLine = ""
        var y = startY
        
        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) > maxWidth) {
                canvas.drawText(currentLine, MARGIN.toFloat(), y.toFloat(), paint)
                y += LINE_HEIGHT
                currentLine = word
            } else {
                currentLine = testLine
            }
        }
        
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, MARGIN.toFloat(), y.toFloat(), paint)
            y += LINE_HEIGHT
        }
        
        return y
    }
    
    private fun drawReteachPriority(
        canvas: android.graphics.Canvas,
        rank: Int,
        priority: ReteachPriority,
        startY: Int
    ): Int {
        var y = startY
        
        // Priority box
        val boxColor = when (priority.priority) {
            Priority.URGENT -> android.graphics.Color.parseColor("#FF3B30")
            Priority.SOON -> android.graphics.Color.parseColor("#FF9500")
            Priority.MONITOR -> android.graphics.Color.parseColor("#34C759")
        }
        
        val paint = Paint().apply {
            color = boxColor
            style = Paint.Style.FILL
        }
        
        canvas.drawRoundRect(
            MARGIN.toFloat(),
            (y - 15).toFloat(),
            (PAGE_WIDTH - MARGIN).toFloat(),
            (y + 80).toFloat(),
            10f,
            10f,
            paint
        )
        
        // White background for text
        paint.color = android.graphics.Color.WHITE
        canvas.drawRoundRect(
            (MARGIN + 5).toFloat(),
            (y - 10).toFloat(),
            (PAGE_WIDTH - MARGIN - 5).toFloat(),
            (y + 75).toFloat(),
            8f,
            8f,
            paint
        )
        
        // Priority label
        paint.color = boxColor
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(
            "$rank. ${priority.priority.label}",
            (MARGIN + 15).toFloat(),
            (y + 5).toFloat(),
            paint
        )
        
        // Mastery percentage
        canvas.drawText(
            "${priority.masteryPercentage}% Mastery",
            (PAGE_WIDTH - MARGIN - 120).toFloat(),
            (y + 5).toFloat(),
            paint
        )
        
        // Competency code
        paint.color = android.graphics.Color.parseColor("#007AFF")
        paint.textSize = 11f
        y += 25
        canvas.drawText(priority.competency.code, (MARGIN + 15).toFloat(), y.toFloat(), paint)
        
        // Description
        paint.color = android.graphics.Color.BLACK
        paint.typeface = Typeface.DEFAULT
        y += 15
        val desc = if (priority.competency.description.length > 60) {
            priority.competency.description.take(60) + "..."
        } else {
            priority.competency.description
        }
        canvas.drawText(desc, (MARGIN + 15).toFloat(), y.toFloat(), paint)
        
        // Affected info
        paint.color = android.graphics.Color.GRAY
        paint.textSize = 10f
        y += 20
        canvas.drawText(
            "Questions: ${priority.affectedQuestions.joinToString(",")} | Students: ${priority.affectedStudentCount}",
            (MARGIN + 15).toFloat(),
            y.toFloat(),
            paint
        )
        
        return y + 15
    }
    
    private fun drawItemAnalysisHeader(canvas: android.graphics.Canvas, startY: Int): Int {
        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        
        canvas.drawText("Q#", (MARGIN + 10).toFloat(), startY.toFloat(), paint)
        canvas.drawText("Key", (MARGIN + 50).toFloat(), startY.toFloat(), paint)
        canvas.drawText("Correct", (MARGIN + 100).toFloat(), startY.toFloat(), paint)
        canvas.drawText("Difficulty", (MARGIN + 180).toFloat(), startY.toFloat(), paint)
        canvas.drawText("Discrimination", (MARGIN + 280).toFloat(), startY.toFloat(), paint)
        canvas.drawText("Level", (MARGIN + 400).toFloat(), startY.toFloat(), paint)
        
        // Draw line
        paint.strokeWidth = 1f
        canvas.drawLine(
            MARGIN.toFloat(),
            (startY + 5).toFloat(),
            (PAGE_WIDTH - MARGIN).toFloat(),
            (startY + 5).toFloat(),
            paint
        )
        
        return startY + 15
    }
    
    private fun drawItemAnalysisRow(canvas: android.graphics.Canvas, item: ItemAnalysisData, startY: Int): Int {
        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
        }
        
        // Question number
        canvas.drawText("${item.questionNumber}", (MARGIN + 15).toFloat(), startY.toFloat(), paint)
        
        // Key
        canvas.drawText(item.correctAnswer, (MARGIN + 55).toFloat(), startY.toFloat(), paint)
        
        // Correct count
        canvas.drawText(
            "${item.correctCount}/${item.totalStudents}",
            (MARGIN + 100).toFloat(),
            startY.toFloat(),
            paint
        )
        
        // Difficulty Index
        canvas.drawText(
            String.format("%.2f", item.difficultyIndex),
            (MARGIN + 190).toFloat(),
            startY.toFloat(),
            paint
        )
        
        // Discrimination Index
        canvas.drawText(
            String.format("%.2f", item.discriminationIndex),
            (MARGIN + 300).toFloat(),
            startY.toFloat(),
            paint
        )
        
        // Level (colored)
        paint.color = item.difficulty.color
        canvas.drawText(
            "${item.difficulty.icon} ${item.difficulty.label}",
            (MARGIN + 400).toFloat(),
            startY.toFloat(),
            paint
        )
        
        return startY + LINE_HEIGHT
    }
    
    private fun drawInterventionGroup(
        canvas: android.graphics.Canvas,
        group: InterventionGroup,
        startY: Int
    ): Int {
        var y = startY
        
        // Competency header
        val paint = Paint().apply {
            color = android.graphics.Color.parseColor("#007AFF")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(group.competency.code, MARGIN.toFloat(), y.toFloat(), paint)
        
        paint.color = android.graphics.Color.parseColor("#FF3B30")
        canvas.drawText(
            "${group.masteryPercentage}% mastery",
            (PAGE_WIDTH - MARGIN - 100).toFloat(),
            y.toFloat(),
            paint
        )
        
        // Description
        paint.color = android.graphics.Color.BLACK
        paint.textSize = 11f
        paint.typeface = Typeface.DEFAULT
        y += 18
        y = drawText(canvas, group.competency.description, y, size = 10f)
        y += 5
        
        // Students
        paint.color = android.graphics.Color.GRAY
        paint.textSize = 10f
        canvas.drawText(
            "Students (${group.students.size}): ${group.students.take(5).joinToString(", ") { it.student.name }}${if (group.students.size > 5) "..." else ""}",
            MARGIN.toFloat(),
            y.toFloat(),
            paint
        )
        
        return y + 15
    }
}
