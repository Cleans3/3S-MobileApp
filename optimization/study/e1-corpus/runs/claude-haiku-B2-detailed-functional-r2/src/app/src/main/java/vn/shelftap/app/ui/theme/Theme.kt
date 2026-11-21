package vn.shelftap.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Teal40 = Color(0xFF00695C)
private val Teal80 = Color(0xFF80CBC4)

private val LightColors = lightColorScheme(primary = Teal40)
private val DarkColors = darkColorScheme(primary = Teal80)

@Composable
fun ShelfTapTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
