package com.examscanner.premium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.examscanner.premium.data.ExamEntity
import com.examscanner.premium.viewmodel.ExamViewModel

@Composable
fun MainScreenWithTabs(
    viewModel: ExamViewModel,
    onExamClick: (ExamEntity) -> Unit,
    onNewExamClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Assignment, contentDescription = null) },
                    label = { Text("Exams") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF007AFF),
                        selectedTextColor = Color(0xFF007AFF),
                        indicatorColor = Color(0xFF007AFF).copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                    label = { Text("Stats") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF007AFF),
                        selectedTextColor = Color(0xFF007AFF),
                        indicatorColor = Color(0xFF007AFF).copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF007AFF),
                        selectedTextColor = Color(0xFF007AFF),
                        indicatorColor = Color(0xFF007AFF).copy(alpha = 0.1f)
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> AllExamsTab(viewModel, onExamClick, onNewExamClick)
                1 -> StatsTab(viewModel)
                2 -> SettingsTab(viewModel)
            }
        }
    }
}

@Composable
fun AllExamsTab(
    viewModel: ExamViewModel,
    onExamClick: (ExamEntity) -> Unit,
    onNewExamClick: () -> Unit
) {
    val examState by viewModel.examState.collectAsState()
    val folders by viewModel.subjectFolders.collectAsState(initial = emptyList())
    
    var selectedFilter by remember { mutableStateOf("All") }
    var showFilterMenu by remember { mutableStateOf(false) }
    
    // Get all exams and group by subject
    val allExams = examState.exams
    val subjects = folders.map { it.name } + listOf("All")
    
    val filteredExams = if (selectedFilter == "All") {
        allExams
    } else {
        allExams.filter { examWithStats ->
            folders.find { it.id == examWithStats.exam.subjectFolderId }?.name == selectedFilter
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFC))
            .padding(20.dp)
    ) {
        // Header with filter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "All Exams",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1C1E)
                )
                Text(
                    text = "${filteredExams.size} exam${if (filteredExams.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8E8E93)
                )
            }
            
            // Filter button
            Box {
                FilterChip(
                    selected = selectedFilter != "All",
                    onClick = { showFilterMenu = true },
                    label = { Text(selectedFilter) },
                    leadingIcon = {
                        Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF007AFF).copy(alpha = 0.1f),
                        selectedLabelColor = Color(0xFF007AFF),
                        selectedLeadingIconColor = Color(0xFF007AFF)
                    )
                )
                
                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false }
                ) {
                    subjects.forEach { subject ->
                        DropdownMenuItem(
                            text = { Text(subject) },
                            onClick = {
                                selectedFilter = subject
                                showFilterMenu = false
                            },
                            leadingIcon = {
                                if (selectedFilter == subject) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF007AFF))
                                }
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // New Exam Button
        Button(
            onClick = onNewExamClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("NEW EXAM", fontWeight = FontWeight.SemiBold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Exams list
        if (filteredExams.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize()
            ) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Assignment,
                        contentDescription = null,
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (selectedFilter == "All") "No exams yet" else "No exams in $selectedFilter",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF8E8E93)
                    )
                }
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredExams.size) { index ->
                    val examWithStats = filteredExams[index]
                    val folder = folders.find { it.id == examWithStats.exam.subjectFolderId }
                    
                    QuickExamCard(
                        exam = examWithStats.exam,
                        subjectName = folder?.name ?: "No Subject",
                        scannedCount = examWithStats.scannedCount,
                        average = examWithStats.averageScore,
                        onClick = { onExamClick(examWithStats.exam) }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickExamCard(
    exam: ExamEntity,
    subjectName: String,
    scannedCount: Int,
    average: Double?,
    onClick: () -> Unit
) {
    androidx.compose.foundation.clickable.Clickable {
        androidx.compose.material3.Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = exam.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1C1C1E)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row {
                            androidx.compose.material3.AssistChip(
                                onClick = { },
                                label = { Text(subjectName, style = MaterialTheme.typography.labelSmall) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color(0xFF007AFF).copy(alpha = 0.1f),
                                    labelColor = Color(0xFF007AFF)
                                ),
                                border = null
                            )
                            if (exam.section.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                androidx.compose.material3.AssistChip(
                                    onClick = { },
                                    label = { Text(exam.section, style = MaterialTheme.typography.labelSmall) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = Color(0xFF34C759).copy(alpha = 0.1f),
                                        labelColor = Color(0xFF34C759)
                                    ),
                                    border = null
                                )
                            }
                        }
                    }
                    
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                        Text(
                            text = "$scannedCount scanned",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8E8E93)
                        )
                        if (average != null) {
                            Text(
                                text = "${String.format("%.1f", average)}% avg",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF007AFF)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "${exam.totalQuestions} questions",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8E93)
                )
            }
        }
    }
}

@Composable
fun StatsTab(viewModel: ExamViewModel) {
    val examState by viewModel.examState.collectAsState()
    val folders by viewModel.subjectFolders.collectAsState(initial = emptyList())
    
    val totalExams = examState.exams.size
    val totalScanned = examState.exams.sumOf { it.scannedCount }
    val overallAverage = examState.exams.mapNotNull { it.averageScore }.average().takeIf { !it.isNaN() }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFC))
            .padding(20.dp)
    ) {
        Text(
            text = "Statistics",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1C1C1E)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Stats cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                value = folders.size.toString(),
                label = "Subjects",
                icon = Icons.Default.Folder
            )
            StatCard(
                modifier = Modifier.weight(1f),
                value = totalExams.toString(),
                label = "Exams",
                icon = Icons.Default.Assignment
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                value = totalScanned.toString(),
                label = "Scanned",
                icon = Icons.Default.QrCodeScanner
            )
            StatCard(
                modifier = Modifier.weight(1f),
                value = overallAverage?.let { String.format("%.1f%%", it) } ?: "N/A",
                label = "Average",
                icon = Icons.Default.TrendingUp
            )
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1C1E)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8E8E93)
            )
        }
    }
}

@Composable
fun SettingsTab(viewModel: ExamViewModel) {
    val folders by viewModel.subjectFolders.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    var showNewSubjectDialog by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFC))
            .padding(20.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1C1C1E)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Subjects management
        Text(
            text = "SUBJECTS",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF8E8E93),
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Button(
            onClick = { showNewSubjectDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("ADD SUBJECT")
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        folders.forEach { folder ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(folder.name, fontWeight = FontWeight.Medium)
                    IconButton(onClick = {
                        kotlinx.coroutines.launch {
                            viewModel.deleteSubjectFolder(folder.id)
                        }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF3B30))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Templates
        Text(
            text = "TEMPLATES",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF8E8E93),
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Button(
            onClick = { showTemplateDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759))
        ) {
            Icon(Icons.Default.Download, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("DOWNLOAD TEMPLATES")
        }
    }
    
    if (showNewSubjectDialog) {
        NewSubjectFolderDialog(
            onDismiss = { showNewSubjectDialog = false },
            onCreate = { name ->
                kotlinx.coroutines.launch {
                    viewModel.createSubjectFolder(name)
                    android.widget.Toast.makeText(context, "Subject added!", android.widget.Toast.LENGTH_SHORT).show()
                }
                showNewSubjectDialog = false
            }
        )
    }
    
    if (showTemplateDialog) {
        TemplateDownloadDialog(
            onDismiss = { showTemplateDialog = false },
            context = context
        )
    }
}
