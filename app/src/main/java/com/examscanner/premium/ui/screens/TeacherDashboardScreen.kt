package com.examscanner.premium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.examscanner.premium.ui.components.FloatingGlassCard
import com.examscanner.premium.ui.theme.*
import com.examscanner.premium.viewmodel.ActionItems
import com.examscanner.premium.viewmodel.DashboardStats
import com.examscanner.premium.viewmodel.DashboardUiState
import com.examscanner.premium.viewmodel.DateRange
import com.examscanner.premium.viewmodel.RecentActivity

/**
 * TeacherDashboardScreen - Binds [com.examscanner.premium.viewmodel.DashboardViewModel]
 * state to the teacher dashboard (Requirement 12.1).
 *
 * Renders at-a-glance statistics, performance trends over a selectable date range,
 * actionable items (learning gaps / behind-schedule MELCs / flagged questions), and
 * recent activity. This is a stateless renderer: the caller owns the ViewModel and
 * passes its [DashboardUiState] plus the range-change/refresh callbacks so the screen
 * stays testable and free of DI concerns.
 *
 * Uses Azure Glass theme colors and content descriptions per workspace conventions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(
    state: DashboardUiState,
    onBack: () -> Unit,
    onRangeSelected: (DateRange) -> Unit,
    onRefresh: () -> Unit,
    onOpenStudentProfile: (Long) -> Unit = {},
    onOpenExam: (Long) -> Unit = {}
) {
    Scaffold(
        containerColor = IceWhite,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Dashboard",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryIce
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = ElectricBlue
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh dashboard",
                            tint = ElectricBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IceWhite)
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(IceWhite),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ElectricBlue)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(IceWhite),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            state.error?.let { error ->
                item { DashboardErrorCard(error) }
            }

            item { StatsSection(state.stats) }

            item {
                DateRangeSelector(
                    selected = state.selectedRange,
                    onRangeSelected = onRangeSelected
                )
            }

            item { TrendsSection(state) }

            item { ActionItemsSection(state.actionItems, onOpenExam = onOpenExam) }

            item {
                RecentActivitySection(
                    state.recentActivity,
                    onOpenStudentProfile = onOpenStudentProfile,
                    onOpenExam = onOpenExam
                )
            }
        }
    }
}

@Composable
private fun DashboardErrorCard(error: String) {
    FloatingGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ErrorCoral)
            Text(error, color = ErrorCoral, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ---- Stats (Req 12.2) ----

@Composable
private fun StatsSection(stats: DashboardStats) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DashboardSectionHeader("At a Glance")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                icon = Icons.Default.Assignment,
                label = "Exams",
                value = stats.totalExams.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Default.People,
                label = "Students",
                value = stats.totalStudents.toString(),
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                icon = Icons.Default.CheckCircle,
                label = "Assessed",
                value = stats.assessmentsCompleted.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Default.CalendarMonth,
                label = "Quarter",
                value = "Q${stats.currentQuarter}",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    FloatingGlassCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clearAndSetSemantics {
                    contentDescription = "$label: $value"
                },
            horizontalAlignment = Alignment.Start
        ) {
            Icon(icon, contentDescription = null, tint = IcyCyan, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricBlue
            )
            Text(label, fontSize = 13.sp, color = TextSecondaryIce)
        }
    }
}

// ---- Date range selector (Req 12.6) ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeSelector(
    selected: DateRange,
    onRangeSelected: (DateRange) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DateRange.values().forEach { range ->
            FilterChip(
                selected = range == selected,
                onClick = { onRangeSelected(range) },
                label = { Text(range.label()) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ElectricBlue,
                    selectedLabelColor = Color.White,
                    containerColor = IceBlue,
                    labelColor = TextPrimaryIce
                )
            )
        }
    }
}

private fun DateRange.label(): String = when (this) {
    DateRange.THIS_WEEK -> "This Week"
    DateRange.THIS_MONTH -> "This Month"
    DateRange.THIS_QUARTER -> "This Quarter"
    DateRange.THIS_YEAR -> "This Year"
}

// ---- Trends (Req 12.3) ----

@Composable
private fun TrendsSection(state: DashboardUiState) {
    val trends = state.trends
    FloatingGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Performance Trends",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryIce
            )
            Text(
                "Bucketed ${trends.interval.name.lowercase()}",
                fontSize = 12.sp,
                color = TextTertiaryIce
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (trends.averageScorePerInterval.isEmpty() &&
                trends.assessmentFrequencyPerInterval.isEmpty() &&
                trends.masteryDistribution.isEmpty()
            ) {
                Text(
                    "No assessment data for this range yet.",
                    fontSize = 13.sp,
                    color = TextSecondaryIce
                )
                return@Column
            }

            if (trends.averageScorePerInterval.isNotEmpty()) {
                Text("Average score", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondaryIce)
                Spacer(modifier = Modifier.height(6.dp))
                val maxScore = 100f
                trends.averageScorePerInterval.forEach { point ->
                    TrendBar(
                        label = point.label,
                        value = point.value,
                        fraction = (point.value / maxScore).coerceIn(0f, 1f),
                        valueText = "${point.value.toInt()}%",
                        color = ElectricBlue
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (trends.masteryDistribution.isNotEmpty()) {
                Text("Mastery distribution", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondaryIce)
                Spacer(modifier = Modifier.height(6.dp))
                val total = trends.masteryDistribution.values.sum().coerceAtLeast(1)
                trends.masteryDistribution.forEach { (level, count) ->
                    TrendBar(
                        label = level,
                        value = count.toFloat(),
                        fraction = count.toFloat() / total,
                        valueText = count.toString(),
                        color = IcyCyan
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendBar(
    label: String,
    value: Float,
    fraction: Float,
    valueText: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clearAndSetSemantics { contentDescription = "$label: $valueText" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 11.sp,
            color = TextSecondaryIce,
            modifier = Modifier.width(84.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(IceBlue)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(color)
            )
        }
        Text(
            valueText,
            fontSize = 11.sp,
            color = TextSecondaryIce,
            modifier = Modifier.width(48.dp).padding(start = 8.dp)
        )
    }
}

// ---- Action items (Req 12.4) ----

@Composable
private fun ActionItemsSection(
    actionItems: ActionItems,
    onOpenExam: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DashboardSectionHeader("Needs Attention")

        if (actionItems.learningGaps.isEmpty() &&
            actionItems.behindScheduleMelcs.isEmpty() &&
            actionItems.flaggedQuestions.isEmpty()
        ) {
            FloatingGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessAzure)
                    Text("You're all caught up.", color = TextSecondaryIce)
                }
            }
            return
        }

        actionItems.learningGaps.forEach { gap ->
            ActionItemCard(
                icon = Icons.Default.TrendingDown,
                tint = ErrorCoral,
                title = "Learning gap: ${gap.melc.code}",
                subtitle = "${gap.averageMastery.toInt()}% avg • ${gap.studentsBelow75}/${gap.totalStudents} below 75%"
            )
        }

        actionItems.behindScheduleMelcs.forEach { melc ->
            ActionItemCard(
                icon = Icons.Default.Schedule,
                tint = WarningAmber,
                title = "Behind schedule: ${melc.code}",
                subtitle = "${melc.subject} • Grade ${melc.gradeLevel} • Q${melc.quarter}"
            )
        }

        actionItems.flaggedQuestions.forEach { flagged ->
            ActionItemCard(
                icon = Icons.Default.Flag,
                tint = WarningAmber,
                title = "Review Q${flagged.flag.questionNumber} in ${flagged.examName}",
                subtitle = "Difficulty ${String.format("%.2f", flagged.flag.difficulty)} • Discrimination ${String.format("%.2f", flagged.flag.discrimination)}",
                onClick = { onOpenExam(flagged.examId) }
            )
        }
    }
}

@Composable
private fun ActionItemCard(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    FloatingGlassCard(
        modifier = if (onClick != null) {
            Modifier.fillMaxWidth().clickable { onClick() }
        } else {
            Modifier.fillMaxWidth()
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = TextPrimaryIce, fontSize = 14.sp)
                Text(subtitle, color = TextSecondaryIce, fontSize = 12.sp)
            }
            if (onClick != null) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextTertiaryIce)
            }
        }
    }
}

// ---- Recent activity (Req 12.5) ----

@Composable
private fun RecentActivitySection(
    recent: RecentActivity,
    onOpenStudentProfile: (Long) -> Unit,
    onOpenExam: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DashboardSectionHeader("Recent Activity")

        if (recent.recentExams.isEmpty() && recent.lastScannedSheets.isEmpty()) {
            FloatingGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "No recent activity.",
                    modifier = Modifier.padding(16.dp),
                    color = TextSecondaryIce
                )
            }
            return
        }

        recent.recentExams.forEach { exam ->
            ActionItemCard(
                icon = Icons.Default.Assignment,
                tint = ElectricBlue,
                title = exam.name,
                subtitle = "${exam.totalQuestions} questions",
                onClick = { onOpenExam(exam.id) }
            )
        }

        recent.lastScannedSheets.forEach { sheet ->
            ActionItemCard(
                icon = Icons.Default.DocumentScanner,
                tint = IcyCyan,
                title = "Scanned: ${sheet.name.ifBlank { "Student ${sheet.studentId}" }}",
                subtitle = "ID ${sheet.studentId}",
                onClick = { onOpenStudentProfile(sheet.id) }
            )
        }
    }
}

@Composable
private fun DashboardSectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondaryIce,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}
