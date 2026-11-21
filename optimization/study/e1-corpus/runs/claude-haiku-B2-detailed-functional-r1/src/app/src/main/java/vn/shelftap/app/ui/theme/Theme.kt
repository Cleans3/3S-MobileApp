package vn.shelftap.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B4D3E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBFEBD6),
    onPrimaryContainer = Color(0xFF002116),
    secondary = Color(0xFF8A5F1E),
    onSecondary = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA3D5BA),
    onPrimary = Color(0xFF003826),
    primaryContainer = Color(0xFF05513A),
    onPrimaryContainer = Color(0xFFBFEBD6),
    secondary = Color(0xFFE7BE83),
    onSecondary = Color(0xFF442B00),
)

@Composable
fun ShelfTapTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
