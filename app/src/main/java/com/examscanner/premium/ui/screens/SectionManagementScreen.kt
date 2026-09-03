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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.examscanner.premium.data.SectionEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionManagementScreen(
    folderId: Long,
    folderName: String,
    sections: List<SectionEntity>,
    onBack: () -> Unit,
    onSectionClick: (SectionEntity) -> Unit,
    onAddSection: (String, Int) -> Unit,
    onEditSection: (SectionEntity, String, Int) -> Unit,
    onDeleteSection: (SectionEntity) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp.dp < 360.dp
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<SectionEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf<SectionEntity?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            "Sections/Classes",
                            fontSize = if (isSmallScreen) 16.sp else 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            folderName,
                            fontSize = if (isSmallScreen) 12.sp else 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E88E5),
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF4CAF50)
            ) {
                Icon(Icons.Default.Add, "Add Section", tint = Color.White)
            }
        }
    ) { padding ->
        if (sections.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No sections yet",
                        fontSize = 18.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Create sections to organize your students",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF5F5F5)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sections) { section ->
                    SectionCard(
                        section = section,
                        onClick = { onSectionClick(section) },
                        onEdit = { showEditDialog = section },
                        onDelete = { showDeleteDialog = section }
                    )
                }
            }
        }
    }
    
    // Add Section Dialog
    if (showAddDialog) {
        AddSectionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, capacity ->
                onAddSection(name, capacity)
                showAddDialog = false
            }
        )
    }
    
    // Edit Section Dialog
    showEditDialog?.let { section ->
        EditSectionDialog(
            section = section,
            onDismiss = { showEditDialog = null },
            onConfirm = { name, capacity ->
                onEditSection(section, name, capacity)
                showEditDialog = null
            }
        )
    }
    
    // Delete Confirmation
    showDeleteDialog?.let { section ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFF44336),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Delete Section?") },
            text = { 
                Text("Are you sure you want to delete ${section.name}? This will also remove all students in this section.") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSection(section)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336)
                    )
                ) {
                    Text("DELETE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
fun SectionCard(
    section: SectionEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFF1E88E5), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    section.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
                Text(
                    "Capacity: ${section.capacity} students",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            
            Box {
                IconButton(
                    onClick = { showMenu = !showMenu },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray)
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("View Students") },
                        onClick = {
                            showMenu = false
                            onClick()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.People, null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, null, tint = Color(0xFF1E88E5))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, null, tint = Color(0xFFF44336))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AddSectionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("50") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Section/Class") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Section Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g., Section A, Grade 10-Rizal") }
                )
                
                OutlinedTextField(
                    value = capacity,
                    onValueChange = { if (it.all { c -> c.isDigit() }) capacity = it },
                    label = { Text("Capacity") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Number of students") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cap = capacity.toIntOrNull() ?: 50
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), cap)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("ADD")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}

@Composable
fun EditSectionDialog(
    section: SectionEntity,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var name by remember { mutableStateOf(section.name) }
    var capacity by remember { mutableStateOf(section.capacity.toString()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Section") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Section Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = capacity,
                    onValueChange = { if (it.all { c -> c.isDigit() }) capacity = it },
                    label = { Text("Capacity") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cap = capacity.toIntOrNull() ?: section.capacity
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), cap)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("SAVE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}
