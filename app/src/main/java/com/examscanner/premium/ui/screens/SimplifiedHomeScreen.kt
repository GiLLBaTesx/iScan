package com.examscanner.premium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.examscanner.premium.ui.components.FloatingGlassCard

/**
 * MVP HOME SCREEN - Simplified 4-Step Flow
 * 
 * Your Teacher's Journey:
 * 1. Upload/Scan → 2. Set Answers + Tag Competencies → 3. Smart Dashboard → 4. Export
 * 
 * This is your app's entry point for the MVP
 */
@Composable
fun SimplifiedHomeScreen(
    onStartNewTest: () -> Unit,
    onViewSettings: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF0F4F8),
                        Color(0xFFFFFFFF)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // App Logo/Title
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Grade Smart",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E)
            )
            
            Text(
                text = "Know what to reteach",
                fontSize = 16.sp,
                color = Color(0xFF8E8E93)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Main CTA - Start New Test
            FloatingGlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onStartNewTest,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007AFF)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Start New Test",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Feature Cards - Show What Makes You Different
            Text(
                text = "Why teachers choose us:",
                fontSize = 14.sp,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureCard(
                    icon = Icons.Default.Psychology,
                    title = "Smart Reteach Suggestions",
                    description = "Automatically identifies weak competencies"
                )
                
                FeatureCard(
                    icon = Icons.Default.BarChart,
                    title = "Deep Item Analysis",
                    description = "Difficulty & Discrimination Index per question"
                )
                
                FeatureCard(
                    icon = Icons.Default.Groups,
                    title = "Intervention Groups",
                    description = "Groups students by skill gaps for targeted help"
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Footer
            Text(
                text = "Built for Filipino teachers 🇵🇭",
                fontSize = 12.sp,
                color = Color(0xFFAEAEB2),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
fun FeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    FloatingGlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = Color(0xFF007AFF).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1C1C1E)
                )
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93),
                    lineHeight = 18.sp
                )
            }
        }
    }
}
