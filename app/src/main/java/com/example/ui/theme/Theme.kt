package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val NeomorphicLightColorScheme = lightColorScheme(
    primary = StudioCyan,
    onPrimary = NeoShadowLight,
    primaryContainer = NeoSurfaceElevated,
    onPrimaryContainer = StudioCyan,
    secondary = StudioAmber,
    onSecondary = NeoShadowLight,
    secondaryContainer = NeoSurfaceInset,
    onSecondaryContainer = StudioAmber,
    tertiary = StudioEmerald,
    background = NeoBackground,
    onBackground = NeoTextPrimary,
    surface = NeoSurface,
    onSurface = NeoTextPrimary,
    surfaceVariant = NeoSurfaceInset,
    onSurfaceVariant = NeoTextSecondary,
    outline = NeoBorder,
    outlineVariant = NeoDivider
)

@Composable
fun AnimForgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NeomorphicLightColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun Sk8aniTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    AnimForgeTheme(darkTheme, dynamicColor, content)
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    AnimForgeTheme(darkTheme, dynamicColor, content)
}

