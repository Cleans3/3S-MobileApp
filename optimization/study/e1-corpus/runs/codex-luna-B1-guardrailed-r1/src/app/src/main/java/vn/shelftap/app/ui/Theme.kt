package vn.shelftap.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ShelfColors = lightColorScheme(
    primary = Color(0xFF315D43), onPrimary = Color.White,
    secondary = Color(0xFF53634F), background = Color(0xFFF9FBF5), surface = Color(0xFFF9FBF5)
)

@Composable
fun ShelfTapTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ShelfColors, typography = Typography(), content = content)
}
