package shelftap.android.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** How strong the veil over a pressed control is: a soft 8% of the ink colour (the mockup has no veil at all, only the shrink). */
private const val PRESS_VEIL = 0.08f

/** The veil comes in fast and goes out slowly, and stays at least as long as it takes to come in, so a quick tap never just blinks. */
private const val VEIL_IN_MS = 100
private const val VEIL_OUT_MS = 180

private val FOCUS_RING = 2.dp

/**
 * The press feedback of the whole app, in place of Material's ripple (a hard grey flash that is square unless every caller clips it):
 * a veil of [ink] over the control that fades in over 100 ms, holds while it is pressed, and fades out over 180 ms; and a keyboard or
 * D-pad focus ring. The shrink of a pressed control is [pressScale], which has to sit outside the control's clip, so it is not done here.
 * Drawn only in the draw phase: nothing recomposes when a control is pressed. On the light tier the veil switches on and off at once.
 */
class SoftPressIndication(private val tier: EffectTier, private val ink: Color) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = SoftPressNode(interactionSource, tier == EffectTier.LIGHT, ink)

    override fun equals(other: Any?) = other is SoftPressIndication && other.tier == tier && other.ink == ink
    override fun hashCode() = 31 * tier.hashCode() + ink.hashCode()
}

private class SoftPressNode(
    private val source: InteractionSource,
    private val instant: Boolean,
    private val ink: Color,
) : Modifier.Node(), DrawModifierNode {
    private val veil = Animatable(0f)
    private var focused by mutableStateOf(false)
    private var pressJob: Job? = null
    private var releaseJob: Job? = null

    override fun onAttach() {
        coroutineScope.launch {
            source.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        releaseJob?.cancel()
                        pressJob = coroutineScope.launch { if (instant) veil.snapTo(1f) else veil.animateTo(1f, tween(VEIL_IN_MS)) }
                    }
                    is PressInteraction.Release, is PressInteraction.Cancel -> {
                        releaseJob = coroutineScope.launch {
                            pressJob?.join() // at least the time it takes to come in
                            if (instant) veil.snapTo(0f) else veil.animateTo(0f, tween(VEIL_OUT_MS))
                        }
                    }
                    is FocusInteraction.Focus -> focused = true
                    is FocusInteraction.Unfocus -> focused = false
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val alpha = veil.value * PRESS_VEIL
        if (alpha > 0f) drawRect(ink.copy(alpha = alpha))
        if (focused) {
            val ring = FOCUS_RING.toPx()
            drawRect(ink, topLeft = Offset(ring / 2, ring / 2), size = Size(size.width - ring, size.height - ring), style = Stroke(ring))
        }
    }
}
