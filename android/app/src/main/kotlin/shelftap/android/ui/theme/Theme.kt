package shelftap.android.ui.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import shelftap.android.data.ThemeMode

/** Light or dark follows the phone unless the member chose one in Account (stored on the device, see [ThemeMode]). */
@Composable
fun ShelfTapTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkColors else LightColors
    CompositionLocalProvider(LocalShelfTapColors provides colors) {
        MaterialTheme(colorScheme = colors.toColorScheme(dark), typography = ShelfTapTypography) {
            // Every clickable, selectable and Material button picks this up from here instead of the ripple Material installs.
            val tier = LocalEffectTier.current
            val indication = remember(tier, colors.ink) { SoftPressIndication(tier, colors.ink) }
            CompositionLocalProvider(LocalIndication provides indication) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, content = content)
            }
        }
    }
}

/** The state colours that Material 3 has no role for. */
val MaterialTheme.shelfTap: ShelfTapColors
    @Composable get() = LocalShelfTapColors.current

/** Off: outlined track with a mid-grey knob; on: the green `switchOn` token. Both clear 3:1 against the page (plan §5.3). */
@Composable
fun shelfTapSwitchColors(): SwitchColors {
    val c = MaterialTheme.shelfTap
    return SwitchDefaults.colors(
        checkedTrackColor = c.switchOn, checkedThumbColor = c.switchKnobOn, checkedBorderColor = c.switchOn,
        uncheckedTrackColor = c.bg, uncheckedThumbColor = c.ink3, uncheckedBorderColor = c.ink3,
    )
}
