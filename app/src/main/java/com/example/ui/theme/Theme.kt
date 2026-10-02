package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NavyDarkPrimary,
    onPrimary = Color(0xFF041E36),
    primaryContainer = NavyDarkPrimaryContainer,
    onPrimaryContainer = Color(0xFFD6E6F9),
    secondary = TealDarkSecondary,
    onSecondary = Color(0xFF022C2B),
    secondaryContainer = Color(0xFF115E59),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = AmberDarkTertiary,
    onTertiary = Color(0xFF3B1902),
    background = SurfaceDark,
    onBackground = Color(0xFFF1F5F9),
    surface = CardSurfaceDark,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1)
)

private val LightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = Color.White,
    primaryContainer = NavyPrimaryContainer,
    onPrimaryContainer = OnNavyPrimaryContainer,
    secondary = TealSecondary,
    onSecondary = Color.White,
    secondaryContainer = TealSecondaryContainer,
    onSecondaryContainer = OnTealSecondaryContainer,
    tertiary = AmberTertiary,
    onTertiary = Color.White,
    tertiaryContainer = AmberTertiaryContainer,
    onTertiaryContainer = OnAmberTertiaryContainer,
    background = SurfaceLight,
    onBackground = Color(0xFF0F172A),
    surface = CardSurfaceLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569)
)

@Composable
fun MyApplicationTheme(
    themeMode: String = "SYSTEM",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode.uppercase()) {
        "DARK" -> true
        "LIGHT" -> false
        else -> systemDark
    }
    val colorScheme = if (useDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
