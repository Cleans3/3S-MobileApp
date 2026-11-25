package shelftap.android.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import shelftap.android.ui.theme.EffectTier
import shelftap.android.ui.theme.GlassSize
import shelftap.android.ui.theme.LocalEffectTier
import shelftap.android.ui.theme.shelfTap

/**
 * Whether the bands under the bars are glass: the page behind the bar is blurred, strongest at the screen's edge and fading out inwards (the
 * boards' progressive blur under the top bar and under the floating button). Only on the full tier (Android 17 and later); the medium and
 * light tiers keep the plain fade of [FadeBand] (plan section 7).
 */
@Composable
fun glassBands(): Boolean = LocalEffectTier.current == EffectTier.FULL && android.os.Build.VERSION.SDK_INT >= 31

/**
 * A band of glass over the page's edge, made with Haze (`hazeSource` on the page, `hazeBlur` here): the page is drawn again, blurred, and the
 * blur fades out over [height] (full at the screen's edge, none at the other end), with a light veil of the page colour. Draws only, never takes a touch.
 * Haze is used because drawing the page again by hand, as the menu does, crashed the renderer while the page was scrolling.
 */
@Composable
fun GlassBand(haze: HazeState, height: Dp, fromTop: Boolean, modifier: Modifier = Modifier) {
    val bg = MaterialTheme.shelfTap.bg
    val style = remember(bg, fromTop) {
        HazeBlurStyle {
            blurRadius(GlassSize.bandBlur)
            colorEffects(listOf(HazeColorEffect.tint(bg.copy(alpha = VEIL))))
            progressive(
                if (fromTop) HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f)
                else HazeProgressive.verticalGradient(startIntensity = 0f, endIntensity = 1f),
            )
        }
    }
    Box(modifier.fillMaxWidth().height(height).hazeBlur(input = HazeInput.Sources(haze), style = style))
}

private const val VEIL = 0.35f // the page colour over the blurred page (the plain fade is 0.6)
