package vn.shelftap.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun ShelfTapTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary = Color(0xFFA5D6A7),
            onPrimary = Color(0xFF003910),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF1B5E20),
            onPrimary = Color.White,
        )
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
