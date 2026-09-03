package com.examscanner.premium.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.examscanner.premium.qrcode.QRCodeGenerator
import com.examscanner.premium.qrcode.QRCodeParser
import com.examscanner.premium.scanner.BubbleSheetProcessor
import com.examscanner.premium.scanner.ScannedResult
import com.examscanner.premium.ui.components.FloatingGlassCard
import com.examscanner.premium.ui.theme.*
import kotlinx.coroutines.launch

/**
 * ProcessingScreen - runs the QR-first scan pipeline over a captured answer sheet.
 *
 * Task 11.2 (Req 4.7, 22.3, 17.1): before running the existing [BubbleSheetProcessor],
 * the screen attempts QR detection ([QRCodeParser]) to auto-associate the scan with the
 * correct exam. When a QR code is decoded, [onExamDetected] is fired so the host can point
 * the save at the exam identified by the sheet itself. When QR detection fails (no code,
 * unreadable, or parser error), the flow degrades gracefully to the existing manual
 * exam-selection behavior driven by the caller's current exam context — the bubble
 * processing path is unchanged and always runs (Req 17.1: integrate, don't replace).
 *
 * @param imageUri captured sheet image.
 * @param totalQuestions the selected exam's expected question count; drives the real
 *   detection grid so answers are read from the actual bubble positions.
 * @param optionLabels the exam's option labels (defaults to A–D); its size sets the
 *   option count used to rebuild the printed layout.
 * @param onProcessingComplete fired with the scanned result to persist.
 * @param onCancel back / retry.
 * @param onExamDetected optional; invoked with decoded QR metadata when the sheet's QR
 *   code identifies an exam, allowing automatic exam association. No-op default keeps the
 *   manual-selection fallback intact for callers that don't wire it.
 */
@Composable
fun ProcessingScreen(
    imageUri: Uri?,
    totalQuestions: Int,
    optionLabels: List<String> = listOf("A", "B", "C", "D"),
    onProcessingComplete: (ScannedResult) -> Unit,
    onCancel: () -> Unit,
    onExamDetected: (QRCodeGenerator.ExamMetadata) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf<ScannedResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    // Task 11.2: QR-first association state. Null when the sheet had no readable QR code,
    // in which case the manual exam-selection fallback (caller's current exam) is used.
    var detectedExam by remember { mutableStateOf<QRCodeGenerator.ExamMetadata?>(null) }

    LaunchedEffect(imageUri) {
        if (imageUri != null) {
            scope.launch {
                try {
                    val processor = BubbleSheetProcessor(context)

                    // Req 4.7 / 22.1 / 22.3: QR-first. Attempt to auto-associate the exam
                    // from the sheet's QR code before scoring. Never fatal — any failure
                    // falls through to the existing manual-selection flow.
                    val qrMetadata = try {
                        processor.loadBitmap(imageUri)?.let { bitmap ->
                            val capped = BubbleSheetProcessor.capImageResolution(bitmap)
                            QRCodeParser().parseQRCode(capped)
                        }
                    } catch (e: Exception) {
                        null // graceful fallback to manual selection
                    }
                    if (qrMetadata != null) {
                        detectedExam = qrMetadata
                        onExamDetected(qrMetadata)
                    }

                    // Req 17.1: run the real bubble-detection scoring path against the
                    // selected exam's expected layout.
                    val scannedResult = processor.processImage(
                        imageUri,
                        totalQuestions = totalQuestions,
                        optionLabels = optionLabels
                    )
                    processor.cleanup()

                    if (scannedResult != null) {
                        result = scannedResult
                        isProcessing = false
                    } else {
                        error = "Failed to process image"
                        isProcessing = false
                    }
                } catch (e: Exception) {
                    error = e.message ?: "Unknown error"
                    isProcessing = false
                }
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite),
        contentAlignment = Alignment.Center
    ) {
        when {
            isProcessing -> {
                FloatingGlassCard(
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(40.dp)
                    ) {
                        CircularProgressIndicator(color = PrimaryBlue)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Processing answer sheet...",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                    }
                }
            }
            error != null -> {
                FloatingGlassCard(
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(40.dp)
                    ) {
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = ErrorRed
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = error!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onCancel,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("Try Again")
                        }
                    }
                }
            }
            result != null -> {
                FloatingGlassCard(
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(40.dp)
                    ) {
                        Text(
                            text = "✓ Scan Complete",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        // Task 11.2: show how the exam was associated (QR auto vs manual).
                        val qrExam = detectedExam
                        if (qrExam != null) {
                            Text(
                                text = "QR matched: ${qrExam.examName}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = ElectricBlue,
                                modifier = Modifier.semantics {
                                    contentDescription =
                                        "Exam automatically detected from QR code: ${qrExam.examName}"
                                }
                            )
                        } else {
                            Text(
                                text = "No QR code — using selected exam",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.semantics {
                                    contentDescription =
                                        "No QR code detected. Saving to the currently selected exam."
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Student: ${result!!.studentName}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "ID: ${result!!.studentId}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "${result!!.answers.size} answers detected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { onProcessingComplete(result!!) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("Save Results")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onCancel) {
                            Text("Cancel", color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}
