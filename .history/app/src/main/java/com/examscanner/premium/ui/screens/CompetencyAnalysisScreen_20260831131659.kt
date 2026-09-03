package com.examscanner.premium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.examscanner.premium.data.AnswerKeyEntity
import com.examscanner.premium.data.MelcEntity
import com.examscanner.premium.data.StudentAnswerEntity
import com.examscanner.premium.ui.components.FloatingGlassCard
import com.examscanner.premium.ui.theme.*

data class CompetencyPerformance(
    val melc: MelcEntity,
    val questionNumbers: List<Int>,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val totalAttempts: Int,
    val masteryPercentage: Int,
    val studentPerformance: Map<Long, Int> // studentId -> correct count
)

@Composable
fun CompetencyAnalysisScreen(
    examId: Long,
    sectionName: String? = null,
    questionMelcMappings: Map<Int, MelcEntity>,
    answerKeys: List<AnswerKeyEntity>,
    studentAnswers: List<StudentAnswerEntity>,
    onBack: () -> Unit
) {
    val competencyData = remember(questionMelcMappings, answerKeys, studentAnswers) {
        calculateCompetencyPerformance(questionMelcMappings, answerKeys, studentAnswers)
    }
    
    var selectedTab by remember { mutableStateOf("BY_COMPETENCY") }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            FloatingGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = PrimaryBlue
                            )
                        }
                    }
                    
                    Text(
                        text = "Competency Analysis",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (sectionName != null) {
                        Text(
                            text = sectionName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = "${competencyData.size} competencies · ${questionMelcMappings.size} tagged questions",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }

            // Summary Card
            if (competencyData.isNotEmpty()) {
                val averageMastery = competencyData.map { it.masteryPercentage }.average().toInt()
                val masteryLevel = when {
                    averageMastery >= 90 -> "Advanced"
                    averageMastery >= 75 -> "Proficient"
                    averageMastery >= 60 -> "Approaching Proficient"
                    averageMastery >= 50 -> "Developing"
                    else -> "Beginning"
                }
                val masteryColor = when {
                    averageMastery >= 75 -> SuccessGreen
                    averageMastery >= 60 -> WarningOrange
                    else -> ErrorRed
                }
                
                FloatingGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Overall Mastery",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "$averageMastery%",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = masteryColor
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = masteryLevel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = masteryColor
                            )
                            Text(
                                text = "Performance Level",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Tab Bar
            FloatingGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    TabButton(
                        text = "BY COMPETENCY",
                        isSelected = selectedTab == "BY_COMPETENCY",
                        modifier = Modifier.weight(1f)
                    ) { selectedTab = "BY_COMPETENCY" }
                    TabButton(
                        text = "BY QUESTION",
                        isSelected = selectedTab == "BY_QUESTION",
                        modifier = Modifier.weight(1f)
                    ) { selectedTab = "BY_QUESTION" }
                }
            }

            // Content
            when (selectedTab) {
                "BY_COMPETENCY" -> {
                    if (competencyData.isEmpty()) {
                        // Empty State
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No Competencies Tagged",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Tag questions with MELCs in Edit Key to see competency analysis",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(competencyData.sortedByDescending { it.masteryPercentage }) { competency ->
                                CompetencyCard(competency = competency)
                            }
                        }
                    }
                }
                "BY_QUESTION" -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(questionMelcMappings.entries.sortedBy { it.key }) { (questionNum, melc) ->
                            val key = answerKeys.find { it.questionNumber == questionNum }
                            val answers = studentAnswers.filter { it.questionNumber == questionNum }
                            val correctCount = answers.count { it.answer == key?.correctAnswer }
                            val totalCount = answers.size
                            val percentage = if (totalCount > 0) (correctCount * 100) / totalCount else 0
                            
                            QuestionCompetencyCard(
                                questionNumber = questionNum,
                                melc = melc,
                                correctCount = correctCount,
                                totalCount = totalCount,
                                percentage = percentage
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompetencyCard(competency: CompetencyPerformance) {
    FloatingGlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // MELC Code Badge
            Box(
                modifier = Modifier
                    .background(
                        color = PrimaryBlue.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = competency.melc.code,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Description
            Text(
                text = competency.melc.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Subject and Quarter
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Badge(
                    text = competency.melc.subject,
                    color = PrimaryBlue.copy(alpha = 0.1f),
                    textColor = PrimaryBlue
                )
                Badge(
                    text = competency.melc.gradeLevel,
                    color = SuccessGreen.copy(alpha = 0.1f),
                    textColor = SuccessGreen
                )
                Badge(
                    text = "Q${competency.melc.quarter}",
                    color = WarningOrange.copy(alpha = 0.1f),
                    textColor = WarningOrange
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Questions covered
            Text(
                text = "Questions: ${competency.questionNumbers.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Mastery Progress Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mastery Level",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "${competency.masteryPercentage}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            competency.masteryPercentage >= 75 -> SuccessGreen
                            competency.masteryPercentage >= 60 -> WarningOrange
                            else -> ErrorRed
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(LightGray.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(competency.masteryPercentage / 100f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    competency.masteryPercentage >= 75 -> SuccessGreen
                                    competency.masteryPercentage >= 60 -> WarningOrange
                                    else -> ErrorRed
                                }
                            )
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${competency.correctAnswers}/${competency.totalAttempts} correct responses",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }
    }
}

@Composable
fun QuestionCompetencyCard(
    questionNumber: Int,
    melc: MelcEntity,
    correctCount: Int,
    totalCount: Int,
    percentage: Int
) {
    FloatingGlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Q$questionNumber",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Box(
                        modifier = Modifier
                            .background(
                                color = PrimaryBlue.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = melc.code,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = melc.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        percentage >= 75 -> SuccessGreen
                        percentage >= 50 -> WarningOrange
                        else -> ErrorRed
                    }
                )
                Text(
                    text = "$correctCount/$totalCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun Badge(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color
) {
    Box(
        modifier = Modifier
            .background(
                color = color,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CompetencyTabButton(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(40.dp)
            .padding(2.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) PrimaryBlue else androidx.compose.ui.graphics.Color.Transparent,
            contentColor = if (isSelected) SurfaceWhite else TextSecondary
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = null,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

private fun calculateCompetencyPerformance(
    questionMelcMappings: Map<Int, MelcEntity>,
    answerKeys: List<AnswerKeyEntity>,
    studentAnswers: List<StudentAnswerEntity>
): List<CompetencyPerformance> {
    // Group questions by MELC
    val melcToQuestions = questionMelcMappings.entries.groupBy({ it.value }, { it.key })
    
    return melcToQuestions.map { (melc, questionNumbers) ->
        val questionNumList = questionNumbers.sorted()
        
        // Calculate performance for this competency
        var totalCorrect = 0
        var totalAttempts = 0
        
        questionNumList.forEach { questionNum ->
            val key = answerKeys.find { it.questionNumber == questionNum }
            val answers = studentAnswers.filter { it.questionNumber == questionNum }
            
            totalAttempts += answers.size
            totalCorrect += answers.count { it.answer == key?.correctAnswer }
        }
        
        val masteryPercentage = if (totalAttempts > 0) {
            (totalCorrect * 100) / totalAttempts
        } else 0
        
        CompetencyPerformance(
            melc = melc,
            questionNumbers = questionNumList,
            totalQuestions = questionNumList.size,
            correctAnswers = totalCorrect,
            totalAttempts = totalAttempts,
            masteryPercentage = masteryPercentage,
            studentPerformance = emptyMap() // Can be expanded for per-student tracking
        )
    }
}
