package com.examscanner.premium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.examscanner.premium.ui.theme.*

@Composable
fun SecurityWarningScreen(
    warningMessage: String,
    details: String,
    onProceedAnyway: () -> Unit,
    onExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Warning Icon
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Security Warning",
                modifier = Modifier.size(80.dp),
                tint = ErrorCoral
            )
            
            // Title
            Text(
                text = "Security Warning",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricBlue,
                textAlign = TextAlign.Center
            )
            
            // Warning Message
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = ErrorCoral.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = warningMessage,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text(
                        text = details,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // Risks
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = IceWhite
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Potential Risks:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricBlue
                    )
                    
                    val risks = listOf(
                        "Student data may be accessible to unauthorized apps",
                        "Exam scores could be modified by third parties",
                        "App security features may be bypassed",
                        "Data encryption effectiveness is reduced"
                    )
                    
                    risks.forEach { risk ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("•", color = ErrorCoral)
                            Text(
                                text = risk,
                                fontSize = 13.sp,
                                color = TextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBlue
                    )
                ) {
                    Text(
                        text = "Exit App (Recommended)",
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                
                OutlinedButton(
                    onClick = onProceedAnyway,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ErrorCoral
                    )
                ) {
                    Text(
                        text = "Proceed Anyway (Not Recommended)",
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}
