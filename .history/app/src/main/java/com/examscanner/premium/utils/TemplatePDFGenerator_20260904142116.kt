package com.examscanner.premium.utils

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil

/**
 * Template PDF Generator - Efficient 2-per-page layout
 * 
 * Creates printable answer sheet templates with:
 * - TWO templates per page (top half + bottom half) - SAVES PAPER!
 * - Dashed cut line between templates with scissors icon
 * - Two-column layout per template (questions split evenly)
 * - Compact design for maximum efficiency
 * - Student info section on each template
 * - Clear bubble design
 */
object TemplatePDFGenerator {
    
    private const val PAGE_WIDTH = 612 // 8.5 inches * 72 DPI
    private const val PAGE_HEIGHT = 792 // 11 inches * 72 DPI
    private const val MARGIN = 30 // Reduced margin for more space
    private const val TEMPLATE_HEIGHT = (PAGE_HEIGHT - 10) / 2 // Split page in half with 10px gap
    private const val BUBBLE_SIZE = 16f // Slightly smaller for compact design
    private const val BUBBLE_SPACING = 22f
    private const val ROW_HEIGHT = 24f // Tighter spacing
    private const val COLUMN_SPACING = 25
    
    fun generateTemplate(
        context: Context,
        templateName: String,
        totalQuestions: Int,
        choicesPerQuestion: Int
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        
        // Paint objects
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f // Smaller title for compact design
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f // Smaller text for compact design
            isAntiAlias = true
        }
        
        val bubblePaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }
        
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }
        
        val dashedLinePaint = Paint().apply {
            color = Color.GRAY
            strokeWidth = 1.5f
            pathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
        }
        
        // Draw TOP template (first half)
        drawSingleTemplate(
            canvas,
            0f,
            1,
            templateName,
            totalQuestions,
            choicesPerQuestion,
            titlePaint,
            headerPaint,
            bodyPaint,
            bubblePaint,
            linePaint
        )
        
        // Draw cut line with scissors icon
        val cutLineY = TEMPLATE_HEIGHT.toFloat() + 5
        canvas.drawLine(0f, cutLineY, PAGE_WIDTH.toFloat(), cutLineY, dashedLinePaint)
        
        // Add "✂ CUT HERE ✂" text
        val cutPaint = Paint(bodyPaint).apply {
            textSize = 10f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("✂ CUT HERE ✂", PAGE_WIDTH / 2f, cutLineY - 3, cutPaint)
        
        // Draw BOTTOM template (second half)
        drawSingleTemplate(
            canvas,
            TEMPLATE_HEIGHT.toFloat() + 10,
            2,
            templateName,
            totalQuestions,
            choicesPerQuestion,
            titlePaint,
            headerPaint,
            bodyPaint,
            bubblePaint,
            linePaint
        )
        
        document.finishPage(page)
        
        // Save to external storage where FileProvider can access it
        val outputDir = File(context.getExternalFilesDir(null), "Templates")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        
        val timestamp = System.currentTimeMillis()
        val outputFile = File(outputDir, "${templateName.replace(" ", "_")}_2up_$timestamp.pdf")
        
        try {
            val outputStream = FileOutputStream(outputFile)
            document.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            document.close()
            return outputFile
        } catch (e: Exception) {
            document.close()
            throw Exception("Failed to generate template: ${e.message}", e)
        }
    }
    
    private fun drawSingleTemplate(
        canvas: Canvas,
        startY: Float,
        templateNumber: Int,
        templateName: String,
        totalQuestions: Int,
        choicesPerQuestion: Int,
        titlePaint: Paint,
        headerPaint: Paint,
        bodyPaint: Paint,
        bubblePaint: Paint,
        linePaint: Paint
    ) {
        var yPosition = startY + MARGIN
        
        // HEADER SECTION
        canvas.drawText("$templateName (#$templateNumber)", MARGIN.toFloat(), yPosition, titlePaint)
        yPosition += 20
        
        canvas.drawText("$totalQuestions Questions", MARGIN.toFloat(), yPosition, bodyPaint)
        yPosition += 20
        
        // Student Info Section
        canvas.drawLine(MARGIN.toFloat(), yPosition, (PAGE_WIDTH - MARGIN).toFloat(), yPosition, linePaint)
        yPosition += 12
        
        // Name field
        canvas.drawText("Name:", MARGIN.toFloat(), yPosition, bodyPaint)
        canvas.drawLine(
            (MARGIN + 60).toFloat(),
            yPosition + 3,
            (PAGE_WIDTH - MARGIN).toFloat(),
            yPosition + 3,
            linePaint
        )
        yPosition += 18
        
        // Date and Score fields
        canvas.drawText("Date:", MARGIN.toFloat(), yPosition, bodyPaint)
        canvas.drawLine(
            (MARGIN + 60).toFloat(),
            yPosition + 3,
            (MARGIN + 180).toFloat(),
            yPosition + 3,
            linePaint
        )
        
        canvas.drawText("Score:", (MARGIN + 200).toFloat(), yPosition, bodyPaint)
        canvas.drawLine(
            (MARGIN + 260).toFloat(),
            yPosition + 3,
            (PAGE_WIDTH - MARGIN).toFloat(),
            yPosition + 3,
            linePaint
        )
        yPosition += 20
        
        canvas.drawLine(MARGIN.toFloat(), yPosition, (PAGE_WIDTH - MARGIN).toFloat(), yPosition, linePaint)
        yPosition += 15
        
        // Instructions
        canvas.drawText("Instructions: Fill bubbles completely. Use #2 pencil.", MARGIN.toFloat(), yPosition, bodyPaint)
        yPosition += 18
        
        // QUESTIONS SECTION - TWO COLUMNS
        val questionsPerColumn = ceil(totalQuestions / 2.0).toInt()
        val columnWidth = (PAGE_WIDTH - (2 * MARGIN) - COLUMN_SPACING) / 2
        val startColumn1 = MARGIN
        val startColumn2 = MARGIN + columnWidth + COLUMN_SPACING
        
        val startYPosition = yPosition
        
        // Column 1
        drawQuestionColumn(
            canvas,
            startColumn1,
            startYPosition,
            1,
            minOf(questionsPerColumn, totalQuestions),
            choicesPerQuestion,
            bodyPaint,
            bubblePaint
        )
        
        // Column 2 (if needed)
        if (totalQuestions > questionsPerColumn) {
            drawQuestionColumn(
                canvas,
                startColumn2,
                startYPosition,
                questionsPerColumn + 1,
                totalQuestions,
                choicesPerQuestion,
                bodyPaint,
                bubblePaint
            )
        }
    }
    
    private fun drawQuestionColumn(
        canvas: Canvas,
        startX: Int,
        startY: Float,
        fromQuestion: Int,
        toQuestion: Int,
        choicesPerQuestion: Int,
        textPaint: Paint,
        bubblePaint: Paint
    ) {
        var yPos = startY
        
        for (questionNum in fromQuestion..toQuestion) {
            // Question number
            canvas.drawText(
                String.format("%2d.", questionNum),
                startX.toFloat(),
                yPos + 13,
                textPaint
            )
            
            // Answer bubbles
            var bubbleX = startX + 35f
            repeat(choicesPerQuestion) { index ->
                val letter = ('A' + index).toString()
                
                // Draw bubble circle
                canvas.drawCircle(
                    bubbleX + BUBBLE_SIZE / 2,
                    yPos + 8,
                    BUBBLE_SIZE / 2,
                    bubblePaint
                )
                
                // Draw letter inside
                val letterPaint = Paint(textPaint).apply {
                    textSize = 9f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(
                    letter,
                    bubbleX + BUBBLE_SIZE / 2,
                    yPos + 12,
                    letterPaint
                )
                
                bubbleX += BUBBLE_SPACING
            }
            
            yPos += ROW_HEIGHT
        }
    }
}
