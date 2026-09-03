package com.examscanner.premium.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.examscanner.premium.scanner.BubbleSheetProcessor
import com.examscanner.premium.scanner.ScannedResult
import com.examscanner.premium.ui.components.FloatingGlassCard
import com.examscanner.premium.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ProcessingScreen(
    imageUri: Uri?,
    onProcessingComplete: (ScannedResult) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf<ScannedResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(imageUri) {
        if (imageUri != null) {
            scope.launch {
                try {
                    val processor = BubbleSheetProcessor(context)
                    val scannedResult = processor.processImage(imageUri)
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
                        Spacer(modifier = Modifier.height(24.dp))
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
