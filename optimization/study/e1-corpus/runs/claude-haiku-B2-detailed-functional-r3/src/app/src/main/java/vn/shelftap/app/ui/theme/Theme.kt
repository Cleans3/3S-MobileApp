package vn.shelftap.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B5E4F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBFEBDD),
    onPrimaryContainer = Color(0xFF002019),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FD3C2),
    onPrimary = Color(0xFF003729),
    primaryContainer = Color(0xFF1B5E4F),
    onPrimaryContainer = Color(0xFFBFEBDD),
)

@Composable
fun ShelfTapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
