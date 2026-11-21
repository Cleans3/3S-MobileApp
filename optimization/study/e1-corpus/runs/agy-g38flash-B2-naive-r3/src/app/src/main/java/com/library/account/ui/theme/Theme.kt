package com.library.account.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ForestGreenLight,
    onPrimary = Color.White,
    primaryContainer = ForestGreenDark,
    secondary = LibraryGoldLight,
    background = Color(0xFF121412),
    surface = Color(0xFF1B1E1B),
    onSurface = Color(0xFFE2E4DE),
    error = Color(0xFFCF6679)
)

private val LightColorScheme = lightColorScheme(
    primary = ForestGreen,
    onPrimary = Color.White,
    primaryContainer = ForestGreenLight,
    secondary = LibraryGold,
    background = WarmPaperBackground,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    error = ErrorRed
)

@Composable
fun LibraryAccountTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
