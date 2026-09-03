package com.examscanner.premium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.examscanner.premium.data.*
import com.examscanner.premium.ui.components.FloatingGlassCard
import com.examscanner.premium.ui.theme.*
import kotlin.math.roundToInt

/**
 * SMART DASHBOARD MVP - Your Competitive Edge
 * 
 * Tab 1: "What to Reteach Now" - AI-suggested intervention priorities
 * Tab 2: "Item Analysis" - Difficulty Index + Discrimination Index
 * Tab 3: "Intervention Groups" - Students grouped by competency gaps
 */
@Composable
fun SmartDashboardMVP(
    examId: Long,
    examName: String,
    answerKeys: List<AnswerKeyEntity>,
    studentAnswers: List<StudentAnswerEntity>,
    students: List<StudentEntity>,
    questionMelcMappings: Map<Int, MelcEntity>,
    onExportPDF: () -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(MVPTab.RETEACH) }
    
    // Calculate all analytics
    val reteachData = remember(answerKeys, studentAnswers, questionMelcMappings) {
        calculateReteachPriorities(answerKeys, studentAnswers, questionMelcMappings)
    }
    
    val itemAnalysis = remember(answerKeys, studentAnswers, students) {
        calculateItemAnalysisWithIndices(answerKeys, studentAnswers, students)
    }
    
    val interventionGroups = remember(students, studentAnswers, answerKeys, questionMelcMappings) {
        calculateInterventionGroups(students, studentAnswers, answerKeys, questionMelcMappings)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        // Header
        FloatingGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .statusBarsPadding()
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = PrimaryBlue)
                        }
                        Column {
                            Text(
                                text = "Smart Report",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = examName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                    IconButton(onClick = onExportPDF) {
                        Icon(Icons.Default.PictureAsPdf, "Export PDF", tint = ErrorRed)
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
                MVPTabButton(
                    text = "RETEACH",
                    icon = Icons.Default.Psychology,
                    isSelected = selectedTab == MVPTab.RETEACH,
                    modifier = Modifier.weight(1f)
                ) { selectedTab = MVPTab.RETEACH }
                
                MVPTabButton(
                    text = "ANALYSIS",
                    icon = Icons.Default.BarChart,
                    isSelected = selectedTab == MVPTab.ANALYSIS,
                    modifier = Modifier.weight(1f)
                ) { selectedTab = MVPTab.ANALYSIS }
                
                MVPTabButton(
                    text = "GROUPS",
                    icon = Icons.Default.Groups,
                    isSelected = selectedTab == MVPTab.INTERVENTION,
                    modifier = Modifier.weight(1f)
                ) { selectedTab = MVPTab.INTERVENTION }
            }
        }

        // Tab Content
        when (selectedTab) {
            MVPTab.RETEACH -> ReteachNowTab(reteachData)
            MVPTab.ANALYSIS -> ItemAnalysisTab(itemAnalysis)
            MVPTab.INTERVENTION -> InterventionGroupsTab(interventionGroups)
        }
    }
}

enum class MVPTab {
    RETEACH,      // What to reteach now
    ANALYSIS,     // Difficulty + Discrimination indices
    INTERVENTION  // Student grouping
}

// ============================================================================
// TAB 1: WHAT TO RETEACH NOW (Your Killer Feature!)
// ============================================================================

data class ReteachPriority(
    val competency: MelcEntity,
    val masteryPercentage: Int,
    val affectedQuestions: List<Int>,
    val affectedStudentCount: Int,
    val priority: Priority
)

enum class Priority(val label: String, val color: Color, val icon: String) {
    URGENT("Urgent - Reteach This Week", Color(0xFFFF3B30), "🔴"),
    SOON("Important - Next 2 Weeks", Color(0xFFFF9500), "🟡"),
    MONITOR("Monitor Progress", Color(0xFF34C759), "🟢")
}

@Composable
fun ReteachNowTab(priorities: List<ReteachPriority>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Intro Card
            FloatingGlassCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFFFF9500),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "AI-Suggested Action Plan",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            "Top ${priorities.size} competencies needing intervention",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
        
        items(priorities) { priority ->
            ReteachPriorityCard(priority)
        }
    }
}

@Composable
fun ReteachPriorityCard(priority: ReteachPriority) {
    FloatingGlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Priority Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = priority.priority.icon,
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = priority.priority.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = priority.priority.color
                        )
                        Text(
                            text = "${priority.masteryPercentage}% class mastery",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
                
                // Mastery Circle
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            priority.priority.color.copy(alpha = 0.1f),
                            RoundedCornerShape(28.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${priority.masteryPercentage}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = priority.priority.color
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Competency Info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        PrimaryBlue.copy(alpha = 0.05f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = priority.competency.code,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Text(
                        text = priority.competency.description,
                        fontSize = 13.sp,
                        color = TextPrimary,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(text = priority.competency.subject, color = SuccessGreen)
                        Chip(text = priority.competency.gradeLevel, color = PrimaryBlue)
                        Chip(text = "Q${priority.competency.quarter}", color = WarningOrange)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Actionable Items
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ActionItem(
                    icon = Icons.Default.QuestionAnswer,
                    label = "Questions",
                    value = priority.affectedQuestions.joinToString(", ")
                )
                ActionItem(
                    icon = Icons.Default.People,
                    label = "Students",
                    value = "${priority.affectedStudentCount}"
                )
            }
        }
    }
}

// ============================================================================
// TAB 2: ITEM ANALYSIS (Difficulty + Discrimination Indices)
// ============================================================================

data class ItemAnalysisData(
    val questionNumber: Int,
    val correctAnswer: String,
    val totalStudents: Int,
    val correctCount: Int,
    val difficultyIndex: Double,  // % correct (0.0 to 1.0)
    val discriminationIndex: Double,  // Top27% - Bottom27% (-1.0 to 1.0)
    val difficulty: DifficultyLevel,
    val discrimination: DiscriminationLevel,
    val answerDistribution: Map<String, Int>
)

enum class DifficultyLevel(val label: String, val color: Color, val icon: String) {
    EASY("Easy", Color(0xFF34C759), "🟢"),
    MODERATE("Moderate", Color(0xFFFF9500), "🟡"),
    DIFFICULT("Difficult", Color(0xFFFF3B30), "🔴")
}

enum class DiscriminationLevel(val label: String, val emoji: String) {
    EXCELLENT("Excellent", "✅"),
    GOOD("Good", "👍"),
    FAIR("Fair", "⚠️"),
    POOR("Poor", "❌")
}

@Composable
fun ItemAnalysisTab(items: List<ItemAnalysisData>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Legend Card
            FloatingGlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Understanding the Indices",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "• Difficulty: % of students who got it right\n" +
                        "• Discrimination: How well it separates high/low performers\n" +
                        "• Good items: Moderate difficulty + High discrimination",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
        
        itemsIndexed(items) { index, item ->
            ItemAnalysisCard(item)
        }
    }
}

@Composable
fun ItemAnalysisCard(item: ItemAnalysisData) {
    FloatingGlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.difficulty.icon,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Q${item.questionNumber}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Key: ${item.correctAnswer}",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
                
                Text(
                    text = "${item.correctCount}/${item.totalStudents}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = item.difficulty.color
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Indices
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IndexCard(
                    label = "Difficulty",
                    value = String.format("%.2f", item.difficultyIndex),
                    subtitle = item.difficulty.label,
                    color = item.difficulty.color,
                    modifier = Modifier.weight(1f)
                )
                
                IndexCard(
                    label = "Discrimination",
                    value = String.format("%.2f", item.discriminationIndex),
                    subtitle = item.discrimination.label,
                    color = when (item.discrimination) {
                        DiscriminationLevel.EXCELLENT, DiscriminationLevel.GOOD -> SuccessGreen
                        DiscriminationLevel.FAIR -> WarningOrange
                        DiscriminationLevel.POOR -> ErrorRed
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Answer Distribution
            Text(
                "Answer Distribution:",
                fontSize = 12.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            listOf("A", "B", "C", "D", "E").forEach { option ->
                val count = item.answerDistribution[option] ?: 0
                val percentage = if (item.totalStudents > 0) {
                    (count * 100.0 / item.totalStudents).roundToInt()
                } else 0
                
                AnswerDistributionBar(
                    option = option,
                    count = count,
                    percentage = percentage,
                    isCorrect = option == item.correctAnswer
                )
            }
        }
    }
}

@Composable
fun IndexCard(
    label: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextSecondary
            )
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = color
            )
        }
    }
}

@Composable
fun AnswerDistributionBar(
    option: String,
    count: Int,
    percentage: Int,
    isCorrect: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = option,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isCorrect) SuccessGreen else TextPrimary,
            modifier = Modifier.width(24.dp)
        )
        
        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(LightGray.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percentage / 100f)
                    .background(
                        if (isCorrect) ErrorRed else ChartBlue
                    )
            )
        }
        
        Text(
            text = "$count ($percentage%)",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.width(80.dp).padding(start = 8.dp)
        )
    }
}

// ============================================================================
// TAB 3: INTERVENTION GROUPS (Student Grouping by Weak Competency)
// ============================================================================

data class InterventionGroup(
    val competency: MelcEntity,
    val masteryPercentage: Int,
    val students: List<StudentInterventionData>
)

data class StudentInterventionData(
    val student: StudentEntity,
    val correctCount: Int,
    val totalQuestions: Int,
    val percentage: Int
)

@Composable
fun InterventionGroupsTab(groups: List<InterventionGroup>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FloatingGlassCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Groups,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Intervention Groups",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            "Students grouped by competency gaps",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
        
        items(groups) { group ->
            InterventionGroupCard(group)
        }
    }
}

@Composable
fun InterventionGroupCard(group: InterventionGroup) {
    FloatingGlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Competency Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.competency.code,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Text(
                        text = group.competency.description,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        lineHeight = 18.sp
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            ErrorRed.copy(alpha = 0.1f),
                            RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${group.masteryPercentage}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRed
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Student List
            Text(
                "Students needing support (${group.students.size}):",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            group.students.forEach { studentData ->
                StudentInterventionItem(studentData)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun StudentInterventionItem(data: StudentInterventionData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = data.student.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = "ID: ${data.student.studentId}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
        
        Text(
            text = "${data.correctCount}/${data.totalQuestions} (${data.percentage}%)",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (data.percentage < 60) ErrorRed else WarningOrange
        )
    }
}

// ============================================================================
// HELPER COMPONENTS
// ============================================================================

@Composable
fun MVPTabButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .padding(2.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) PrimaryBlue else androidx.compose.ui.graphics.Color.Transparent,
            contentColor = if (isSelected) SurfaceWhite else TextSecondary
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = null
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun CompetencyChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

// ============================================================================
// CALCULATION FUNCTIONS
// ============================================================================

private fun calculateReteachPriorities(
    answerKeys: List<AnswerKeyEntity>,
    studentAnswers: List<StudentAnswerEntity>,
    questionMelcMappings: Map<Int, MelcEntity>
): List<ReteachPriority> {
    // Group by MELC and calculate mastery
    val melcPerformance = mutableMapOf<Long, MutableList<Pair<Int, Boolean>>>()
    
    questionMelcMappings.forEach { (questionNum, melc) ->
        val key = answerKeys.find { it.questionNumber == questionNum }
        val answers = studentAnswers.filter { it.questionNumber == questionNum }
        
        answers.forEach { answer ->
            val isCorrect = answer.answer == key?.correctAnswer
            melcPerformance.getOrPut(melc.id) { mutableListOf() }.add(questionNum to isCorrect)
        }
    }
    
    // Calculate priorities
    val priorities = melcPerformance.mapNotNull { (melcId, results) ->
        val melc = questionMelcMappings.values.find { it.id == melcId } ?: return@mapNotNull null
        val correctCount = results.count { it.second }
        val totalAttempts = results.size
        val masteryPercentage = if (totalAttempts > 0) (correctCount * 100) / totalAttempts else 0
        
        val affectedQuestions = results.map { it.first }.distinct()
        val affectedStudentCount = studentAnswers.filter { it.questionNumber in affectedQuestions }
            .map { it.studentEntityId }.distinct().size
        
        val priority = when {
            masteryPercentage < 50 -> Priority.URGENT
            masteryPercentage < 70 -> Priority.SOON
            else -> Priority.MONITOR
        }
        
        ReteachPriority(
            competency = melc,
            masteryPercentage = masteryPercentage,
            affectedQuestions = affectedQuestions,
            affectedStudentCount = affectedStudentCount,
            priority = priority
        )
    }.sortedBy { it.masteryPercentage }.take(3)
    
    return priorities
}

private fun calculateItemAnalysisWithIndices(
    answerKeys: List<AnswerKeyEntity>,
    studentAnswers: List<StudentAnswerEntity>,
    students: List<StudentEntity>
): List<ItemAnalysisData> {
    // First, calculate total scores for each student
    val studentScores = students.map { student ->
        val correctCount = studentAnswers
            .filter { it.studentEntityId == student.id }
            .count { answer ->
                val key = answerKeys.find { it.questionNumber == answer.questionNumber }
                answer.answer == key?.correctAnswer
            }
        student.id to correctCount
    }.toMap()
    
    // Sort students into top 27% and bottom 27%
    val sortedStudents = studentScores.entries.sortedByDescending { it.value }
    val top27Count = (sortedStudents.size * 0.27).toInt()
    val topStudents = sortedStudents.take(top27Count).map { it.key }.toSet()
    val bottomStudents = sortedStudents.takeLast(top27Count).map { it.key }.toSet()
    
    return answerKeys.map { key ->
        val answersForQuestion = studentAnswers.filter { it.questionNumber == key.questionNumber }
        val totalStudents = answersForQuestion.size
        val correctCount = answersForQuestion.count { it.answer == key.correctAnswer }
        
        // Difficulty Index (0.0 - 1.0)
        val difficultyIndex = if (totalStudents > 0) correctCount.toDouble() / totalStudents else 0.0
        
        // Discrimination Index
        val topCorrect = answersForQuestion.filter { it.studentEntityId in topStudents }
            .count { it.answer == key.correctAnswer }
        val bottomCorrect = answersForQuestion.filter { it.studentEntityId in bottomStudents }
            .count { it.answer == key.correctAnswer }
        
        val topPercentage = if (topStudents.size > 0) topCorrect.toDouble() / topStudents.size else 0.0
        val bottomPercentage = if (bottomStudents.size > 0) bottomCorrect.toDouble() / bottomStudents.size else 0.0
        val discriminationIndex = topPercentage - bottomPercentage
        
        // Classifications
        val difficulty = when {
            difficultyIndex >= 0.70 -> DifficultyLevel.EASY
            difficultyIndex >= 0.30 -> DifficultyLevel.MODERATE
            else -> DifficultyLevel.DIFFICULT
        }
        
        val discrimination = when {
            discriminationIndex >= 0.40 -> DiscriminationLevel.EXCELLENT
            discriminationIndex >= 0.30 -> DiscriminationLevel.GOOD
            discriminationIndex >= 0.20 -> DiscriminationLevel.FAIR
            else -> DiscriminationLevel.POOR
        }
        
        // Answer distribution
        val distribution = mutableMapOf<String, Int>()
        listOf("A", "B", "C", "D", "E").forEach { option ->
            distribution[option] = answersForQuestion.count { it.answer == option }
        }
        
        ItemAnalysisData(
            questionNumber = key.questionNumber,
            correctAnswer = key.correctAnswer,
            totalStudents = totalStudents,
            correctCount = correctCount,
            difficultyIndex = difficultyIndex,
            discriminationIndex = discriminationIndex,
            difficulty = difficulty,
            discrimination = discrimination,
            answerDistribution = distribution
        )
    }.sortedBy { it.questionNumber }
}

private fun calculateInterventionGroups(
    students: List<StudentEntity>,
    studentAnswers: List<StudentAnswerEntity>,
    answerKeys: List<AnswerKeyEntity>,
    questionMelcMappings: Map<Int, MelcEntity>
): List<InterventionGroup> {
    // For each student, calculate performance per competency
    val studentCompetencyPerformance = mutableMapOf<Long, MutableMap<Long, Pair<Int, Int>>>()
    
    students.forEach { student ->
        val studentAnswersList = studentAnswers.filter { it.studentEntityId == student.id }
        
        studentAnswersList.forEach { answer ->
            val melc = questionMelcMappings[answer.questionNumber]
            if (melc != null) {
                val key = answerKeys.find { it.questionNumber == answer.questionNumber }
                val isCorrect = answer.answer == key?.correctAnswer
                
                val performance = studentCompetencyPerformance
                    .getOrPut(student.id) { mutableMapOf() }
                    .getOrPut(melc.id) { 0 to 0 }
                
                studentCompetencyPerformance[student.id]!![melc.id] = 
                    (performance.first + if (isCorrect) 1 else 0) to (performance.second + 1)
            }
        }
    }
    
    // Group students by their weakest competency
    val competencyGroups = mutableMapOf<Long, MutableList<StudentInterventionData>>()
    
    studentCompetencyPerformance.forEach { (studentId, competencies) ->
        val student = students.find { it.id == studentId } ?: return@forEach
        
        // Find weakest competency for this student
        val weakest = competencies.minByOrNull { (_, performance) ->
            val (correct, total) = performance
            if (total > 0) (correct * 100) / total else 100
        }
        
        if (weakest != null) {
            val (correct, total) = weakest.value
            val percentage = if (total > 0) (correct * 100) / total else 0
            
            // Only include if below 70% mastery
            if (percentage < 70) {
                competencyGroups.getOrPut(weakest.key) { mutableListOf() }.add(
                    StudentInterventionData(
                        student = student,
                        correctCount = correct,
                        totalQuestions = total,
                        percentage = percentage
                    )
                )
            }
        }
    }
    
    // Create intervention groups
    return competencyGroups.mapNotNull { (melcId, studentList) ->
        val melc = questionMelcMappings.values.find { it.id == melcId } ?: return@mapNotNull null
        val avgMastery = studentList.map { it.percentage }.average().roundToInt()
        
        InterventionGroup(
            competency = melc,
            masteryPercentage = avgMastery,
            students = studentList.sortedBy { it.percentage }
        )
    }.sortedBy { it.masteryPercentage }.take(5)
}
