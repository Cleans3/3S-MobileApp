package shelftap.android.ui.icons

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.clearAndSetSemantics
import shelftap.android.ui.theme.EffectTier
import shelftap.android.ui.theme.LocalEffectTier
import shelftap.android.ui.theme.MenuSize
import shelftap.android.ui.theme.Motion

/**
 * One icon morph baked at build time (`android/app/tools/icon-morph`). [frames] are evenly spaced in time and already
 * carry the spring's easing: each holds [points] x 2 numbers per subpath, the subpaths one after another, in the same
 * order in every frame. [closed] has one '1' (closed loop) or '0' per subpath.
 */
class MorphSpec(val durationMs: Int, val points: Int, val closed: String, val frames: List<String>) {
    val subpaths get() = closed.length
    internal val parsed: List<FloatArray> by lazy { frames.map { f -> f.split(' ').map(String::toFloat).toFloatArray() } }

    init {
        require(frames.size >= 2) { "a morph needs at least two frames" }
        require(frames.all { it.count { c -> c == ' ' } + 1 == subpaths * points * 2 }) { "every frame must hold $subpaths x $points points" }
    }
}

/** Writes the blend of keyframes at [progress] (0 = first frame, 1 = last) into [out]. Pure, so it can be tested without a screen. */
internal fun blendFrames(frames: List<FloatArray>, progress: Float, out: FloatArray) {
    val position = progress.coerceIn(0f, 1f) * (frames.size - 1)
    val i = position.toInt().coerceAtMost(frames.size - 2)
    val f = position - i
    val a = frames[i]
    val b = frames[i + 1]
    for (k in out.indices) out[k] = a[k] + (b[k] - a[k]) * f
}

private const val GRID = 24f
private const val STROKE = 2f

/** A static Lucide icon from its path data (24 x 24 grid, 2-unit round stroke). Decorative: it carries no description. */
@Composable
fun LucideIcon(pathData: String, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    val path = remember(pathData) { PathParser().parsePathString(pathData).toPath() }
    Canvas(modifier.size(MenuSize.icon).clearAndSetSemantics {}) {
        scale(size.width / GRID, size.width / GRID, pivot = Offset.Zero) {
            drawPath(path, tint, style = Stroke(STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/**
 * An icon that morphs between the start and end of [spec] when [toEnd] changes.
 * - FULL tier: the baked Morphicons spring, played over its settle time, read in the draw phase (no recomposition per frame).
 * - MEDIUM tier: the two shapes cross-fade (150 ms).
 * - LIGHT tier: the new shape appears at once.
 * The icon shows the right shape at once when it first appears; only a later change animates. Decorative: callers describe the button.
 */
@Composable
fun MorphIcon(spec: MorphSpec, toEnd: Boolean, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    val tier = LocalEffectTier.current
    val progress = remember(spec) { Animatable(if (toEnd) 1f else 0f) }
    LaunchedEffect(spec, toEnd, tier) {
        val target = if (toEnd) 1f else 0f
        when (tier) {
            EffectTier.FULL -> progress.animateTo(target, tween(spec.durationMs, easing = LinearEasing))
            EffectTier.MEDIUM -> progress.animateTo(target, tween(Motion.SHORT_FADE_MS, easing = Motion.decelerate))
            EffectTier.LIGHT -> progress.snapTo(target)
        }
    }
    val frames = spec.parsed
    val buffer = remember(spec) { FloatArray(frames[0].size) }
    val path = remember { Path() }
    Canvas(modifier.size(MenuSize.icon).clearAndSetSemantics {}) {
        val p = progress.value // read here, in the draw phase
        scale(size.width / GRID, size.width / GRID, pivot = Offset.Zero) {
            val stroke = Stroke(STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round)
            if (tier == EffectTier.MEDIUM) {
                drawShape(spec, frames.first(), path, tint.copy(alpha = tint.alpha * (1f - p)), stroke)
                drawShape(spec, frames.last(), path, tint.copy(alpha = tint.alpha * p), stroke)
            } else {
                blendFrames(frames, p, buffer)
                drawShape(spec, buffer, path, tint, stroke)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawShape(spec: MorphSpec, pts: FloatArray, path: Path, color: Color, stroke: Stroke) {
    if (color.alpha <= 0f) return
    path.rewind()
    var at = 0
    for (s in 0 until spec.subpaths) {
        path.moveTo(pts[at], pts[at + 1])
        for (i in 1 until spec.points) path.lineTo(pts[at + 2 * i], pts[at + 2 * i + 1])
        if (spec.closed[s] == '1') path.close()
        at += spec.points * 2
    }
    drawPath(path, color, style = stroke)
}
