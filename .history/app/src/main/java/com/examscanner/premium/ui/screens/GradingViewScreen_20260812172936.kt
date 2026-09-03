package com.examscanner.premium.ui.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.examscanner.premium.data.AnswerKeyEntity
import com.examscanner.premium.data.StudentAnswerEntity
import com.examscanner.premium.ui.components.FloatingGlassCard
import com.examscanner.premium.ui.theme.*

data class GradedAnswer(
    val questionNumber: Int,
    val studentAnswer: String,
    val correctAnswer: String,
    val isCorrect: Boolean,
    val isAlternative: Boolean = false
)

@Composable
fun GradingViewScreen(
    studentName: String,
    studentId: String,
    percentage: Int,
    imageUri: Uri?,
    answerKeys: List<AnswerKeyEntity>,
    studentAnswers: List<StudentAnswerEntity>,
    onBack: () -> Unit
) {
    val gradedAnswers = remember(answerKeys, studentAnswers) {
        gradeAnswers(answerKeys, studentAnswers)
    }
    
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
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = PrimaryBlue
                            )
                        }
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = studentName,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "ID: $studentId",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                    
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (percentage >= 50) SuccessGreen else ErrorRed
                    )
                }
            }
            
            // Scanned Image Preview (if available)
            if (imageUri != null) {
                FloatingGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = "Scanned Sheet",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            // Legend
            FloatingGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem(
                        color = SuccessGreen,
                        icon = Icons.Default.Check,
                        label = "CORRECT"
                    )
                    LegendItem(
                        color = AccentBlue,
                        icon = Icons.Default.Check,
                        label = "ALTERNATIVE"
                    )
                    LegendItem(
                        color = ErrorRed,
                        icon = Icons.Default.Close,
                        label = "WRONG"
                    )
                }
            }
            
            // Answers List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(gradedAnswers) { index, answer ->
                    GradedAnswerItem(answer = answer)
                }
            }
        }
    }
}

@Composable
fun LegendItem(
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SurfaceWhite,
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
fun GradedAnswerItem(answer: GradedAnswer) {
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
            // Question Number
            Text(
                text = "Q${answer.questionNumber}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.width(50.dp)
            )
            
            // Student Answer
            AnswerBubble(
                answer = answer.studentAnswer,
                color = when {
                    answer.isCorrect -> SuccessGreen
                    answer.isAlternative -> AccentBlue
                    else -> ErrorRed
                },
                isCorrect = answer.isCorrect
            )
            
            // Arrow
            Text(
                text = "→",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
            
            // Correct Answer
            AnswerBubble(
                answer = answer.correctAnswer,
                color = LightGray,
                isCorrect = true,
                isKey = true
            )
            
            // Status Icon
            Icon(
                imageVector = if (answer.isCorrect || answer.isAlternative) 
                    Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = when {
                    answer.isCorrect -> SuccessGreen
                    answer.isAlternative -> AccentBlue
                    else -> ErrorRed
                },
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun AnswerBubble(
    answer: String,
    color: Color,
    isCorrect: Boolean,
    isKey: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isKey) color.copy(alpha = 0.2f) else color)
            .border(
                width = if (isKey) 2.dp else 0.dp,
                color = if (isKey) TextSecondary else Color.Transparent,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = answer,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (isKey) TextPrimary else SurfaceWhite
        )
    }
}

private fun gradeAnswers(
    answerKeys: List<AnswerKeyEntity>,
    studentAnswers: List<StudentAnswerEntity>
): List<GradedAnswer> {
    return answerKeys.map { key ->
        val studentAnswer = studentAnswers.find { 
            it.questionNumber == key.questionNumber 
        }?.answer ?: "—"
        
        val isCorrect = studentAnswer == key.correctAnswer
        val isAlternative = key.alternativeAnswers.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .contains(studentAnswer)
        
        GradedAnswer(
            questionNumber = key.questionNumber,
            studentAnswer = studentAnswer,
            correctAnswer = key.correctAnswer,
            isCorrect = isCorrect,
            isAlternative = isAlternative
        )
    }.sortedBy { it.questionNumber }
}
