package shelftap.android.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlin.math.PI
import kotlin.math.sin
import shelftap.android.ui.icons.LucideIcon
import shelftap.android.ui.icons.LucideIcons
import shelftap.android.ui.icons.LucideMorphs
import shelftap.android.ui.icons.MorphIcon
import shelftap.android.ui.theme.EffectTier
import shelftap.android.ui.theme.IconSize
import shelftap.android.ui.theme.LocalEffectTier
import shelftap.android.ui.theme.Motion

private const val RING_CYCLE_MS = 1800
private const val BREATH_MS = 2400
private const val RING_START_SCALE = 0.5f
private const val RING_END_SCALE = 1f
private const val BREATH_PEAK = 1.06f
private const val POP_START = 0.6f
private const val POP_MS = 380
private const val SHAKE_MS = 360
private const val SHAKE_WOBBLES = 3
private const val SHAKE_DISTANCE_DP = 12f

/**
 * The picture on the self-checkout screen: the tag icon with two rings spreading out while the phone waits for a tag;
 * after the server confirmed the loan, the icon becomes a tick that pops in; after a refusal, the alert icon shakes.
 * Rings and breathing run only while waiting (the loop ends when the state changes or the screen leaves), and are still on the
 * light tier. Everything is read in the draw phase.
 */
@Composable
fun NfcStage(waiting: Boolean, done: Boolean, failed: Boolean) {
    val tier = LocalEffectTier.current
    val animate = tier != EffectTier.LIGHT
    val color = MaterialTheme.colorScheme.onSurface

    val pop = remember { Animatable(1f) }
    LaunchedEffect(done) {
        if (done && animate) {
            pop.snapTo(POP_START)
            pop.animateTo(1f, Motion.spec(tier, POP_MS))
        }
    }
    val shake = remember { Animatable(1f) }
    LaunchedEffect(failed) {
        if (failed && animate) {
            shake.snapTo(0f)
            shake.animateTo(1f, tween(SHAKE_MS, easing = LinearEasing))
        }
    }

    val loop = if (waiting && animate) rememberInfiniteTransition(label = "nfc") else null
    val ring1 = loop?.animateFloat(0f, 1f, infiniteRepeatable(tween(RING_CYCLE_MS, easing = LinearEasing)), label = "ring1")
    // The second ring is the first, half a cycle later.
    val breath = loop?.animateFloat(
        1f, BREATH_PEAK, infiniteRepeatable(tween(BREATH_MS / 2, easing = Motion.decelerate), RepeatMode.Reverse), label = "breath",
    )

    Box(Modifier.size(IconSize.hero * 2).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        if (ring1 != null) {
            Canvas(Modifier.size(IconSize.hero * 2)) {
                val maxRadius = size.minDimension / 2
                for (k in 0..1) {
                    val t = (ring1.value + k * 0.5f) % 1f
                    val scale = RING_START_SCALE + (RING_END_SCALE - RING_START_SCALE) * t
                    drawCircle(color.copy(alpha = RING_ALPHA * (1f - t)), radius = maxRadius * scale, style = Stroke(RING_STROKE_PX * density))
                }
            }
        }
        Box(
            Modifier.graphicsLayer {
                val s = pop.value * (breath?.value ?: 1f)
                scaleX = s
                scaleY = s
                translationX = sin(shake.value * SHAKE_WOBBLES * 2 * PI.toFloat()) * (1f - shake.value) * SHAKE_DISTANCE_DP * density
            },
        ) {
            if (failed) {
                LucideIcon(LucideIcons.CircleAlert, tint = MaterialTheme.colorScheme.error, size = IconSize.hero)
            } else {
                MorphIcon(LucideMorphs.NfcToCheck, toEnd = done, size = IconSize.hero)
            }
        }
    }
}

private const val RING_ALPHA = 0.35f
private const val RING_STROKE_PX = 2f
