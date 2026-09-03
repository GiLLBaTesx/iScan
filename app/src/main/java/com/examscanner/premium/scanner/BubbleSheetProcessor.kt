package com.examscanner.premium.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.examscanner.premium.qrcode.QRCodeParser
import com.examscanner.premium.utils.AnswerSheetPrettyPrinter
import com.examscanner.premium.utils.SecureLogger
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
    val answers: List<Pair<Int, String>>
)

class BubbleSheetProcessor(private val context: Context) {
    
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    /**
     * Real bubble-sheet processing for an app-printed answer sheet (Req 4.7, 17.1, 22.3).
     *
     * Runs the actual detection pipeline instead of the previous mock:
     *  1. Load + cap the scanned bitmap to 1920×1080 (Req 19.4).
     *  2. Rebuild the exact printed layout via [AnswerSheetPrettyPrinter.buildSheetModel]
     *     (pure; no PDF/Context needed) from the exam's [totalQuestions]/[optionLabels].
     *  3. Map that layout to a pixel-space grid via [BubbleGridMapper.buildGrid].
     *  4. Run [AnswerSheetParser.parse], which does QR-first identification and real
     *     bubble fill classification via [BubbleDetectionEngine] (reusing THIS processor
     *     for ML Kit text extraction, so there's no duplicate recognizer).
     *  5. Map the [AnswerSheetParser.ParseResult] to a [ScannedResult].
     *
     * Resolved answers may be the [AnswerSheetParser.NO_ANSWER] / [AnswerSheetParser.INVALID_MULTIPLE]
     * sentinels; they are kept as-is (the grading layer treats non-key-matching answers as
     * wrong), never replaced with random guesses.
     *
     * @param uri captured sheet image.
     * @param totalQuestions the exam's expected question count (drives the grid geometry).
     * @param optionLabels the exam's option labels (e.g. A–D); its size sets the option count.
     */
    suspend fun processImage(
        uri: Uri,
        totalQuestions: Int,
        optionLabels: List<String>
    ): ScannedResult? = withContext(Dispatchers.IO) {
        try {
            val bitmap = loadBitmap(uri) ?: return@withContext null
            val capped = capImageResolution(bitmap)

            // Rebuild the printed layout (pure — no PDF I/O, no Context).
            val model = AnswerSheetPrettyPrinter(qrGenerator = null, context = null)
                .buildSheetModel(
                    AnswerSheetPrettyPrinter.SheetConfig(
                        examName = "",
                        totalQuestions = totalQuestions,
                        optionsCount = optionLabels.size,
                        optionsLabels = optionLabels,
                    )
                )

            // Map printed point-space geometry to the scanned bitmap's pixel space, using the
            // SAME page size the model selected (deterministic for the same totalQuestions/
            // optionLabels). The mapper maps only the first page's questions (single-page scan).
            val grid = BubbleGridMapper.buildGrid(
                model,
                capped.width,
                capped.height,
                model.pageWidthPt,
                model.pageHeightPt
            )

            // Real parse: QR-first identification + bubble fill classification. Reuse THIS
            // processor for ML Kit text extraction (no duplicate recognizer client).
            val parseResult = AnswerSheetParser(
                QRCodeParser(),
                BubbleDetectionEngine(this@BubbleSheetProcessor)
            ).parse(capped, grid)

            val studentId = parseResult.studentId ?: generateRandomId()
            val studentName = parseResult.studentName ?: "Student $studentId"
            val answers = parseResult.answers.map { it.questionNumber to it.answer }

            return@withContext ScannedResult(
                studentId = studentId,
                studentName = studentName,
                answers = answers
            )
        } catch (e: Exception) {
            SecureLogger.e(TAG, "processImage failed", e)
            return@withContext null
        }
    }

    /**
     * Holder for text extracted from an answer sheet via ML Kit.
     */
    data class ExtractedText(
        val studentId: String,
        val studentName: String
    )

    /**
     * Load a bitmap from a content [Uri]. Public so collaborators
     * (e.g. [com.examscanner.premium.scanner.BubbleDetectionEngine]) can
     * reuse the same image-loading path rather than duplicating it.
     */
    fun loadBitmap(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            SecureLogger.e(TAG, "loadBitmap failed", e)
            null
        }
    }

    /**
     * Extract student id / name from a bitmap using the shared ML Kit
     * text recognizer (Req 17.2). Returns empty strings when nothing matches.
     */
    suspend fun extractStudentInfo(bitmap: Bitmap): ExtractedText {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val visionText = suspendCoroutine<com.google.mlkit.vision.text.Text> { continuation ->
                textRecognizer.process(inputImage)
                    .addOnSuccessListener { result -> continuation.resume(result) }
                    .addOnFailureListener { e -> continuation.resumeWithException(e) }
            }

            var studentId = ""
            var studentName = ""
            for (block in visionText.textBlocks) {
                val text = block.text
                if (text.matches(Regex("\\d{5,}"))) {
                    studentId = text
                }
                if (text.matches(Regex("[A-Z][a-z]+ [A-Z][a-z]+"))) {
                    studentName = text
                }
            }
            ExtractedText(studentId, studentName)
        } catch (e: Exception) {
            SecureLogger.e(TAG, "extractStudentInfo failed", e)
            ExtractedText("", "")
        }
    }

    private fun generateRandomId(): String {
        return (10000..99999).random().toString()
    }

    fun cleanup() {
        textRecognizer.close()
    }

    companion object {
        private const val TAG = "BubbleSheetProcessor"

        /** Requirement 19.4: cap scanned images to 1920×1080 to reduce memory. */
        const val MAX_IMAGE_WIDTH = 1920
        const val MAX_IMAGE_HEIGHT = 1080

        /**
         * Downscale [bitmap] so it fits within [MAX_IMAGE_WIDTH]×[MAX_IMAGE_HEIGHT]
         * while preserving aspect ratio. Returns the original bitmap if it already
         * fits. Orientation-agnostic: the longer side is capped to 1920 and the
         * shorter side to 1080.
         */
        fun capImageResolution(
            bitmap: Bitmap,
            maxLongSide: Int = MAX_IMAGE_WIDTH,
            maxShortSide: Int = MAX_IMAGE_HEIGHT
        ): Bitmap {
            val width = bitmap.width
            val height = bitmap.height
            if (width <= 0 || height <= 0) return bitmap

            val longSide = maxOf(width, height)
            val shortSide = minOf(width, height)
            val scale = minOf(
                maxLongSide.toFloat() / longSide,
                maxShortSide.toFloat() / shortSide
            )
            if (scale >= 1f) return bitmap

            val newWidth = maxOf(1, (width * scale).toInt())
            val newHeight = maxOf(1, (height * scale).toInt())
            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }
    }
}
