package com.examscanner.premium.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

data class ScannedResult(
    val studentId: String,
    val studentName: String,
    val answers: List<Pair<Int, String>> // Question number to answer
)

class BubbleSheetProcessor(private val context: Context) {
    
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    suspend fun processImage(uri: Uri): ScannedResult? {
        try {
            val bitmap = loadBitmap(uri) ?: return null
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            
            val visionText = textRecognizer.process(inputImage).await()
            
            // Extract student info
            var studentId = ""
            var studentName = ""
            val answers = mutableListOf<Pair<Int, String>>()
            
            // Simple text extraction logic
            visionText.textBlocks.forEach { block ->
                val text = block.text
                
                // Look for ID pattern (numbers)
                if (text.matches(Regex("\\d{5,}"))) {
                    studentId = text
                }
                
                // Look for name (capitalize words)
                if (text.matches(Regex("[A-Z][a-z]+ [A-Z][a-z]+"))) {
                    studentName = text
                }
            }
            
            // Simulate bubble detection (in real implementation, use image processing)
            // For now, generate mock answers
            for (i in 1..20) {
                answers.add(i to listOf("A", "B", "C", "D", "E").random())
            }
            
            return ScannedResult(
                studentId = studentId.ifEmpty { generateRandomId() },
                studentName = studentName.ifEmpty { "Student ${generateRandomId()}" },
                answers = answers
            )
            
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    private fun loadBitmap(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun generateRandomId(): String {
        return (10000..99999).random().toString()
    }
    
    fun cleanup() {
        textRecognizer.close()
    }
}
