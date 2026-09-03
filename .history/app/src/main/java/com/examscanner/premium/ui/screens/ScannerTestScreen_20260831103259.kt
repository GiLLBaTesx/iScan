package com.examscanner.premium.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.content.FileProvider
import com.examscanner.premium.testing.ScannerTestUtility
import com.examscanner.premium.ui.components.FloatingGlassCard
import com.examscanner.premium.ui.components.GlassCard
import com.examscanner.premium.utils.PdfSaveUtility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerTestScreen(
    onBack: () -> Unit,
    onScanTestSheet: (expectedAnswers: List<String>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    
    // Responsive sizing
    val isSmallScreen = screenWidth < 360.dp
    val titleSize = if (isSmallScreen) 20.sp else 24.sp
    val bodySize = if (isSmallScreen) 13.sp else 14.sp
    val smallTextSize = if (isSmallScreen) 11.sp else 12.sp
    val iconSize = if (isSmallScreen) 20.dp else 24.dp
    val cardIconSize = if (isSmallScreen) 28.dp else 32.dp
    val padding = if (isSmallScreen) 12.dp else 20.dp
    
    var isGenerating by remember { mutableStateOf(false) }
    var showInstructions by remember { mutableStateOf(false) }
    var selectedTestCase by remember { mutableStateOf<ScannerTestUtility.TestAnswerSheet?>(null) }
    
    val testCases = remember {
        listOf(
            ScannerTestUtility.TestCases.allA(),
            ScannerTestUtility.TestCases.allB(),
            ScannerTestUtility.TestCases.alternating(),
            ScannerTestUtility.TestCases.random(),
            ScannerTestUtility.TestCases.diagonal()
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFAFAFC),
                        Color(0xFFF0F4F8)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(iconSize)
                    )
                }
                
                Spacer(modifier = Modifier.width(if (isSmallScreen) 4.dp else 8.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Scanner Accuracy Test",
                        fontSize = titleSize,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E),
                        maxLines = if (isSmallScreen) 2 else 1
                    )
                    Text(
                        text = "Test bubble detection accuracy",
                        fontSize = bodySize,
                        color = Color(0xFF8E8E93)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(if (isSmallScreen) 16.dp else 24.dp))
            
            // Instructions Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(if (isSmallScreen) 12.dp else 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(iconSize)
                        )
                        Spacer(modifier = Modifier.width(if (isSmallScreen) 8.dp else 12.dp))
                        Text(
                            text = "How to Test",
                            fontSize = if (isSmallScreen) 15.sp else 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1C1C1E)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(if (isSmallScreen) 8.dp else 12.dp))
                    
                    Text(
                        text = "1. Tap SHARE or SAVE on test case\n" +
                              "2. Print or display PDF on screen\n" +
                              "3. Tap SCAN TEST SHEET\n" +
                              "4. Point camera at the sheet\n" +
                              "5. View accuracy results",
                        fontSize = bodySize,
                        color = Color(0xFF3C3C43),
                        lineHeight = bodySize * 1.6f
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(if (isSmallScreen) 16.dp else 24.dp))
            
            Text(
                text = "Test Cases",
                fontSize = if (isSmallScreen) 15.sp else 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1C1E)
            )
            
            Spacer(modifier = Modifier.height(if (isSmallScreen) 8.dp else 12.dp))
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(if (isSmallScreen) 8.dp else 12.dp)
            ) {
                items(testCases) { testCase ->
                    TestCaseCard(
                        testCase = testCase,
                        isGenerating = isGenerating,
                        isSmallScreen = isSmallScreen,
                        onShare = {
                            scope.launch {
                                isGenerating = true
                                try {
                                    val file = withContext(Dispatchers.IO) {
                                        ScannerTestUtility.generateTestAnswerSheet(context, testCase)
                                    }
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        file
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Test Sheet"))
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Error: ${e.message}",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                } finally {
                                    isGenerating = false
                                }
                            }
                        },
                        onSave = {
                            scope.launch {
                                try {
                                    android.util.Log.d("ScannerTest", "Generating PDF for save...")
                                    val file = withContext(Dispatchers.IO) {
                                        ScannerTestUtility.generateTestAnswerSheet(context, testCase)
                                    }
                                    android.util.Log.d("ScannerTest", "PDF generated at: ${file.absolutePath}")
                                    
                                    val fileName = "test_${testCase.examName.replace(" ", "_").replace(":", "")}.pdf"
                                    android.util.Log.d("ScannerTest", "Attempting to save as: $fileName")
                                    
                                    withContext(Dispatchers.Main) {
                                        PdfSaveUtility.savePdfWithNotification(context, file, fileName)
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("ScannerTest", "Error saving PDF", e)
                                    withContext(Dispatchers.Main) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Save failed: ${e.message}",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        },
                        onScan = {
                            selectedTestCase = testCase
                            showInstructions = true
                        }
                    )
                }
            }
        }
    }
    
    // Instructions Dialog
    if (showInstructions && selectedTestCase != null) {
        AlertDialog(
            onDismissRequest = { showInstructions = false },
            title = {
                Text(
                    text = "How to Test This Case",
                    fontSize = if (isSmallScreen) 16.sp else 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "To test scanner accuracy:",
                        fontSize = bodySize,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(if (isSmallScreen) 8.dp else 12.dp))
                    Text("1. SAVE or SHARE the PDF", fontSize = bodySize)
                    Text("2. Print or display on screen", fontSize = bodySize)
                    Spacer(modifier = Modifier.height(if (isSmallScreen) 6.dp else 8.dp))
                    Text("3. Go Home → Create new exam", fontSize = bodySize)
                    Text("4. Set answer key to match:", fontSize = bodySize)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = selectedTestCase!!.answers.joinToString(", "),
                        fontFamily = FontFamily.Monospace,
                        fontSize = smallTextSize,
                        color = Color(0xFF007AFF)
                    )
                    Spacer(modifier = Modifier.height(if (isSmallScreen) 6.dp else 8.dp))
                    Text("5. Use SCAN SHEETS to scan PDF", fontSize = bodySize)
                    Text("6. Compare detected vs expected", fontSize = bodySize)
                }
            },
            confirmButton = {
                TextButton(onClick = { showInstructions = false }) {
                    Text("GOT IT", fontSize = bodySize, color = Color(0xFF007AFF))
                }
            }
        )
    }
}

@Composable
fun TestCaseCard(
    testCase: ScannerTestUtility.TestAnswerSheet,
    isGenerating: Boolean,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onScan: () -> Unit
) {
    FloatingGlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Assignment,
                    contentDescription = null,
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(32.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = testCase.examName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1C1C1E)
                    )
                    Text(
                        text = "${testCase.questionCount} questions",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8E8E93)
                    )
                    
                    val preview = testCase.answers.take(10).joinToString(" ") + "..."
                    Text(
                        text = "Pattern: $preview",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8E8E93),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onShare,
                    enabled = !isGenerating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007AFF)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SHARE")
                }
                
                Button(
                    onClick = onSave,
                    enabled = !isGenerating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF34C759)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SAVE")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onScan,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9500)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("SCAN TEST SHEET")
            }
        }
    }
}
