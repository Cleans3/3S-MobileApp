package shelftap.android.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.dp

/**
 * Motion tokens (plan §6.5). Rules: animate only transform and opacity (read in `graphicsLayer { }` or `drawScope`, so
 * nothing recomposes per frame); a "done" effect only after the server answered; no motion at all on the [EffectTier.LIGHT] tier,
 * which is also what a system animation scale of 0 selects.
 */
object Motion {
    const val GLYPH_MS = 260
    const val MENU_MS = 260
    const val SCREEN_MS = 300
    const val FADE_IN_MS = 220
    const val FADE_OUT_MS = 90
    const val SHORT_FADE_MS = 150

    /** Close to `cubic-bezier(0.2, 0.8, 0.2, 1)` in the mockup: fast start, soft landing. */
    val decelerate = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

    /** How far a screen slides when going one level deeper (list to detail) and back. */
    val slideDistance = 30.dp

    /** A tween on the full and medium tiers, an instant change on the light tier. */
    fun <T> spec(tier: EffectTier, durationMs: Int, delayMs: Int = 0): FiniteAnimationSpec<T> =
        if (tier == EffectTier.LIGHT) snap() else tween(durationMs, delayMs, decelerate)
}
