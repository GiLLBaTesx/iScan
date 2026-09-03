package com.examscanner.premium.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.examscanner.premium.ui.theme.GlassBorder
import com.examscanner.premium.ui.theme.GlassOverlay

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    borderWidth: Dp = 0.5.dp,
    backgroundColor: Color = GlassOverlay,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .border(
                width = borderWidth,
                color = GlassBorder,
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        content()
    }
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = GlassOverlay,
    content: @Composable () -> Unit
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassBorder),
        shadowElevation = 4.dp
    ) {
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            content()
        }
    }
}

@Composable
fun FloatingGlassCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 8.dp,
    cornerRadius: Dp = 24.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.material3.Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = Color.White.copy(alpha = 0.95f),
        shadowElevation = elevation,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassBorder)
    ) {
        content()
    }
}

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    withGradient: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color(0xFF007AFF).copy(alpha = 0.1f),
                spotColor = Color(0xFF007AFF).copy(alpha = 0.15f)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                if (withGradient) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.98f),
                            Color.White.copy(alpha = 0.92f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.95f),
                            Color.White.copy(alpha = 0.95f)
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.8f),
                        Color.White.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        content()
    }
}

@Composable
fun PrimaryActionCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val alpha by animateFloatAsState(if (enabled) 1f else 0.6f, label = "alpha")
    
    Box(
        modifier = modifier
            .shadow(
                elevation = if (enabled) 16.dp else 4.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color(0xFF007AFF).copy(alpha = 0.2f),
                spotColor = Color(0xFF007AFF).copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF007AFF).copy(alpha = alpha),
                        Color(0xFF0051D5).copy(alpha = alpha)
                    )
                )
            )
    ) {
        content()
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF007AFF),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = accentColor.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.98f),
                        Color.White.copy(alpha = 0.95f)
                    )
                )
            )
            .border(
                width = 0.5.dp,
                color = accentColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        content()
    }
}
