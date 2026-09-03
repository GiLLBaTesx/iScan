package com.examscanner.premium.qrcode

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import com.examscanner.premium.utils.SecureLogger
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/**
 * QRCodeGenerator - Domain service for encoding exam metadata into QR codes.
 *
 * Encodes an [ExamMetadata] payload as JSON and renders it to a QR code using ZXing.
 * The service exposes two outputs:
 *  - [generateQRCode]: a [Bitmap] for on-screen preview / direct drawing
 *  - [generateQRCodeBase64]: a Base64-encoded PNG string for embedding into generated PDFs
 *
 * The JSON payload is intentionally stable and self-describing so that [QRCodeParser]
 * (task 5.2) can decode it via [ExamMetadata.fromJson] without a shared serialization runtime.
 *
 * Note: The project does not include the kotlinx-serialization plugin/runtime (verified in
 * app/build.gradle), so encoding uses the bundled org.json API to match existing conventions
 * (see data/BuiltInData.kt). ZXing (com.google.zxing:core) is available and used for QR encoding.
 *
 * Requirements: 4.1 (embed exam ID/metadata via QR), 4.3 (fixed QR placement/embeddable output).
 */
class QRCodeGenerator {

    /**
     * Metadata embedded into an exam's QR code.
     *
     * This class is public and lives here (as [QRCodeGenerator.ExamMetadata]) so that the
     * companion [QRCodeParser] can reference and decode the exact same shape.
     *
     * @param examId Room primary key of the exam.
     * @param examName Human-readable exam name.
     * @param totalQuestions Number of questions on the answer sheet.
     * @param subjectId Room primary key of the owning subject folder.
     * @param createdAt Creation timestamp (epoch millis).
     * @param version Payload schema version, for future forward-compatibility.
     */
    data class ExamMetadata(
        val examId: Long,
        val examName: String,
        val totalQuestions: Int,
        val subjectId: Long,
        val createdAt: Long,
        val version: Int = 1
    ) {
        /** Serializes this metadata to a compact, stable JSON string. */
        fun toJson(): String = JSONObject().apply {
            put(KEY_EXAM_ID, examId)
            put(KEY_EXAM_NAME, examName)
            put(KEY_TOTAL_QUESTIONS, totalQuestions)
            put(KEY_SUBJECT_ID, subjectId)
            put(KEY_CREATED_AT, createdAt)
            put(KEY_VERSION, version)
        }.toString()

        companion object {
            private const val KEY_EXAM_ID = "examId"
            private const val KEY_EXAM_NAME = "examName"
            private const val KEY_TOTAL_QUESTIONS = "totalQuestions"
            private const val KEY_SUBJECT_ID = "subjectId"
            private const val KEY_CREATED_AT = "createdAt"
            private const val KEY_VERSION = "version"

            /**
             * Parses metadata from a JSON string produced by [toJson].
             *
             * @return the decoded [ExamMetadata], or null if the payload is malformed or
             *         missing required fields.
             */
            fun fromJson(json: String): ExamMetadata? {
                return try {
                    val obj = JSONObject(json)
                    ExamMetadata(
                        examId = obj.getLong(KEY_EXAM_ID),
                        examName = obj.getString(KEY_EXAM_NAME),
                        totalQuestions = obj.getInt(KEY_TOTAL_QUESTIONS),
                        subjectId = obj.getLong(KEY_SUBJECT_ID),
                        createdAt = obj.getLong(KEY_CREATED_AT),
                        version = if (obj.has(KEY_VERSION)) obj.getInt(KEY_VERSION) else 1
                    )
                } catch (e: Exception) {
                    SecureLogger.e(TAG, "Failed to parse ExamMetadata from JSON", e)
                    null
                }
            }
        }
    }

    /**
     * Generates a QR code [Bitmap] containing the encoded [metadata].
     *
     * Uses high error correction (H) so the code remains scannable after printing and
     * light smudging. The default [size] of 200px is suitable for A4 answer-sheet headers.
     *
     * @param metadata exam metadata to encode.
     * @param size square dimension of the produced bitmap, in pixels.
     * @return a black-on-white QR [Bitmap] of [size] x [size].
     */
    fun generateQRCode(metadata: ExamMetadata, size: Int = DEFAULT_SIZE): Bitmap {
        val json = metadata.toJson()

        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 1
        )

        val bitMatrix = MultiFormatWriter().encode(
            json,
            BarcodeFormat.QR_CODE,
            size,
            size,
            hints
        )

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    /**
     * Generates the QR code as a Base64-encoded PNG string, suitable for embedding into
     * generated PDF answer sheets (the pretty printer places it in the fixed header slot).
     *
     * @param metadata exam metadata to encode.
     * @param size square dimension of the underlying bitmap, in pixels.
     * @return a Base64 (NO_WRAP) string of the PNG-compressed QR bitmap.
     */
    fun generateQRCodeBase64(metadata: ExamMetadata, size: Int = DEFAULT_SIZE): String {
        val bitmap = generateQRCode(metadata, size)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    companion object {
        private const val TAG = "QRCodeGenerator"

        /** Default QR bitmap dimension in pixels (suitable for A4 printing). */
        const val DEFAULT_SIZE = 200
    }
}
