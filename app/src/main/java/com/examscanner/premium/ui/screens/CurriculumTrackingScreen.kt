package com.examscanner.premium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.examscanner.premium.analytics.PacingEngine
import com.examscanner.premium.data.ExamRepository
import com.examscanner.premium.data.MelcEntity
import com.examscanner.premium.data.SubjectFolderEntity
import com.examscanner.premium.ui.components.FloatingGlassCard
import com.examscanner.premium.ui.theme.*
import com.examscanner.premium.utils.SecureLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * CurriculumTrackingScreen - Surfaces DepEd MELC curriculum pacing.
 *
 * Wires the existing [PacingEngine] output into a dedicated, least-invasive
 * screen (rather than editing the shared dashboard) so teachers can:
 *  - See MELC coverage % per quarter for a subject + grade level (Req 9.1, 9.2, 9.5).
 *  - Review the behind-schedule MELC list (Req 9.4).
 *  - Mark individual MELCs covered or skipped (Req 9.6), persisted through
 *    [PacingEngine.markMelcCovered] / [PacingEngine.markMelcSkipped].
 *
 * MELC codes and descriptions are always shown in English (Req 9 / 20.5).
 *
 * The screen builds its own [PacingEngine] from the [repository] it is handed
 * (matching how [ExamRepository] is created once in MainActivity and passed
 * down); no separate ViewModel is required for this read-mostly surface.
 */
@Composable
fun CurriculumTrackingScreen(
    repository: ExamRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val pacingEngine = remember(repository) { PacingEngine(repository) }

    // Available subject/grade combinations, derived from the seeded MELC set.
    var allMelcs by remember { mutableStateOf<List<MelcEntity>>(emptyList()) }
    var subjectFolders by remember { mutableStateOf<List<SubjectFolderEntity>>(emptyList()) }
    var loadError by remember { mutableStateOf<String?>(null) }

    // Selection state.
    var selectedSubject by remember { mutableStateOf<String?>(null) }
    var selectedGrade by remember { mutableStateOf<String?>(null) }
    var selectedQuarter by remember { mutableStateOf(1) }

    // Current pacing guide + quarterly summary for the selection.
    var pacingGuide by remember { mutableStateOf<PacingEngine.PacingGuide?>(null) }
    var quarterlySummary by remember { mutableStateOf<PacingEngine.QuarterlySummary?>(null) }
    var isLoadingGuide by remember { mutableStateOf(false) }
    // Bumped after a mark action to force a guide refresh.
    var refreshTrigger by remember { mutableStateOf(0) }

    // Load MELC catalog + subject folders once.
    LaunchedEffect(Unit) {
        try {
            allMelcs = repository.getAllMelcs().first()
            subjectFolders = repository.getAllSubjectFolders().first()
            val firstSubject = allMelcs.map { it.subject }.distinct().sorted().firstOrNull()
            selectedSubject = firstSubject
            selectedGrade = allMelcs.filter { it.subject == firstSubject }
                .map { it.gradeLevel }.distinct().sorted().firstOrNull()
        } catch (e: Exception) {
            SecureLogger.e("CurriculumTracking", "Failed to load MELC catalog", e)
            loadError = e.message ?: "Failed to load curriculum data"
        }
    }

    val subjects = remember(allMelcs) { allMelcs.map { it.subject }.distinct().sorted() }
    val grades = remember(allMelcs, selectedSubject) {
        allMelcs.filter { it.subject == selectedSubject }
            .map { it.gradeLevel }.distinct().sorted()
    }

    // Recompute the pacing guide + summary whenever the selection or a mark action changes.
    LaunchedEffect(selectedSubject, selectedGrade, selectedQuarter, refreshTrigger) {
        val subject = selectedSubject
        val grade = selectedGrade
        if (subject == null || grade == null) {
            pacingGuide = null
            quarterlySummary = null
            return@LaunchedEffect
        }
        isLoadingGuide = true
        try {
            // currentWeek at end-of-quarter surfaces every not-yet-covered MELC
            // as behind-schedule, which is the useful default for review.
            pacingGuide = pacingEngine.getPacingGuide(
                subject = subject,
                gradeLevel = grade,
                quarter = selectedQuarter,
                currentWeek = PacingEngine.WEEKS_PER_QUARTER
            )
            quarterlySummary = pacingEngine.getQuarterlySummary(
                subject = subject,
                gradeLevel = grade,
                schoolYear = ""
            )
        } catch (e: Exception) {
            SecureLogger.e("CurriculumTracking", "Failed to build pacing guide", e)
            loadError = e.message ?: "Failed to build pacing guide"
        } finally {
            isLoadingGuide = false
        }
    }

    // Resolve a subject folder id for the current subject so mark actions can
    // persist a coverage row (melc_coverage requires a subjectId FK).
    val subjectFolderId: Long? = remember(subjectFolders, selectedSubject) {
        selectedSubject?.let { subject ->
            subjectFolders.firstOrNull { it.name.equals(subject, ignoreCase = true) }?.id
                ?: subjectFolders.firstOrNull()?.id
        }
    }

    fun markCovered(melc: MelcEntity) {
        val folderId = subjectFolderId ?: return
        scope.launch {
            try {
                pacingEngine.markMelcCovered(melcId = melc.id, subjectId = folderId)
                refreshTrigger++
            } catch (e: Exception) {
                SecureLogger.e("CurriculumTracking", "markMelcCovered failed", e)
            }
        }
    }

    fun markSkipped(melc: MelcEntity) {
        val folderId = subjectFolderId ?: return
        scope.launch {
            try {
                pacingEngine.markMelcSkipped(melcId = melc.id, subjectId = folderId)
                refreshTrigger++
            } catch (e: Exception) {
                SecureLogger.e("CurriculumTracking", "markMelcSkipped failed", e)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IceWhite)
    ) {
        // Header
        FloatingGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Navigate back",
                        tint = ElectricBlue
                    )
                }
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = IcyCyan,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Curriculum Tracking",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryIce
                    )
                    Text(
                        text = "MELC coverage and pacing",
                        fontSize = 13.sp,
                        color = TextSecondaryIce
                    )
                }
            }
        }

        if (loadError != null) {
            FloatingGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = loadError ?: "",
                    color = ErrorRed,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
            return@Column
        }

        if (subjects.isEmpty()) {
            EmptyCurriculumState()
            return@Column
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Subject + grade selectors
            item {
                SelectorRow(
                    label = "Subject",
                    options = subjects,
                    selected = selectedSubject,
                    onSelect = { subject ->
                        selectedSubject = subject
                        selectedGrade = allMelcs.filter { it.subject == subject }
                            .map { it.gradeLevel }.distinct().sorted().firstOrNull()
                    }
                )
            }
            item {
                SelectorRow(
                    label = "Grade Level",
                    options = grades,
                    selected = selectedGrade,
                    onSelect = { selectedGrade = it }
                )
            }

            // Quarterly coverage summary (coverage % by quarter) - Req 9.1, 9.2, 9.5
            quarterlySummary?.let { summary ->
                item {
                    QuarterlyCoverageCard(
                        summary = summary,
                        selectedQuarter = selectedQuarter,
                        onQuarterSelected = { selectedQuarter = it }
                    )
                }
            }

            if (isLoadingGuide) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ElectricBlue)
                    }
                }
            }

            // Behind-schedule MELCs - Req 9.4
            pacingGuide?.let { guide ->
                if (guide.behindSchedule.isNotEmpty()) {
                    item {
                        BehindScheduleCard(behindSchedule = guide.behindSchedule)
                    }
                }

                item {
                    Text(
                        text = "MELCs - Quarter ${guide.quarter}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryIce,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (guide.melcs.isEmpty()) {
                    item {
                        FloatingGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "No MELCs found for this subject, grade, and quarter.",
                                color = TextSecondaryIce,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(guide.melcs) { melcWithSchedule ->
                        MelcCoverageCard(
                            item = melcWithSchedule,
                            onMarkCovered = { markCovered(melcWithSchedule.melc) },
                            onMarkSkipped = { markSkipped(melcWithSchedule.melc) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCurriculumState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        FloatingGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = IcyCyan,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No curriculum data available",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryIce
                )
                Text(
                    text = "MELC competencies will appear here once loaded.",
                    fontSize = 13.sp,
                    color = TextSecondaryIce
                )
            }
        }
    }
}

@Composable
private fun SelectorRow(
    label: String,
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit
) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondaryIce
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(option) },
                    label = { Text(option) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricBlue,
                        selectedLabelColor = FrostedWhite,
                        containerColor = IceBlue,
                        labelColor = TextPrimaryIce
                    ),
                    modifier = Modifier.semantics {
                        contentDescription = "$label $option" +
                            if (isSelected) ", selected" else ""
                    }
                )
            }
        }
    }
}

@Composable
private fun QuarterlyCoverageCard(
    summary: PacingEngine.QuarterlySummary,
    selectedQuarter: Int,
    onQuarterSelected: (Int) -> Unit
) {
    FloatingGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Coverage by Quarter",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryIce
                )
                Text(
                    text = "Overall ${summary.overallCoverage.toInt()}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IcyCyan
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            summary.quarters.forEach { quarter ->
                QuarterProgressRow(
                    quarter = quarter,
                    isSelected = quarter.quarter == selectedQuarter,
                    onClick = { onQuarterSelected(quarter.quarter) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun QuarterProgressRow(
    quarter: PacingEngine.QuarterProgress,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val fraction = (quarter.coveragePercentage / 100f).coerceIn(0f, 1f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) IceBlue else Color.Transparent)
            .semantics {
                contentDescription = "Quarter ${quarter.quarter}, " +
                    "${quarter.coveragePercentage.toInt()} percent covered, " +
                    "${quarter.assessedMelcs} of ${quarter.totalMelcs} MELCs" +
                    if (isSelected) ", selected" else ""
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Quarter ${quarter.quarter}",
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = TextPrimaryIce
            )
            Text(
                text = "${quarter.coveragePercentage.toInt()}%  (${quarter.assessedMelcs}/${quarter.totalMelcs})",
                fontSize = 13.sp,
                color = TextSecondaryIce
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(LightGray.copy(alpha = 0.4f))
                .clearAndSetSemantics { }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(if (fraction >= 0.75f) SuccessAzure else IcyCyan)
            )
        }
        // Clickable overlay row for quarter selection.
        TextButton(
            onClick = onClick,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(
                text = if (isSelected) "Viewing" else "View",
                fontSize = 12.sp,
                color = if (isSelected) IcyCyan else ElectricBlue
            )
        }
    }
}

@Composable
private fun BehindScheduleCard(behindSchedule: List<MelcEntity>) {
    FloatingGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = WarningAmber,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Behind Schedule (${behindSchedule.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryIce
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            behindSchedule.forEach { melc ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .semantics {
                            contentDescription =
                                "Behind schedule: ${melc.code}. ${melc.description}"
                        }
                ) {
                    Text(
                        text = "• ",
                        fontSize = 13.sp,
                        color = WarningAmber
                    )
                    Column {
                        Text(
                            text = melc.code,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WarningAmber
                        )
                        Text(
                            text = melc.description,
                            fontSize = 13.sp,
                            color = TextPrimaryIce,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MelcCoverageCard(
    item: PacingEngine.MelcWithSchedule,
    onMarkCovered: () -> Unit,
    onMarkSkipped: () -> Unit
) {
    val statusColor = when (item.status) {
        PacingEngine.CoverageStatus.FULLY_ASSESSED -> SuccessAzure
        PacingEngine.CoverageStatus.PARTIALLY_ASSESSED -> IcyCyan
        PacingEngine.CoverageStatus.BEHIND_SCHEDULE -> WarningAmber
        PacingEngine.CoverageStatus.NOT_ASSESSED -> TextTertiaryIce
    }
    val statusLabel = when (item.status) {
        PacingEngine.CoverageStatus.FULLY_ASSESSED -> "Fully assessed"
        PacingEngine.CoverageStatus.PARTIALLY_ASSESSED -> "Marked covered"
        PacingEngine.CoverageStatus.BEHIND_SCHEDULE -> "Behind schedule"
        PacingEngine.CoverageStatus.NOT_ASSESSED -> "Not yet assessed"
    }

    FloatingGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.melc.code,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricBlue
                    )
                    // MELC description kept in English (Req 9 / 20.5).
                    Text(
                        text = item.melc.description,
                        fontSize = 14.sp,
                        color = TextPrimaryIce,
                        lineHeight = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Non-color-only status indicator: colored chip WITH a text label.
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .semantics { contentDescription = "Status: $statusLabel" }
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Recommended week ${item.recommendedWeek}",
                fontSize = 11.sp,
                color = TextTertiaryIce
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onMarkCovered,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .semantics {
                            contentDescription = "Mark ${item.melc.code} as covered"
                        },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessAzure)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Covered", fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onMarkSkipped,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .semantics {
                            contentDescription = "Mark ${item.melc.code} as skipped"
                        },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue)
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Skip", fontSize = 13.sp)
                }
            }
        }
    }
}
