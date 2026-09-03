package com.examscanner.premium.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import android.util.Log
import java.io.File
import java.io.FileInputStream

object PdfSaveUtility {
    
    private const val TAG = "PdfSaveUtility"
    
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
                // Android 10+ (API 29+) - Use MediaStore (no permission needed)
                Log.d(TAG, "Using MediaStore for Android 10+")
                
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        FileInputStream(sourceFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    Log.d(TAG, "File saved successfully via MediaStore: $uri")
                    true
                } else {
                    Log.e(TAG, "Failed to create MediaStore entry")
                    false
                }
            } else {
                // Android 9 and below - Use app-specific external storage (no permission needed)
                Log.d(TAG, "Using app external storage for Android 9 and below")
                
                // Use app-specific directory which doesn't require permission
                val appDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                if (appDir != null) {
                    if (!appDir.exists()) {
                        appDir.mkdirs()
                    }
                    val destFile = File(appDir, displayName)
                    sourceFile.copyTo(destFile, overwrite = true)
                    Log.d(TAG, "File saved to: ${destFile.absolutePath}")
                    true
                } else {
                    Log.e(TAG, "External files dir is null")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving PDF", e)
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
        try {
            val success = savePdfToDownloads(context, sourceFile, displayName)
            
            if (success) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Toast.makeText(
                        context,
                        "✓ PDF saved to Downloads/$displayName",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "✓ PDF saved to app files",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                Toast.makeText(
                    context,
                    "✗ Failed to save PDF",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in savePdfWithNotification", e)
            Toast.makeText(
                context,
                "✗ Error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
