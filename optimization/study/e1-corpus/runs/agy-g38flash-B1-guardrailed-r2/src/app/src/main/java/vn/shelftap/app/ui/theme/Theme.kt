package vn.shelftap.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = ForestGreen,
    onPrimary = Color.White,
    primaryContainer = LightMint,
    onPrimaryContainer = DarkMint,
    background = SoftBackground,
    onBackground = TextPrimary,
    surface = CardBackground,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF0F2F4),
    onSurfaceVariant = TextSecondary
)

private val DarkColorScheme = darkColorScheme(
    primary = LightMint,
    onPrimary = DarkMint,
    primaryContainer = ForestGreen,
    onPrimaryContainer = Color.White,
    background = Color(0xFF121414),
    onBackground = Color(0xFFE2E3E3),
    surface = Color(0xFF1E2121),
    onSurface = Color(0xFFE2E3E3),
    surfaceVariant = Color(0xFF2A2E2E),
    onSurfaceVariant = Color(0xFFB9BCBC)
)

@Composable
fun ShelfTapTheme(
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
