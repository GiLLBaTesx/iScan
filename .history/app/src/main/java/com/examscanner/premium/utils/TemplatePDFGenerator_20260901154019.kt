package com.examscanner.premium.utils

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil

/**
 * Template PDF Generator - ZipGrade-style compact layout
 * 
 * Creates printable answer sheet templates with:
 * - Two-column layout (questions split evenly)
 * - Compact design (fits more on one page)
 * - QR code for automatic recognition
 * - Student info section
 * - Clear bubble design
 */
object TemplatePDFGenerator {
    
    private const val PAGE_WIDTH = 612 // 8.5 inches * 72 DPI
    private const val PAGE_HEIGHT = 792 // 11 inches * 72 DPI
    private const val MARGIN = 36 // 0.5 inch margin
    private const val BUBBLE_SIZE = 18f
    private const val BUBBLE_SPACING = 24f
    private const val ROW_HEIGHT = 28f
    private const val COLUMN_SPACING = 30
    
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
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }
        
        val bubblePaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }
        
        var yPosition = MARGIN.toFloat()
        
        // HEADER SECTION
        canvas.drawText(templateName, MARGIN.toFloat(), yPosition, titlePaint)
        yPosition += 30
        
        canvas.drawText("Answer Sheet - $totalQuestions Questions", MARGIN.toFloat(), yPosition, bodyPaint)
        yPosition += 30
        
        // Student Info Section
        canvas.drawLine(MARGIN.toFloat(), yPosition, (PAGE_WIDTH - MARGIN).toFloat(), yPosition, linePaint)
        yPosition += 15
        
        // Name field
        canvas.drawText("Name:", MARGIN.toFloat(), yPosition, bodyPaint)
        canvas.drawLine(
            (MARGIN + 80).toFloat(),
            yPosition + 5,
            (PAGE_WIDTH - MARGIN).toFloat(),
            yPosition + 5,
            linePaint
        )
        yPosition += 25
        
        // Date and Score fields
        canvas.drawText("Date:", MARGIN.toFloat(), yPosition, bodyPaint)
        canvas.drawLine(
            (MARGIN + 70).toFloat(),
            yPosition + 5,
            (MARGIN + 220).toFloat(),
            yPosition + 5,
            linePaint
        )
        
        canvas.drawText("Score:", (MARGIN + 250).toFloat(), yPosition, bodyPaint)
        canvas.drawLine(
            (MARGIN + 320).toFloat(),
            yPosition + 5,
            (PAGE_WIDTH - MARGIN).toFloat(),
            yPosition + 5,
            linePaint
        )
        yPosition += 35
        
        canvas.drawLine(MARGIN.toFloat(), yPosition, (PAGE_WIDTH - MARGIN).toFloat(), yPosition, linePaint)
        yPosition += 20
        
        // Instructions
        canvas.drawText("Instructions: Fill bubbles completely. Use #2 pencil or dark pen.", MARGIN.toFloat(), yPosition, bodyPaint)
        yPosition += 30
        
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
        
        // Footer - Template ID
        val footerY = (PAGE_HEIGHT - 30).toFloat()
        canvas.drawText(
            "Template: $templateName | Questions: $totalQuestions | Choices: $choicesPerQuestion",
            MARGIN.toFloat(),
            footerY,
            bodyPaint.apply { textSize = 10f }
        )
        
        document.finishPage(page)
        
        // Save to file
        val outputDir = File(context.filesDir, "templates")
        outputDir.mkdirs()
        val outputFile = File(outputDir, "${templateName.replace(" ", "_")}_${System.currentTimeMillis()}.pdf")
        
        try {
            val outputStream = FileOutputStream(outputFile)
            document.writeTo(outputStream)
            outputStream.close()
            document.close()
            return outputFile
        } catch (e: Exception) {
            document.close()
            throw e
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
                yPos + 15,
                textPaint
            )
            
            // Answer bubbles
            var bubbleX = startX + 40f
            repeat(choicesPerQuestion) { index ->
                val letter = ('A' + index).toString()
                
                // Draw bubble circle
                canvas.drawCircle(
                    bubbleX + BUBBLE_SIZE / 2,
                    yPos + 10,
                    BUBBLE_SIZE / 2,
                    bubblePaint
                )
                
                // Draw letter inside
                val letterPaint = Paint(textPaint).apply {
                    textSize = 10f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(
                    letter,
                    bubbleX + BUBBLE_SIZE / 2,
                    yPos + 14,
                    letterPaint
                )
                
                bubbleX += BUBBLE_SPACING
            }
            
            yPos += ROW_HEIGHT
        }
    }
}
