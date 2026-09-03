package com.examscanner.premium.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Novelty Azure Glass - Light Ice Theme
private val AzureGlassColorScheme = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = FrostedWhite,
    primaryContainer = IcyCyan,
    onPrimaryContainer = DeepUltramarine,
    secondary = IcyCyan,
    onSecondary = FrostedWhite,
    secondaryContainer = CyanSpecular,
    onSecondaryContainer = DeepUltramarine,
    tertiary = LuminousAzure,
    onTertiary = FrostedWhite,
    background = IceWhite,
    onBackground = TextPrimaryIce,
    surface = FrostedWhite,
    onSurface = TextPrimaryIce,
    surfaceVariant = GlassFloating,
    onSurfaceVariant = TextSecondaryIce,
    outline = TextTertiaryIce,
    error = ErrorCoral,
    onError = FrostedWhite
)

@Composable
fun ExamScannerTheme(
    darkTheme: Boolean = false, // Use light Azure Glass theme
    content: @Composable () -> Unit
) {
    val colorScheme = AzureGlassColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = IceWhite.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
