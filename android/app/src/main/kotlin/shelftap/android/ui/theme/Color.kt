package shelftap.android.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Every colour the app uses, once for the light theme and once for the dark one. The values mirror the tokens
 * `TOK_L` and `TOK_D` in `optimization/ui-ux-mockup/tools/wide-boards/shell.js` (plan §5.1). No screen writes a colour:
 * it reads [MaterialTheme.colorScheme] or [LocalShelfTapColors]. State colours (ok, warn, danger) are always shown with
 * text or an icon as well, never as colour alone.
 */
@Immutable
class ShelfTapColors(
    val bg: Color,
    val bg2: Color,
    val surface: Color,
    val dialog: Color,
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    val primary: Color,
    val onPrimary: Color,
    val ok: Color,
    val danger: Color,
    val warn: Color,
    val warnBg: Color,
    val dangerBg: Color,
    val switchOn: Color,
    val switchKnobOn: Color,
)

val LightColors = ShelfTapColors(
    bg = Color(0xFFECE7E1), bg2 = Color(0xFFD9D1C6), surface = Color(0xFFFFFFFF), dialog = Color(0xFFF4F1EC),
    ink = Color(0xFF1C1B1A), ink2 = Color(0xFF3D3833), ink3 = Color(0xFF5C554E),
    primary = Color(0xFF1C1B1A), onPrimary = Color(0xFFFFFFFF),
    ok = Color(0xFF2F5B3A), danger = Color(0xFF7A3420), warn = Color(0xFF6B4A10),
    warnBg = Color(0xFFFFF4DC), dangerBg = Color(0xFFF6E4DC),
    switchOn = Color(0xFF2F5B3A), switchKnobOn = Color(0xFFFFFFFF),
)

/** In the dark theme the primary button swaps to a light fill with dark text, instead of a black button on a black page. */
val DarkColors = ShelfTapColors(
    bg = Color(0xFF151412), bg2 = Color(0xFF1F1C19), surface = Color(0xFF2A2723), dialog = Color(0xFF2A2723),
    ink = Color(0xFFF2EEE9), ink2 = Color(0xFFD3CBC1), ink3 = Color(0xFFA99F94),
    primary = Color(0xFFF2EEE9), onPrimary = Color(0xFF1C1B1A),
    ok = Color(0xFF9FD3AC), danger = Color(0xFFF0A58E), warn = Color(0xFFF0C878),
    warnBg = Color(0xFF3A2E14), dangerBg = Color(0xFF3B221A),
    switchOn = Color(0xFF9FD3AC), switchKnobOn = Color(0xFF1C1B1A),
)

val LocalShelfTapColors = staticCompositionLocalOf { LightColors }

/** Maps the tokens onto Material 3 roles so stock components pick them up. Roles with no token reuse the nearest one. */
fun ShelfTapColors.toColorScheme(dark: Boolean): ColorScheme {
    val scrim = Color.Black
    return if (dark) {
        darkColorScheme(
            primary = primary, onPrimary = onPrimary, primaryContainer = bg2, onPrimaryContainer = ink,
            secondary = ink2, onSecondary = bg, secondaryContainer = bg2, onSecondaryContainer = ink,
            tertiary = ok, onTertiary = bg, tertiaryContainer = bg2, onTertiaryContainer = ok,
            error = danger, onError = bg, errorContainer = dangerBg, onErrorContainer = danger,
            background = bg, onBackground = ink, surface = bg, onSurface = ink, surfaceVariant = bg2, onSurfaceVariant = ink2,
            surfaceTint = primary, inverseSurface = ink, inverseOnSurface = bg, inversePrimary = bg,
            outline = ink3, outlineVariant = ink.copy(alpha = 0.2f), scrim = scrim,
            surfaceBright = surface, surfaceDim = bg, surfaceContainerLowest = bg, surfaceContainerLow = bg,
            surfaceContainer = surface, surfaceContainerHigh = dialog, surfaceContainerHighest = dialog,
        )
    } else {
        lightColorScheme(
            primary = primary, onPrimary = onPrimary, primaryContainer = bg2, onPrimaryContainer = ink,
            secondary = ink2, onSecondary = surface, secondaryContainer = bg2, onSecondaryContainer = ink,
            tertiary = ok, onTertiary = surface, tertiaryContainer = bg2, onTertiaryContainer = ok,
            error = danger, onError = surface, errorContainer = dangerBg, onErrorContainer = danger,
            background = bg, onBackground = ink, surface = bg, onSurface = ink, surfaceVariant = bg2, onSurfaceVariant = ink2,
            surfaceTint = primary, inverseSurface = ink, inverseOnSurface = bg, inversePrimary = bg,
            outline = ink3, outlineVariant = ink.copy(alpha = 0.2f), scrim = scrim,
            surfaceBright = surface, surfaceDim = bg2, surfaceContainerLowest = surface, surfaceContainerLow = bg,
            surfaceContainer = surface, surfaceContainerHigh = dialog, surfaceContainerHighest = dialog,
        )
    }
}
