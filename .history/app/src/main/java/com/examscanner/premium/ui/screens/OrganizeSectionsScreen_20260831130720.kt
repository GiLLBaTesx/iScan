package com.examscanner.premium.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.examscanner.premium.data.SectionEntity
import com.examscanner.premium.data.StudentEntity
import com.examscanner.premium.ui.components.FloatingGlassCard
import com.examscanner.premium.ui.components.GlassCard
import com.examscanner.premium.ui.theme.*

data class StudentWithSelection(
    val student: StudentEntity,
    var isSelected: Boolean = false
)

@Composable
fun OrganizeSectionsScreen(
    examId: Long,
    examName: String,
    students: List<StudentEntity>,
    sections: List<SectionEntity>,
    onBack: () -> Unit,
    onCreateSection: (String) -> Unit,
    onAssignStudents: (sectionId: Long, studentIds: List<Long>) -> Unit,
    onAutoOrganize: () -> Unit,
    onViewItemAnalysis: (sectionId: Long) -> Unit
) {
    var selectedTab by remember { mutableStateOf("UNASSIGNED") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAssignDialog by remember { mutableStateOf(false) }
    var selectedSectionId by remember { mutableStateOf<Long?>(null) }
    
    // Separate students by section assignment
    val unassignedStudents = remember(students) {
        students.filter { it.sectionId == 0L }.map { StudentWithSelection(it) }.toMutableStateList()
    }
    
    val studentsBySection = remember(students, sections) {
        sections.associateWith { section ->
            students.filter { it.sectionId == section.id }
        }
    }
    
    var selectedStudents by remember { mutableStateOf<List<Long>>(emptyList()) }
    var selectionMode by remember { mutableStateOf(false) }

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
                        Row {
                            if (selectionMode) {
                                IconButton(
                                    onClick = {
                                        selectionMode = false
                                        selectedStudents = emptyList()
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel Selection",
                                        tint = TextSecondary
                                    )
                                }
                            }
                            IconButton(
                                onClick = { showCreateDialog = true },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Create Section",
                                    tint = PrimaryBlue
                                )
                            }
                        }
                    }
                    
                    Text(
                        text = "Organize Classes",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = examName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "${students.size} students · ${sections.size} sections",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }

            // Action Buttons
            if (unassignedStudents.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        backgroundColor = WarningOrange,
                        cornerRadius = 12.dp
                    ) {
                        Button(
                            onClick = onAutoOrganize,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = androidx.compose.ui.graphics.Color.Transparent
                            ),
                            elevation = null
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = SurfaceWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "AUTO-ORGANIZE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SurfaceWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    if (selectedStudents.isNotEmpty()) {
                        GlassCard(
                            modifier = Modifier.weight(1f),
                            backgroundColor = PrimaryBlue,
                            cornerRadius = 12.dp
                        ) {
                            Button(
                                onClick = { showAssignDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                                ),
                                elevation = null
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Group,
                                        contentDescription = null,
                                        tint = SurfaceWhite,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "ASSIGN (${selectedStudents.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SurfaceWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
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
                        text = "UNASSIGNED (${unassignedStudents.size})",
                        isSelected = selectedTab == "UNASSIGNED",
                        modifier = Modifier.weight(1f)
                    ) { selectedTab = "UNASSIGNED" }
                    TabButton(
                        text = "SECTIONS (${sections.size})",
                        isSelected = selectedTab == "SECTIONS",
                        modifier = Modifier.weight(1f)
                    ) { selectedTab = "SECTIONS" }
                }
            }

            // Content
            when (selectedTab) {
                "UNASSIGNED" -> {
                    if (unassignedStudents.isEmpty()) {
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
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "All Organized!",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "All students have been assigned to sections",
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
                            items(unassignedStudents) { studentWithSel ->
                                UnassignedStudentCard(
                                    student = studentWithSel.student,
                                    isSelected = studentWithSel.student.id in selectedStudents,
                                    selectionMode = selectionMode,
                                    onClick = {
                                        if (selectionMode) {
                                            selectedStudents = if (studentWithSel.student.id in selectedStudents) {
                                                selectedStudents - studentWithSel.student.id
                                            } else {
                                                selectedStudents + studentWithSel.student.id
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        selectionMode = true
                                        selectedStudents = listOf(studentWithSel.student.id)
                                    }
                                )
                            }
                        }
                    }
                }
                
                "SECTIONS" -> {
                    if (sections.isEmpty()) {
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
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No Sections Yet",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Create sections to organize your students",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { showCreateDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PrimaryBlue
                                    )
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("CREATE SECTION")
                                }
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
                            items(sections) { section ->
                                SectionCard(
                                    section = section,
                                    studentCount = studentsBySection[section]?.size ?: 0,
                                    onClick = {
                                        if (studentsBySection[section]?.isNotEmpty() == true) {
                                            onViewItemAnalysis(section.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Section Dialog
    if (showCreateDialog) {
        var sectionName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    text = "Create Section",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a name for the new section/class",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = sectionName,
                        onValueChange = { if (it.length <= 50) sectionName = it },
                        label = { Text("Section Name") },
                        placeholder = { Text("e.g., Grade 7 - Einstein") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (sectionName.isNotBlank()) {
                            onCreateSection(sectionName)
                            showCreateDialog = false
                            sectionName = ""
                        }
                    },
                    enabled = sectionName.isNotBlank()
                ) {
                    Text("CREATE", color = PrimaryBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    sectionName = ""
                }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }

    // Assign to Section Dialog
    if (showAssignDialog && selectedStudents.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showAssignDialog = false },
            title = {
                Text(
                    text = "Assign to Section",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Select a section for ${selectedStudents.size} student(s)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sections) { section ->
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAssignStudents(section.id, selectedStudents)
                                        showAssignDialog = false
                                        selectedStudents = emptyList()
                                        selectionMode = false
                                    },
                                cornerRadius = 8.dp
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
                                            text = section.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${studentsBySection[section]?.size ?: 0} students",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = TextTertiary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAssignDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun UnassignedStudentCard(
    student: StudentEntity,
    isSelected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    FloatingGlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (selectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = PrimaryBlue
                        )
                    )
                }
                Column {
                    Text(
                        text = "ID ${student.studentId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = student.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun SectionCard(
    section: SectionEntity,
    studentCount: Int,
    onClick: () -> Unit
) {
    FloatingGlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick, enabled = studentCount > 0)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (studentCount > 0) PrimaryBlue.copy(alpha = 0.1f) else LightGray,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = if (studentCount > 0) PrimaryBlue else TextTertiary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = section.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "$studentCount students",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            if (studentCount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VIEW ANALYSIS",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = PrimaryBlue
                    )
                }
            }
        }
    }
}

@Composable
fun TabButton(
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
