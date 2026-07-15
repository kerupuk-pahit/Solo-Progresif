package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    tertiary = DarkTertiary,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = SilverText,
    onSurface = SilverText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = SilverText,
    error = Color(0xFFFF3D00)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark Theme for RPG "System" feeling
    dynamicColor: Boolean = false, // Disable to preserve custom aesthetic
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
