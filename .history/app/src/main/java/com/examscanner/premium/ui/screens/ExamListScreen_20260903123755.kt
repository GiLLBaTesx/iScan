package com.examscanner.premium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.examscanner.premium.data.ExamWithStats
import com.examscanner.premium.ui.components.FloatingGlassCard
import com.examscanner.premium.ui.components.GlassCard
import com.examscanner.premium.ui.theme.*

@Composable
fun ExamListScreen(
    exams: List<ExamWithStats>,
    onExamClick: (ExamWithStats) -> Unit,
    onNewExamClick: () -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    val totalGraded = exams.sumOf { it.scannedCount }
    val totalExams = exams.size
    val averageAccuracy = if (totalGraded > 0) {
        exams.filter { it.averageScore != null }
            .mapNotNull { it.averageScore }
            .average()
            .toInt()
    } else 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Enhanced Header with Stats
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = DarkCard,
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "JAYSON SUYAT WORKSPACE",
                                style = MaterialTheme.typography.labelSmall,
                                color = DarkTextTertiary,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Exams",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = DarkTextPrimary
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Live Sync Badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = LiveSyncGreen.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(LiveSyncGreen, CircleShape)
                                    )
                                    Text(
                                        text = "LIVE SYNC",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = LiveSyncGreen,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = CoralPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    // Stats Row (ScanKey style)
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Graded Count
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = totalGraded.toString(),
                                    style = MaterialTheme.typography.displayLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkTextPrimary,
                                    fontSize = 48.sp
                                )
                                if (totalGraded > 0) {
                                    Text(
                                        text = "+${minOf(totalGraded, 12)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = LiveSyncGreen,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                            }
                            Text(
                                text = "GRADED",
                                style = MaterialTheme.typography.labelMedium,
                                color = DarkTextSecondary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "today",
                                style = MaterialTheme.typography.labelSmall,
                                color = DarkTextTertiary,
                                fontSize = 10.sp
                            )
                        }
                        
                        Divider(
                            modifier = Modifier
                                .height(70.dp)
                                .width(1.dp),
                            color = DarkDivider
                        )
                        
                        // Active Count
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = totalExams.toString(),
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold,
                                color = DarkTextPrimary,
                                fontSize = 48.sp
                            )
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelMedium,
                                color = DarkTextSecondary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "exams",
                                style = MaterialTheme.typography.labelSmall,
                                color = DarkTextTertiary,
                                fontSize = 10.sp
                            )
                        }
                        
                        Divider(
                            modifier = Modifier
                                .height(70.dp)
                                .width(1.dp),
                            color = DarkDivider
                        )
                        
                        // Accuracy
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$averageAccuracy%",
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    averageAccuracy >= 75 -> LiveSyncGreen
                                    averageAccuracy >= 50 -> DarkWarningOrange
                                    else -> DarkErrorRed
                                },
                                fontSize = 48.sp
                            )
                            Text(
                                text = "ACCURACY",
                                style = MaterialTheme.typography.labelMedium,
                                color = DarkTextSecondary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = when {
                                    averageAccuracy >= 75 -> "excellent"
                                    averageAccuracy >= 50 -> "good"
                                    else -> "needs work"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = DarkTextTertiary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Category Tabs (All, Midterms, Quizzes, Archived)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryTab("All Exams", true, totalExams) { }
                CategoryTab("Midterms", false, 0) { }
                CategoryTab("Quizzes", false, 0) { }
                CategoryTab("Archived", false, 0) { }
            }

            // Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Rosters & Keys",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                TextButton(onClick = { /* Sort */ }) {
                    Text(
                        text = "SORT BY DATE ▼",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Exam List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(exams) { examWithStats ->
                    ExamListItem(examWithStats = examWithStats, onClick = { onExamClick(examWithStats) })
                }
            }
        }

        // Floating Action Button (ScanKey style)
        FloatingActionButton(
            onClick = onNewExamClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp, end = 20.dp),
            containerColor = CoralPrimary,
            contentColor = androidx.compose.ui.graphics.Color.White,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create New Exam",
                modifier = Modifier.size(28.dp)
            )
        }

        // Bottom Navigation
        FloatingGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            elevation = 12.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NavigationItem(label = "EXAMS", isSelected = true) { }
                NavigationItem(label = "STUDENTS", isSelected = false) { }
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
fun ExamListItem(examWithStats: ExamWithStats, onClick: () -> Unit) {
    FloatingGlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(20.dp)
        ) {
            Text(
                text = examWithStats.exam.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${examWithStats.exam.totalQuestions} Questions · ${examWithStats.scannedCount}/${examWithStats.keyedQuestions} keyed",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (examWithStats.scannedCount == 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(LightGray)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NO SCANS",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                } else {
                    examWithStats.averageScore?.let { avg ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$avg%",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (avg >= 50) SuccessGreen else ErrorRed
                            )
                            Text(
                                text = "AVERAGE",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationItem(label: String, isSelected: Boolean, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) PrimaryBlue else TextSecondary,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(PrimaryBlue)
            )
        }
    }
}


@Composable
fun CategoryTab(
    text: String,
    isSelected: Boolean,
    count: Int,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) CoralPrimary else SurfaceWhite,
        border = if (!isSelected) BorderStroke(1.dp, LightGray) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) androidx.compose.ui.graphics.Color.White else TextPrimary
            )
            if (isSelected && count > 0) {
                Surface(
                    shape = CircleShape,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
