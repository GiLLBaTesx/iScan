package com.examscanner.premium.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream

object PdfSaveUtility {
    
    /**
     * Save PDF to Downloads folder (works on all Android versions)
     */
    fun savePdfToDownloads(
        context: Context,
        sourceFile: File,
        displayName: String
    ): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (API 29+) - Use MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                
                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        FileInputStream(sourceFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    true
                } ?: false
            } else {
                // Android 9 and below - Use legacy external storage
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )
                
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                
                val destFile = File(downloadsDir, displayName)
                sourceFile.copyTo(destFile, overwrite = true)
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Save PDF and show toast notification
     */
    fun savePdfWithNotification(
        context: Context,
        sourceFile: File,
        displayName: String
    ) {
        val success = savePdfToDownloads(context, sourceFile, displayName)
        
        if (success) {
            Toast.makeText(
                context,
                "PDF saved to Downloads/$displayName",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                context,
                "Failed to save PDF. Check permissions.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
