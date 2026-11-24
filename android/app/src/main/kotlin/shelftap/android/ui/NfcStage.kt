package shelftap.android.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import shelftap.android.ui.icons.LucideIcon
import shelftap.android.ui.icons.LucideIcons
import shelftap.android.ui.theme.EffectTier
import shelftap.android.ui.theme.IconSize
import shelftap.android.ui.theme.LocalEffectTier
import shelftap.android.ui.theme.Motion
import shelftap.android.ui.theme.NfcSize
import shelftap.android.ui.theme.Radius
import shelftap.android.ui.theme.okFill
import shelftap.android.ui.theme.shelfTap

private const val RING_CYCLE_MS = 1800
private const val BREATH_MS = 2400
private const val RING_END_SCALE = 1.35f // the rings spread from the disc's edge to 135% of it, fading (the board's st-ping)
private const val BREATH_PEAK = 1.04f
private const val RING_ALPHA = 0.55f
private const val RING_STROKE_DP = 2f
private const val DISC_FILL = 0.4f
private const val DISC_INNER_FILL = 0.75f
private const val DISC_EDGE = 0.9f
private const val POP_START = 0.6f
private const val POP_OVERSHOOT = 1.06f
private const val POP_MS = 380

/** The teal of the rings (a fixed colour in the board). */
private val RING_COLOR = Color(0xFF2F4A44)

/**
 * The picture while the phone waits for a tag, built to the board `Checkout`: a glass disc with a smaller one inside that breathes, the
 * tag icon on it, and two rings spreading out from the disc's edge. The loop ends when the state changes or the screen leaves; on the light
 * tier nothing moves. Everything animated is read in the draw phase. Decorative: the words under it say what to do.
 */
@Composable
fun NfcStage() {
    val tier = LocalEffectTier.current
    val c = MaterialTheme.shelfTap
    val loop = if (tier != EffectTier.LIGHT) rememberInfiniteTransition(label = "nfc") else null
    val phase = loop?.animateFloat(0f, 1f, infiniteRepeatable(tween(RING_CYCLE_MS, easing = LinearEasing)), label = "ring")
    val breath = loop?.animateFloat(1f, BREATH_PEAK, infiniteRepeatable(tween(BREATH_MS / 2, easing = Motion.decelerate), RepeatMode.Reverse), label = "breath")
    Box(Modifier.size(NfcSize.disc).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().clip(CircleShape).background(c.glass.copy(alpha = DISC_FILL)).border(BorderStroke(Radius.hairline, c.edge.copy(alpha = DISC_EDGE)), CircleShape))
        if (phase != null) {
            Canvas(Modifier.fillMaxSize()) {
                val edge = size.minDimension / 2
                for (k in 0..1) { // the second ring is the first, half a cycle later (the board delays it by .9 s)
                    val t = (phase.value + k * 0.5f) % 1f
                    drawCircle(
                        RING_COLOR.copy(alpha = RING_ALPHA * (1f - t) * 0.73f), radius = edge * (1f + (RING_END_SCALE - 1f) * t),
                        style = Stroke(RING_STROKE_DP * density),
                    )
                }
            }
        }
        Box(
            Modifier.size(NfcSize.inner).graphicsLayer { val s = breath?.value ?: 1f; scaleX = s; scaleY = s }
                .clip(CircleShape).background(c.glass.copy(alpha = DISC_INNER_FILL)),
            contentAlignment = Alignment.Center,
        ) { LucideIcon(LucideIcons.MockupNfc, tint = c.ink, size = IconSize.hero) }
    }
}

/** The green circle with the tick on a loan that the server confirmed: it pops in (a little past its size, then settles). */
@Composable
fun DoneBadge() {
    val tier = LocalEffectTier.current
    val pop = remember { Animatable(if (tier == EffectTier.LIGHT) 1f else POP_START) }
    LaunchedEffect(Unit) {
        if (tier != EffectTier.LIGHT) {
            pop.animateTo(POP_OVERSHOOT, tween(POP_MS * 7 / 10, easing = Motion.decelerate))
            pop.animateTo(1f, tween(POP_MS * 3 / 10, easing = Motion.decelerate))
        }
    }
    Box(
        Modifier.size(NfcSize.badge).graphicsLayer { val s = pop.value; scaleX = s; scaleY = s; alpha = ((s - POP_START) / (1f - POP_START)).coerceIn(0f, 1f) }
            .clip(CircleShape).background(okFill).clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) { LucideIcon(LucideIcons.Check, tint = Color.White, size = NfcSize.badgeIcon) }
}

