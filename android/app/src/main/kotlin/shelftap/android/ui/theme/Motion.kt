package shelftap.android.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween

/**
 * Motion tokens (plan §6.5). Rules: animate only transform and opacity (read in `graphicsLayer { }` or `drawScope`, so
 * nothing recomposes per frame); a "done" effect only after the server answered; no motion at all on the [EffectTier.LIGHT] tier,
 * which is also what a system animation scale of 0 selects.
 */
object Motion {
    const val GLYPH_MS = 260
    const val MENU_MS = 260
    const val SCREEN_MS = 260
    const val FADE_IN_MS = 220
    const val FADE_OUT_MS = 90
    const val SHORT_FADE_MS = 150
    const val PRESS_MS = 120
    const val RISE_MS = 320
    const val MENU_EXIT_MS = 140

    /** The menu panel itself comes and goes faster than the page's blur and dimming around it (a half-faded glass panel is a pale rectangle). */
    const val PANEL_IN_MS = 140
    const val PANEL_OUT_MS = 90
    const val PULSE_MS = 1200

    /** The start screen stays at least this long on a cold start, so its entrance (pop 380 ms, rise 80 + 420 ms) is seen to the end. */
    const val SPLASH_MIN_MS = 700L

    /** Close to `cubic-bezier(0.2, 0.8, 0.2, 1)` in the mockup: fast start, soft landing. */
    val decelerate = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

    /**
     * The spec of something that is leaving: even speed, no long soft tail. (With [decelerate] the last third of a leaving blur or dimming
     * takes as long as the first two, and it lingers after the panel it belonged to is gone.) An instant change on the light tier.
     */
    fun <T> exitSpec(tier: EffectTier, durationMs: Int): FiniteAnimationSpec<T> =
        if (tier == EffectTier.LIGHT) snap() else tween(durationMs, easing = androidx.compose.animation.core.LinearEasing)

    /** A tween on the full and medium tiers, an instant change on the light tier. */
    fun <T> spec(tier: EffectTier, durationMs: Int, delayMs: Int = 0): FiniteAnimationSpec<T> =
        if (tier == EffectTier.LIGHT) snap() else tween(durationMs, delayMs, decelerate)
}
