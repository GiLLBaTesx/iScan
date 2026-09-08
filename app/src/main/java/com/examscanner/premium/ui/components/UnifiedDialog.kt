package com.examscanner.premium.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.examscanner.premium.ui.theme.*

/**
 * Unified Dialog Component - Novelty Azure Glass Design
 * Frosted glass with electric cerulean accents and specular borders
 */
@Composable
fun UnifiedDialog(
    onDismissRequest: () -> Unit,
    title: String,
    icon: ImageVector? = null,
    iconTint: Color = ElectricBlue,
    content: @Composable ColumnScope.() -> Unit,
    confirmText: String = "CONFIRM",
    dismissText: String = "CANCEL",
    onConfirm: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    confirmEnabled: Boolean = true,
    isDangerous: Boolean = false,
    properties: DialogProperties = DialogProperties()
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
        containerColor = GlassOverlay, // Frosted glass overlay
        iconContentColor = iconTint,
        titleContentColor = TextPrimaryIce,
        textContentColor = TextSecondaryIce,
        icon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = if (isDangerous) ErrorCoral else iconTint,
                    modifier = Modifier.size(48.dp)
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryIce
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        },
        confirmButton = {
            if (onConfirm != null) {
                Button(
                    onClick = onConfirm,
                    enabled = confirmEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDangerous) ErrorCoral else ElectricBlue,
                        contentColor = Color.White,
                        disabledContainerColor = SurfaceDim.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(12.dp),
                        ambientColor = if (isDangerous) ErrorCoral.copy(alpha = 0.3f) else ElectricBlue.copy(alpha = 0.4f),
                        spotColor = if (isDangerous) ErrorCoral.copy(alpha = 0.3f) else LuminousAzure.copy(alpha = 0.4f)
                    )
                ) {
                    Text(
                        text = confirmText,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        },
        dismissButton = {
            if (onDismiss != null) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = TextSecondaryIce
                    )
                ) {
                    Text(
                        text = dismissText,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }
    )
}

/**
 * Unified Input Dialog - For text input with consistent styling
 */
@Composable
fun UnifiedInputDialog(
    onDismissRequest: () -> Unit,
    title: String,
    icon: ImageVector? = null,
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    confirmText: String = "SAVE",
    dismissText: String = "CANCEL",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isError: Boolean = false,
    supportingText: String? = null
) {
    UnifiedDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        icon = icon,
        confirmText = confirmText,
        dismissText = dismissText,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        confirmEnabled = value.isNotBlank(),
        content = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label) },
                placeholder = { Text(placeholder) },
                singleLine = true,
                isError = isError,
                supportingText = supportingText?.let { { Text(it) } },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricBlue,
                    focusedLabelColor = ElectricBlue,
                    cursorColor = ElectricBlue,
                    errorBorderColor = ErrorCoral,
                    errorLabelColor = ErrorCoral,
                    unfocusedBorderColor = GlassBorderAzure,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = GlassBase.copy(alpha = 0.3f),
                    focusedTextColor = TextPrimaryIce,
                    unfocusedTextColor = TextPrimaryIce
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

/**
 * Unified Confirmation Dialog - For simple yes/no confirmations
 */
@Composable
fun UnifiedConfirmDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    icon: ImageVector? = Icons.Default.Warning,
    confirmText: String = "CONFIRM",
    dismissText: String = "CANCEL",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDangerous: Boolean = false
) {
    UnifiedDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        icon = icon,
        confirmText = confirmText,
        dismissText = dismissText,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        isDangerous = isDangerous,
        content = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondaryIce
            )
        }
    )
}
