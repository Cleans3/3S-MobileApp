package shelftap.android.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/** How far a pressed button or card shrinks, and a full-width row (plan section 6.5: 0.96 and 0.98). */
const val PRESS_SCALE_CARD = 0.96f
const val PRESS_SCALE_ROW = 0.98f

/**
 * The control shrinks a little while pressed (120 ms). Not on the light tier. Put it OUTSIDE the control's clip.
 * It is a draw-only modifier node: no composition state, no layer of its own while the control rests, one small coroutine that
 * follows the presses. Lists are full of pressable rows and cards, so what each one costs while scrolling matters; the first version
 * (a `remember`ed animation, a `LaunchedEffect` and a `graphicsLayer` per item) measured slower on the phone.
 */
@Composable
fun Modifier.pressScale(source: InteractionSource, target: Float = PRESS_SCALE_CARD): Modifier {
    val tier = LocalEffectTier.current
    if (tier == EffectTier.LIGHT) return this
    return this.then(PressScaleElement(source, target))
}

private data class PressScaleElement(val source: InteractionSource, val target: Float) : ModifierNodeElement<PressScaleNode>() {
    override fun create() = PressScaleNode(source, target)
    override fun update(node: PressScaleNode) {
        node.target = target
    }
}

private class PressScaleNode(private val source: InteractionSource, var target: Float) : Modifier.Node(), DrawModifierNode {
    private val scale = Animatable(1f)

    override fun onAttach() {
        coroutineScope.launch {
            source.interactions.collectLatest { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> scale.animateTo(target, tween(Motion.PRESS_MS))
                    is PressInteraction.Release, is PressInteraction.Cancel -> scale.animateTo(1f, tween(Motion.PRESS_MS))
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val s = scale.value
        if (s == 1f) drawContent() else scale(s, s, pivot = center) { this@draw.drawContent() }
    }
}

/** Rows that rise in on the first load, at most this many (plan section 6.5). */
const val RISE_IN_ROWS = 8

/** Pause between one row and the next, in milliseconds. */
private const val RISE_STEP_MS = 40

/**
 * Rises into place and fades in, row after row. Only while [enabled] (the first load, the first [RISE_IN_ROWS] rows):
 * a row that appears later, for example when scrolling back, is drawn in place. No effect on the light tier.
 * Offset and alpha are read in the draw phase, so the list does not recompose per frame.
 */
@Composable
fun Modifier.riseIn(index: Int, enabled: Boolean): Modifier {
    val tier = LocalEffectTier.current
    val active = enabled && tier != EffectTier.LIGHT && index < RISE_IN_ROWS
    val progress = remember { Animatable(if (active) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (active) {
            delay((RISE_STEP_MS * index).toLong())
            progress.animateTo(1f, Motion.spec(tier, Motion.RISE_MS))
        }
    }
    if (progress.value == 1f && !active) return this
    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * RISE_DISTANCE_DP * density
    }
}

/** 16 dp: the distance a row rises, multiplied by the screen density when drawn. */
private const val RISE_DISTANCE_DP = 16f

/** How long after a list first shows its rows may still rise in: the eight rows plus their pauses. */
private const val RISE_WINDOW_MS = 900L

/** True while a list that has just appeared should let its first rows rise in; false afterwards and after a rotation. */
@Composable
fun rememberRiseIn(): Boolean {
    var done by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(RISE_WINDOW_MS)
        done = true
    }
    return !done
}

/** Rows that change (a loan renewed, a reservation cancelled) move the others into place; not on the light tier. */
@Composable
fun androidx.compose.foundation.lazy.LazyItemScope.itemMotion(): Modifier =
    if (LocalEffectTier.current == EffectTier.LIGHT) Modifier else Modifier.animateItem()
