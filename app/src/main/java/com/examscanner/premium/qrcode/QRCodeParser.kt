package com.examscanner.premium.qrcode

import android.graphics.Bitmap
import com.examscanner.premium.utils.SecureLogger
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * QRCodeParser - Decodes QR codes from scanned answer-sheet images.
 *
 * Uses ML Kit Barcode Scanning to detect a QR code in a [Bitmap] and decodes
 * the embedded JSON payload into a [QRCodeGenerator.ExamMetadata]. This is the
 * inverse of [QRCodeGenerator.generateQRCode], enabling scans to be associated
 * automatically with the correct exam.
 *
 * The payload is decoded via [QRCodeGenerator.ExamMetadata.fromJson], which uses the
 * bundled org.json API. The project does not include the kotlinx-serialization
 * plugin/runtime (verified in app/build.gradle), so this matches existing conventions
 * (see data/BuiltInData.kt).
 *
 * Requirements: 4.4 (attempt QR detection before bubble processing),
 * 4.5 (decode metadata on detection), 4.7 (graceful fallback on failure),
 * 22.1/22.2 (fast QR detection and metadata extraction).
 */
class QRCodeParser {

    /**
     * Scans an image for a QR code and extracts the embedded exam metadata.
     *
     * @param bitmap the captured/scanned image to inspect
     * @return the decoded [QRCodeGenerator.ExamMetadata], or null if no QR code
     *         is present or the payload cannot be parsed. Never throws.
     */
    suspend fun parseQRCode(bitmap: Bitmap): QRCodeGenerator.ExamMetadata? =
        withContext(Dispatchers.Default) {
            val image = InputImage.fromBitmap(bitmap, 0)
            val scanner = BarcodeScanning.getClient()

            try {
                val barcodes = scanner.process(image).await()

                // Find the first QR code and extract its raw payload.
                val qrCode = barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }
                val rawValue = qrCode?.rawValue ?: return@withContext null

                // Decode the JSON payload into exam metadata.
                QRCodeGenerator.ExamMetadata.fromJson(rawValue)
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Failed to parse QR code", e)
                null
            } finally {
                scanner.close()
            }
        }

    /**
     * Fast check for the presence of a decodable QR code in the image.
     *
     * @param bitmap the captured/scanned image to inspect
     * @return true if a QR code with valid exam metadata is detected, else false
     */
    suspend fun hasQRCode(bitmap: Bitmap): Boolean {
        return parseQRCode(bitmap) != null
    }

    companion object {
        private const val TAG = "QRCodeParser"
    }
}
