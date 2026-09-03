package com.examscanner.premium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.examscanner.premium.data.AnswerKeyEntity
import com.examscanner.premium.data.StudentAnswerEntity
import com.examscanner.premium.ui.components.FloatingGlassCard
import com.examscanner.premium.ui.theme.*

data class QuestionAnalysis(
    val questionNumber: Int,
    val correctAnswer: String,
    val answerDistribution: Map<String, Int>, // Answer -> Count
    val correctCount: Int,
    val totalResponses: Int,
    val percentCorrect: Int
)

@Composable
fun ItemAnalysisContent(
    answerKeys: List<AnswerKeyEntity>,
    allStudentAnswers: List<StudentAnswerEntity>
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isSmallScreen = screenWidth < 360.dp
    
    // Responsive sizing
    val infoTextSize = if (isSmallScreen) 12.sp else 13.sp
    val horizontalPadding = if (isSmallScreen) 12.dp else 16.dp
    
    val analysisData = remember(key1 = answerKeys, key2 = allStudentAnswers) {
        calculateQuestionAnalysis(answerKeys, allStudentAnswers)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding)
    ) {
        // Info Card - Compact header
        FloatingGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            Text(
                text = "Bars show how the class split across choices. Red = the key.",
                fontSize = infoTextSize,
                color = TextSecondary,
                modifier = Modifier.padding(
                    horizontal = if (isSmallScreen) 12.dp else 16.dp,
                    vertical = if (isSmallScreen) 10.dp else 12.dp
                )
            )
        }
        
        // Question Analysis List - Takes up remaining space
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // This ensures it takes all available space
            contentPadding = PaddingValues(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(if (isSmallScreen) 10.dp else 12.dp)
        ) {
            itemsIndexed(analysisData) { index, analysis ->
                QuestionAnalysisCard(
                    analysis = analysis,
                    isSmallScreen = isSmallScreen
                )
            }
        }
    }
}

@Composable
fun QuestionAnalysisCard(
    analysis: QuestionAnalysis,
    isSmallScreen: Boolean
) {
    // Responsive sizing
    val questionTextSize = if (isSmallScreen) 15.sp else 16.sp
    val percentTextSize = if (isSmallScreen) 13.sp else 14.sp
    val optionTextSize = if (isSmallScreen) 14.sp else 15.sp
    val countTextSize = if (isSmallScreen) 12.sp else 13.sp
    val cardPadding = if (isSmallScreen) 14.dp else 20.dp
    val spacingBetweenBars = if (isSmallScreen) 6.dp else 8.dp
    
    FloatingGlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(cardPadding)
        ) {
            // Question Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Q${analysis.questionNumber} · key ${analysis.correctAnswer}",
                    fontSize = questionTextSize,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "${analysis.percentCorrect}% correct",
                    fontSize = percentTextSize,
                    fontWeight = FontWeight.SemiBold,
                    color = if (analysis.percentCorrect >= 50) SuccessGreen else ErrorRed
                )
            }
            
            Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 16.dp))
            
            // Answer Distribution Bars
            val maxCount = analysis.answerDistribution.values.maxOrNull() ?: 1
            val options = listOf("A", "B", "C", "D", "E")
            
            options.forEach { option ->
                val count = analysis.answerDistribution[option] ?: 0
                val isCorrect = option == analysis.correctAnswer
                
                AnswerBar(
                    option = option,
                    count = count,
                    maxCount = maxCount,
                    isCorrect = isCorrect,
                    isSmallScreen = isSmallScreen,
                    optionTextSize = optionTextSize,
                    countTextSize = countTextSize
                )
                Spacer(modifier = Modifier.height(spacingBetweenBars))
            }
        }
    }
}

@Composable
fun AnswerBar(
    option: String,
    count: Int,
    maxCount: Int,
    isCorrect: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Option Label
        Text(
            text = option,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.width(32.dp)
        )
        
        // Bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(32.dp)
        ) {
            // Background bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(LightGray.copy(alpha = 0.3f))
            )
            
            // Filled bar
            if (count > 0) {
                val fillFraction = count.toFloat() / maxCount.toFloat()
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fillFraction)
                        .height(32.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isCorrect) ErrorRed else ChartBlue
                        )
                )
            }
        }
        
        // Count
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            modifier = Modifier
                .width(40.dp)
                .padding(start = 8.dp)
        )
    }
}

private fun calculateQuestionAnalysis(
    answerKeys: List<AnswerKeyEntity>,
    allStudentAnswers: List<StudentAnswerEntity>
): List<QuestionAnalysis> {
    return answerKeys.map { key ->
        // Get all answers for this question
        val answersForQuestion = allStudentAnswers.filter { 
            it.questionNumber == key.questionNumber 
        }
        
        // Count distribution
        val distribution = mutableMapOf<String, Int>()
        listOf("A", "B", "C", "D", "E").forEach { option ->
            distribution[option] = answersForQuestion.count { it.answer == option }
        }
        
        val correctCount = answersForQuestion.count { it.answer == key.correctAnswer }
        val totalResponses = answersForQuestion.size
        val percentCorrect = if (totalResponses > 0) {
            (correctCount * 100) / totalResponses
        } else 0
        
        QuestionAnalysis(
            questionNumber = key.questionNumber,
            correctAnswer = key.correctAnswer,
            answerDistribution = distribution,
            correctCount = correctCount,
            totalResponses = totalResponses,
            percentCorrect = percentCorrect
        )
    }.sortedBy { it.questionNumber }
}
