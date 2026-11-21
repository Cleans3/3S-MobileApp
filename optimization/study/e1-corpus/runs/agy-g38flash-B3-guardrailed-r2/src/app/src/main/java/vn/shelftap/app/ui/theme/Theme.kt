package vn.shelftap.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = OnBluePrimary,
    primaryContainer = BluePrimaryContainer,
    onPrimaryContainer = OnBluePrimaryContainer,
    secondary = SecondaryBlue,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    error = RedError,
    errorContainer = RedErrorContainer,
    onErrorContainer = OnRedErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimaryContainer,
    onPrimary = OnBluePrimaryContainer,
    primaryContainer = BluePrimary,
    onPrimaryContainer = OnBluePrimary,
    secondary = SecondaryBlue,
    error = RedErrorContainer,
    errorContainer = RedError,
    onErrorContainer = RedErrorContainer
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
