package com.examscanner.premium.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONObject

object QRCodeGenerator {
    
    fun generateExamQRCode(
        examId: Long,
        examName: String,
        questionCount: Int,
        timestamp: Long = System.currentTimeMillis()
    ): String {
        val json = JSONObject().apply {
            put("examId", examId)
            put("name", examName)
            put("questions", questionCount)
            put("timestamp", timestamp)
        }
        return json.toString()
    }
    
    fun generateQRBitmap(content: String, size: Int = 512): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        
        return bitmap
    }
    
    fun parseExamQRCode(qrContent: String): ExamQRData? {
        return try {
            val json = JSONObject(qrContent)
            ExamQRData(
                examId = json.getLong("examId"),
                examName = json.getString("name"),
                questionCount = json.getInt("questions"),
                timestamp = json.getLong("timestamp")
            )
        } catch (e: Exception) {
            null
        }
    }
}

data class ExamQRData(
    val examId: Long,
    val examName: String,
    val questionCount: Int,
    val timestamp: Long
)
