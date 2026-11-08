package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ClassicDarkColorScheme = darkColorScheme(
    primary = ClassicRed,
    onPrimary = TextPrimary,
    primaryContainer = SurfaceCardHigh,
    onPrimaryContainer = ClassicRedGlow,
    secondary = TextSecondary,
    onSecondary = BackgroundDark,
    secondaryContainer = SurfaceDark,
    onSecondaryContainer = TextPrimary,
    tertiary = ClassicRedDark,
    onTertiary = TextPrimary,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCardHigh,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    error = ErrorRed,
    onError = TextPrimary
)

@Composable
fun NutriTrackTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = ClassicDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        // System UI colors are set via MaterialTheme colors; no manual Window
        // flags needed here to avoid Java deprecation warnings.
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
