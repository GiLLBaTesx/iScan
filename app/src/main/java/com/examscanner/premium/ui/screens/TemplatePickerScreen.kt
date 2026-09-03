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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.examscanner.premium.data.TemplateEntity
import com.examscanner.premium.ui.components.FloatingGlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatePickerScreen(
    templates: List<TemplateEntity>,
    selectedTemplateId: Long?,
    onBack: () -> Unit,
    onTemplateSelected: (TemplateEntity) -> Unit,
    onCreateCustom: () -> Unit
) {
    var showInfo by remember { mutableStateOf<TemplateEntity?>(null) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFAFAFC),
                        Color(0xFFF0F4F8)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF007AFF)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Answer Sheet Templates",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E)
                    )
                    Text(
                        text = "Choose or create a template",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8E8E93)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Create Custom Button
            FloatingGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCreateCustom)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "CREATE CUSTOM TEMPLATE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF007AFF)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Built-in Templates Section
            Text(
                text = "BUILT-IN TEMPLATES",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(templates.filter { it.isBuiltIn }) { template ->
                    TemplateCard(
                        template = template,
                        isSelected = template.id == selectedTemplateId,
                        onClick = { onTemplateSelected(template) },
                        onInfoClick = { showInfo = template }
                    )
                }
                
                // Custom Templates Section
                val customTemplates = templates.filter { !it.isBuiltIn }
                if (customTemplates.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "CUSTOM TEMPLATES",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF8E8E93),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    
                    items(customTemplates) { template ->
                        TemplateCard(
                            template = template,
                            isSelected = template.id == selectedTemplateId,
                            onClick = { onTemplateSelected(template) },
                            onInfoClick = { showInfo = template }
                        )
                    }
                }
            }
        }
    }
    
    // Template Info Dialog
    showInfo?.let { template ->
        TemplateInfoDialog(
            template = template,
            onDismiss = { showInfo = null }
        )
    }
}

@Composable
fun TemplateCard(
    template: TemplateEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    FloatingGlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Template Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isSelected) Color(0xFF007AFF).copy(alpha = 0.2f)
                        else Color(0xFF007AFF).copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    tint = if (isSelected) Color(0xFF007AFF) else Color(0xFF8E8E93),
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Template Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1C1C1E)
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = Color(0xFF34C759),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        text = "${template.totalQuestions} items",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8E8E93)
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8E8E93)
                    )
                    Text(
                        text = "${template.numberOfChoices} choices (${getChoiceRange(template.numberOfChoices)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8E8E93)
                    )
                }
                if (template.templateType != "STANDARD") {
                    Text(
                        text = getTemplateTypeLabel(template.templateType),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF007AFF)
                    )
                }
            }
            
            // Info Button
            IconButton(onClick = onInfoClick) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Info",
                    tint = Color(0xFF8E8E93)
                )
            }
        }
    }
}

@Composable
fun TemplateInfoDialog(
    template: TemplateEntity,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = template.name,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                InfoRow("Total Questions", "${template.totalQuestions}")
                InfoRow("Number of Choices", "${template.numberOfChoices} (${getChoiceRange(template.numberOfChoices)})")
                InfoRow("Template Type", getTemplateTypeLabel(template.templateType))
                InfoRow("QR Code Position", template.qrCodePosition.replace("_", " ").lowercase().capitalize())
                
                if (template.headerText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = Color(0xFFE5E5EA))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Header Text",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8E8E93)
                    )
                    Text(
                        text = template.headerText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF1C1C1E)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "💡 This template will be used to generate answer sheets for your students.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8E8E93)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = Color(0xFF007AFF))
            }
        }
    )
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF8E8E93)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1C1C1E)
        )
    }
}

private fun getChoiceRange(numberOfChoices: Int): String {
    return when (numberOfChoices) {
        2 -> "T/F"
        3 -> "A-C"
        4 -> "A-D"
        5 -> "A-E"
        6 -> "A-F"
        else -> "A-${('A' + numberOfChoices - 1)}"
    }
}

private fun getTemplateTypeLabel(templateType: String): String {
    return when (templateType) {
        "STANDARD" -> "Standard Format"
        "MULTI_SECTION" -> "Multiple Sections"
        "TRUE_FALSE" -> "True or False"
        else -> templateType.replace("_", " ").lowercase().capitalize()
    }
}
