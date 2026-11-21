package vn.shelftap.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = OnPrimaryWhite,
    primaryContainer = PrimaryBlueContainer,
    onPrimaryContainer = OnPrimaryBlueContainer,
    secondary = SecondarySlate,
    onSecondary = OnSecondaryWhite,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    error = ErrorRed,
    onError = OnErrorWhite,
    errorContainer = ErrorContainerRed,
    onErrorContainer = OnErrorContainerRed,
    outline = OutlineLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueContainer,
    onPrimary = OnPrimaryBlueContainer,
    primaryContainer = PrimaryBlue,
    onPrimaryContainer = PrimaryBlueContainer,
    secondary = SecondarySlate,
    onSecondary = OnSecondaryWhite,
    background = OnBackgroundLight,
    onBackground = BackgroundLight,
    surface = OnBackgroundLight,
    onSurface = BackgroundLight,
    error = ErrorContainerRed,
    onError = OnErrorContainerRed,
    outline = OutlineLight
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
