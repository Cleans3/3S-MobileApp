package shelftap.android.ui

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntSize

/**
 * A blur of what lies behind a floating panel, made by hand (Compose has no backdrop filter): the page records itself into [pageLayer]
 * while [enabled]; the panel draws that recording again under itself, clipped to its shape and blurred more strongly than the page.
 * Nothing is recorded while the layer is shut, so the cost is zero then.
 */
@Stable
class BackdropState(val pageLayer: GraphicsLayer) {
    var enabled by mutableStateOf(false)
    var pageOrigin by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberBackdrop(): BackdropState {
    val layer = rememberGraphicsLayer()
    return remember(layer) { BackdropState(layer) }
}

/** Put on the page: it draws as usual, and also records itself for the panel while [BackdropState.enabled]. */
fun Modifier.recordBackdrop(state: BackdropState): Modifier = this
    .onGloballyPositioned { state.pageOrigin = it.positionInWindow() }
    .drawWithContent {
        if (state.enabled) {
            state.pageLayer.record { this@drawWithContent.drawContent() }
            drawLayer(state.pageLayer)
        } else {
            drawContent()
        }
    }

/** The blur of the glass behind the panel, with the saturation boost of the mockup (`blur(26px) saturate(160%)`). Android 12 and later. */
@RequiresApi(31)
private fun glassEffect(blurPx: Float, saturation: Float): androidx.compose.ui.graphics.RenderEffect {
    val matrix = ColorMatrix().apply { setSaturation(saturation) }
    val blur = RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP)
    return RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(matrix), blur).asComposeRenderEffect()
}

/**
 * Put on the panel, before its fill: draws the recorded page behind the panel, blurred, clipped by the caller (a `clip(shape)` before this), with [scrim] over it
 * (the scrim is in front of the page in the mockup too, so the glass sees it). Needs Android 12; the callers only use it on the
 * medium and full tiers.
 */
@Composable
fun Modifier.backdropBlur(state: BackdropState, blurPx: Float, saturation: Float, scrim: Color): Modifier {
    val layer = rememberGraphicsLayer()
    var panelOrigin by remember { mutableStateOf<Offset?>(null) } // unknown until the panel is placed: nothing is drawn before that
    return this
        .onGloballyPositioned { panelOrigin = it.positionInWindow() }
        .drawBehind {
            val origin = panelOrigin
            if (android.os.Build.VERSION.SDK_INT >= 31 && origin != null) {
                layer.renderEffect = glassEffect(blurPx, saturation)
                layer.record(size = IntSize(size.width.toInt(), size.height.toInt())) {
                    val shift = state.pageOrigin - origin
                    translate(shift.x, shift.y) { drawLayer(state.pageLayer) }
                    drawRect(scrim)
                }
                drawLayer(layer)
            }
        }
}
