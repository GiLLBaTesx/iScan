package com.examscanner.premium.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.examscanner.premium.analytics.AnalyticsEngine
import com.examscanner.premium.data.AnswerKeyEntity
import com.examscanner.premium.data.StudentAnswerEntity
import com.examscanner.premium.data.StudentScore
import com.examscanner.premium.ui.components.FloatingGlassCard
import com.examscanner.premium.ui.theme.*
import com.examscanner.premium.viewmodel.AnalyticsViewModel
import com.examscanner.premium.viewmodel.QuestionAnalyticsRow
import kotlin.math.roundToInt

/**
 * ItemAnalysisPanel - Task 11.1 item-analysis visualizations.
 *
 * Full-screen overlay hosted by [ExamDetailScreen] that renders, alongside the
 * exam's existing item-analysis bar charts (in SmartDashboardMVP), three new
 * visualizations required by Requirements 6.4 / 6.6 / 17.4 / 17.5:
 *
 * - Item response curve for a selected question (fed by [AnalyticsViewModel];
 *   Req 6.4). Advanced-analytics gating from the ViewModel is honored via an
 *   upgrade prompt when locked.
 * - Answer-distribution bars per question (computed from the already-loaded
 *   [studentAnswers] + [answerKeys]; Req 17.4/17.5 preserve existing bars).
 * - Class mastery distribution as a pie chart plus a per-student score line
 *   chart (Req 6.6 analytics feed).
 *
 * All colors come from the Azure Glass theme; graphical/interactive elements
 * carry content descriptions for accessibility (Req 6.6).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemAnalysisPanel(
    examId: Long,
    examName: String,
    answerKeys: List<AnswerKeyEntity>,
    studentAnswers: List<StudentAnswerEntity>,
    students: List<StudentScore>,
    analyticsViewModel: AnalyticsViewModel,
    onBack: () -> Unit,
    // When true the panel is rendered inside a host that already supplies its own
    // Scaffold + TopAppBar (e.g. the Analytics tab of ExamDetailScreen). In that case we
    // drop the inner Scaffold/TopAppBar to avoid a doubled app bar and a dead back arrow,
    // and render the content directly. Defaults to false so standalone/full-screen callers
    // keep the original behavior unchanged.
    embedded: Boolean = false
) {
    if (embedded) {
        ItemAnalysisContent(
            examId = examId,
            answerKeys = answerKeys,
            studentAnswers = studentAnswers,
            students = students,
            analyticsViewModel = analyticsViewModel,
            outerPadding = null
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Item Analysis",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = examName,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryIce
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Close item analysis"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FrostedWhite)
            )
        }
    ) { padding ->
        ItemAnalysisContent(
            examId = examId,
            answerKeys = answerKeys,
            studentAnswers = studentAnswers,
            students = students,
            analyticsViewModel = analyticsViewModel,
            outerPadding = padding
        )
    }
}

/**
 * The scrollable item-analysis body, shared by the standalone (Scaffold-wrapped) and the
 * embedded (tab-hosted) presentations. When [outerPadding] is provided it is applied first
 * (standalone case, consuming the Scaffold insets); embedded callers pass null.
 */
@Composable
private fun ItemAnalysisContent(
    examId: Long,
    answerKeys: List<AnswerKeyEntity>,
    studentAnswers: List<StudentAnswerEntity>,
    students: List<StudentScore>,
    analyticsViewModel: AnalyticsViewModel,
    outerPadding: PaddingValues?
) {
    val state by analyticsViewModel.state.collectAsState()

    val sortedKeys = remember(answerKeys) { answerKeys.sortedBy { it.questionNumber } }
    var selectedQuestion by remember(sortedKeys) {
        mutableStateOf(sortedKeys.firstOrNull()?.questionNumber ?: 1)
    }

    // Load the basic item analysis once, then the response curve for the
    // initially-selected question.
    LaunchedEffect(examId) {
        analyticsViewModel.loadItemAnalysis(examId)
    }
    LaunchedEffect(examId, selectedQuestion) {
        analyticsViewModel.loadItemResponseCurve(examId, selectedQuestion)
    }

    val baseModifier = Modifier
        .fillMaxSize()
        .background(IceWhite)
    LazyColumn(
        modifier = if (outerPadding != null) {
            baseModifier.padding(outerPadding).padding(horizontal = 16.dp)
        } else {
            baseModifier.padding(horizontal = 16.dp)
        },
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Question selector
        item {
            QuestionSelectorRow(
                questionNumbers = sortedKeys.map { it.questionNumber },
                selected = selectedQuestion,
                onSelect = { selectedQuestion = it }
            )
        }

        // Item response curve (Req 6.4)
        item {
            ItemResponseCurveCard(
                questionNumber = selectedQuestion,
                correctAnswer = sortedKeys.find { it.questionNumber == selectedQuestion }?.correctAnswer,
                curve = state.itemResponseCurve,
                isLocked = state.advancedAnalyticsLocked,
                isLoading = state.isLoading
            )
        }

        // Answer-distribution bars (Req 17.4 / 17.5)
        item {
            AnswerDistributionCard(
                questionNumber = selectedQuestion,
                answerKeys = sortedKeys,
                studentAnswers = studentAnswers,
                totalStudents = students.size
            )
        }

        // Difficulty / discrimination row for the selected question
        item {
            val row = state.questionRows.find { it.questionNumber == selectedQuestion }
            QuestionQualityCard(row)
        }

        // Class mastery distribution pie (Req 6.6)
        item {
            MasteryDistributionCard(students = students)
        }

        // Per-student score line chart (Req 6.6)
        item {
            ScoreTrendCard(students = students)
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionSelectorRow(
    questionNumbers: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    FloatingGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Select question",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondaryIce
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(questionNumbers) { q ->
                    val isSelected = q == selected
                    Surface(
                        onClick = { onSelect(q) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) ElectricBlue else IceBlue,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .widthIn(min = 48.dp)
                            .semantics {
                                contentDescription =
                                    "Question $q" + if (isSelected) ", selected" else ""
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Q$q",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                color = if (isSelected) FrostedWhite else ElectricBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemResponseCurveCard(
    questionNumber: Int,
    correctAnswer: String?,
    curve: AnalyticsEngine.ItemResponseCurve?,
    isLocked: Boolean,
    isLoading: Boolean
) {
    FloatingGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle(
                title = "Item Response Curve",
                subtitle = "Q$questionNumber — how responses spread across options"
            )
            Spacer(modifier = Modifier.height(12.dp))

            when {
                isLocked -> {
                    UpgradePrompt(
                        message = "The item response curve is a premium analytics feature. Upgrade to unlock detailed per-option response breakdowns."
                    )
                }
                isLoading && curve == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ElectricBlue)
                    }
                }
                curve == null || curve.responses.isEmpty() -> {
                    EmptyHint("No responses recorded for this question yet.")
                }
                else -> {
                    val total = curve.totalResponses.coerceAtLeast(1)
                    curve.responses.entries.forEach { (option, count) ->
                        val pct = (count * 100f / total).roundToInt()
                        val isCorrect = correctAnswer != null && option == correctAnswer
                        HorizontalStatBar(
                            label = option,
                            count = count,
                            percentage = pct,
                            barColor = if (isCorrect) SuccessAzure else IcyCyan,
                            descriptionPrefix = if (isCorrect) "Correct option" else "Option"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnswerDistributionCard(
    questionNumber: Int,
    answerKeys: List<AnswerKeyEntity>,
    studentAnswers: List<StudentAnswerEntity>,
    totalStudents: Int
) {
    val key = answerKeys.find { it.questionNumber == questionNumber }
    val correctAnswer = key?.correctAnswer

    // Build the distribution from the already-loaded student answers.
    val distribution = remember(questionNumber, studentAnswers) {
        studentAnswers
            .filter { it.questionNumber == questionNumber }
            .groupingBy { it.answer.ifBlank { AnalyticsEngine.NO_ANSWER_LABEL } }
            .eachCount()
    }
    val options = remember(distribution) {
        val base = listOf("A", "B", "C", "D", "E")
        (base + distribution.keys).distinct()
            .filter { distribution.containsKey(it) || it in base }
    }

    FloatingGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle(
                title = "Answer Distribution",
                subtitle = "Q$questionNumber — count per selected option"
            )
            Spacer(modifier = Modifier.height(12.dp))
            val total = totalStudents.coerceAtLeast(1)
            options.forEach { option ->
                val count = distribution[option] ?: 0
                val pct = (count * 100f / total).roundToInt()
                val isCorrect = correctAnswer != null && option == correctAnswer
                HorizontalStatBar(
                    label = option,
                    count = count,
                    percentage = pct,
                    barColor = if (isCorrect) SuccessAzure else LuminousAzure,
                    descriptionPrefix = if (isCorrect) "Correct answer" else "Option"
                )
            }
        }
    }
}

@Composable
private fun QuestionQualityCard(row: QuestionAnalyticsRow?) {
    FloatingGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle(
                title = "Difficulty & Discrimination",
                subtitle = "Classical test-theory indices"
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (row == null) {
                EmptyHint("Indices unavailable for this question.")
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricTile(
                        modifier = Modifier.weight(1f),
                        label = "Difficulty",
                        value = "${row.difficulty.roundToInt()}%",
                        accent = ElectricBlue
                    )
                    MetricTile(
                        modifier = Modifier.weight(1f),
                        label = "Discrimination",
                        value = String.format("%.2f", row.discrimination),
                        accent = classificationColor(row.classification)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                val statusColor = classificationColor(row.classification)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(statusColor)
                            .semantics {
                                contentDescription =
                                    "Quality indicator: ${row.classification.name.lowercase()}"
                            }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = row.classification.name.lowercase()
                            .replaceFirstChar { it.uppercase() } +
                            if (row.isLowQuality) " — flagged low quality" else "",
                        fontSize = 13.sp,
                        color = if (row.isLowQuality) ErrorCoral else TextSecondaryIce,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun MasteryDistributionCard(students: List<StudentScore>) {
    // Mastery bands mirror MasteryCalculator (Developing / Approaching / Proficient / Advanced).
    val bands = remember(students) {
        val developing = students.count { it.percentage < 75 }
        val approaching = students.count { it.percentage in 75..79 }
        val proficient = students.count { it.percentage in 80..89 }
        val advanced = students.count { it.percentage >= 90 }
        listOf(
            MasterySlice("Developing", developing, ErrorCoral),
            MasterySlice("Approaching", approaching, WarningAmber),
            MasterySlice("Proficient", proficient, IcyCyan),
            MasterySlice("Advanced", advanced, ElectricBlue)
        )
    }
    val total = students.size

    FloatingGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle(
                title = "Class Mastery Distribution",
                subtitle = "Students by mastery band"
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (total == 0) {
                EmptyHint("No scored students yet.")
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MasteryPie(
                        slices = bands,
                        total = total,
                        modifier = Modifier
                            .size(140.dp)
                            .semantics {
                                contentDescription = "Mastery distribution pie chart. " +
                                    bands.joinToString(", ") { "${it.label} ${it.count}" }
                            }
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        bands.forEach { slice ->
                            val pct = (slice.count * 100f / total).roundToInt()
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(slice.color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${slice.label}: ${slice.count} ($pct%)",
                                    fontSize = 13.sp,
                                    color = TextPrimaryIce
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreTrendCard(students: List<StudentScore>) {
    val scores = remember(students) {
        students.sortedByDescending { it.percentage }.map { it.percentage }
    }
    FloatingGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle(
                title = "Score Distribution",
                subtitle = "Per-student percentage (ranked high to low)"
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (scores.size < 2) {
                EmptyHint("Need at least two scored students to draw the trend.")
            } else {
                ScoreLineChart(
                    scores = scores,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .semantics {
                            contentDescription = "Score line chart across ${scores.size} students, " +
                                "ranging ${scores.min()} to ${scores.max()} percent"
                        }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Shared small building blocks
// ---------------------------------------------------------------------------

private data class MasterySlice(val label: String, val count: Int, val color: Color)

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryIce
        )
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = TextSecondaryIce
        )
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = TextTertiaryIce,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun UpgradePrompt(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(IceBlue)
            .padding(16.dp)
    ) {
        Text(
            text = message,
            fontSize = 13.sp,
            color = ElectricBlue,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun HorizontalStatBar(
    label: String,
    count: Int,
    percentage: Int,
    barColor: Color,
    descriptionPrefix: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .semantics {
                contentDescription = "$descriptionPrefix $label: $count responses, $percentage percent"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimaryIce,
            modifier = Modifier.width(48.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(IceBlue)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((percentage / 100f).coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(6.dp))
                    .background(barColor)
            )
        }
        Text(
            text = "$count ($percentage%)",
            fontSize = 12.sp,
            color = TextSecondaryIce,
            modifier = Modifier
                .width(84.dp)
                .padding(start = 8.dp)
        )
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.1f))
            .padding(14.dp)
    ) {
        Column {
            Text(text = label, fontSize = 11.sp, color = TextSecondaryIce)
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
    }
}

@Composable
private fun MasteryPie(
    slices: List<MasterySlice>,
    total: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        var startAngle = -90f
        val safeTotal = total.coerceAtLeast(1)
        slices.forEach { slice ->
            if (slice.count > 0) {
                val sweep = slice.count.toFloat() / safeTotal * 360f
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true
                )
                startAngle += sweep
            }
        }
        // Inner cut-out for a donut feel using the ice-white canvas color.
        drawCircle(
            color = FrostedWhite,
            radius = size.minDimension / 4f,
            center = center
        )
    }
}

@Composable
private fun ScoreLineChart(
    scores: List<Int>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (scores.size < 2) return@Canvas
        val maxV = 100f
        val minV = 0f
        val range = (maxV - minV).coerceAtLeast(1f)
        val stepX = size.width / (scores.size - 1)

        fun yFor(v: Int): Float =
            size.height - ((v - minV) / range) * size.height

        // Baseline grid at the 75% passing threshold.
        val thresholdY = yFor(75)
        drawLine(
            color = TextTertiaryIce.copy(alpha = 0.4f),
            start = Offset(0f, thresholdY),
            end = Offset(size.width, thresholdY),
            strokeWidth = 2f
        )

        val path = Path()
        scores.forEachIndexed { index, score ->
            val x = index * stepX
            val y = yFor(score)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = ElectricBlue,
            style = Stroke(width = 5f, cap = StrokeCap.Round)
        )

        scores.forEachIndexed { index, score ->
            drawCircle(
                color = if (score >= 75) SuccessAzure else ErrorCoral,
                radius = 6f,
                center = Offset(index * stepX, yFor(score))
            )
        }
    }
}

private fun classificationColor(quality: AnalyticsEngine.DiscriminationQuality): Color =
    when (quality) {
        AnalyticsEngine.DiscriminationQuality.EXCELLENT -> SuccessAzure
        AnalyticsEngine.DiscriminationQuality.GOOD -> IcyCyan
        AnalyticsEngine.DiscriminationQuality.FAIR -> WarningAmber
        AnalyticsEngine.DiscriminationQuality.POOR -> ErrorCoral
    }
