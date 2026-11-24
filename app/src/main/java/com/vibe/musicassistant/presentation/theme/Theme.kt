package com.vibe.musicassistant.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VibeColorScheme = darkColorScheme(
    primary = VibeElectricBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1B2A6B),
    onPrimaryContainer = VibeTextPrimary,
    secondary = VibeViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2E1F5E),
    onSecondaryContainer = VibeTextPrimary,
    tertiary = VibeCyan,
    onTertiary = VibeNavyDeep,
    background = VibeNavyDeep,
    onBackground = VibeTextPrimary,
    surface = VibeNavy,
    onSurface = VibeTextPrimary,
    surfaceVariant = VibeNavySurface,
    onSurfaceVariant = VibeTextSecondary,
    outline = VibeNavyOutline,
    error = VibeError,
    onError = VibeNavyDeep,
)

/**
 * VIBE is dark-first by design: the same navy scheme is used whatever the system
 * setting, and dynamic color is off so the brand palette always shows.
 */
@Composable
fun VibeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VibeColorScheme,
        typography = VibeTypography,
        content = content,
    )
}
