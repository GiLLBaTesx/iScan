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

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = SurfaceWhite,
    primaryContainer = LightBlue,
    onPrimaryContainer = PrimaryBlueDark,
    secondary = AccentBlue,
    onSecondary = SurfaceWhite,
    secondaryContainer = SkyBlue,
    onSecondaryContainer = PrimaryBlueDark,
    tertiary = PrimaryBlueLight,
    onTertiary = SurfaceWhite,
    background = BackgroundWhite,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = GlassWhite,
    onSurfaceVariant = TextSecondary,
    outline = LightGray,
    error = ErrorRed,
    onError = SurfaceWhite
)

private val DarkColorScheme = darkColorScheme(
    primary = CoralPrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = CoralDark,
    onPrimaryContainer = androidx.compose.ui.graphics.Color.White,
    secondary = DarkInfoBlue,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = DarkCardElevated,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = LiveSyncGreen,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    error = DarkErrorRed,
    onError = androidx.compose.ui.graphics.Color.White
)

@Composable
fun ExamScannerTheme(
    darkTheme: Boolean = true, // Force dark theme by default
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = if (darkTheme) DarkBackground.toArgb() else BackgroundWhite.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
