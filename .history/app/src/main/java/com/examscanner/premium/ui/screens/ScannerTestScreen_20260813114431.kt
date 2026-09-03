package com.examscanner.premium.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.core.content.FileProvider
import com.examscanner.premium.testing.ScannerTestUtility
import com.examscanner.premium.ui.components.FloatingGlassCard
import com.examscanner.premium.ui.components.GlassCard
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
    var isGenerating by remember { mutableStateOf(false) }
    var generatedFile by remember { mutableStateOf<java.io.File?>(null) }
    var selectedTestCase by remember { mutableStateOf<ScannerTestUtility.TestAnswerSheet?>(null) }
    var showTestReport by remember { mutableStateOf(false) }
    var testReport by remember { mutableStateOf("") }
    
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
                .padding(20.dp)
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
                        tint = Color(0xFF007AFF)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Scanner Accuracy Test",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E)
                    )
                    Text(
                        text = "Test bubble detection accuracy",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8E8E93)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Instructions Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "How to Test",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1C1C1E)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "1. Select a test case below\n" +
                              "2. Generate test answer sheet PDF\n" +
                              "3. Print or display on another device\n" +
                              "4. Scan with camera\n" +
                              "5. View accuracy results",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF3C3C43),
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.5f
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Test Cases List
            Text(
                text = "Test Cases",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1C1E)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(testCases) { testCase ->
                    TestCaseCard(
                        testCase = testCase,
                        isSelected = selectedTestCase == testCase,
                        isGenerating = isGenerating,
                        onSelect = { selectedTestCase = testCase },
                        onGenerate = {
                            scope.launch {
                                isGenerating = true
                                try {
                                    val file = withContext(Dispatchers.IO) {
                                        ScannerTestUtility.generateTestAnswerSheet(context, testCase)
                                    }
                                    generatedFile = file
                                    isGenerating = false
                                    
                                    // Share PDF instead of trying to open it directly
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        file
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_SUBJECT, testCase.examName)
                                        putExtra(Intent.EXTRA_TEXT, "Test Answer Sheet - ${testCase.examName}")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Save/Share Test Sheet"))
                                } catch (e: Exception) {
                                    isGenerating = false
                                    android.widget.Toast.makeText(
                                        context, 
                                        "PDF generation failed: ${e.message}", 
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },
                        onScan = {
                            onScanTestSheet(testCase.answers)
                        }
                    )
                }
            }
        }
    }
    
    // Test Report Dialog
    if (showTestReport) {
        AlertDialog(
            onDismissRequest = { showTestReport = false },
            title = {
                Text(
                    text = "Test Results",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = testReport,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTestReport = false }) {
                    Text("CLOSE", color = Color(0xFF007AFF))
                }
            }
        )
    }
}

@Composable
fun TestCaseCard(
    testCase: ScannerTestUtility.TestAnswerSheet,
    isSelected: Boolean,
    isGenerating: Boolean,
    onSelect: () -> Unit,
    onGenerate: () -> Unit,
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
                    tint = if (isSelected) Color(0xFF007AFF) else Color(0xFF8E8E93),
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
                    
                    // Show pattern preview
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
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Generate Button
                Button(
                    onClick = onGenerate,
                    enabled = !isGenerating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007AFF),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isGenerating) "GENERATING..." else "GENERATE PDF")
                }
                
                // Scan Button
                Button(
                    onClick = onScan,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF34C759),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SCAN NOW")
                }
            }
        }
    }
}
