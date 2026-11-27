package shelftap.android.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import shelftap.android.R
import shelftap.android.ui.icons.LucideIcon
import shelftap.android.ui.icons.LucideIcons
import shelftap.android.ui.theme.EffectTier
import shelftap.android.ui.theme.LocalEffectTier
import shelftap.android.ui.theme.Motion
import shelftap.android.ui.theme.Spacing

// The colours of the board `Splash`: the same in the light and dark themes (the starting window is the same teal).
private val TEAL = Color(0xFF2F4A44)
private val TEAL_SOFT = Color(0xFF3D5F57)
private val GOLD = Color(0xFFC9A66B)
private const val GOLD_ALPHA = 0.55f
private const val TILE_FILL = 0.22f
private const val TILE_EDGE = 0.5f
private const val TEXT_ALPHA = 0.85f
private const val TRACK_ALPHA = 0.2f

// The board's motion: `st-pop` (the tile, 380 ms: from 60% to 106% at 70% of the way, then to 100%), `st-rise` (the name and the line, 420 ms,
// 80 ms later, from 16 dp below) and `st-indet` (the bar: a pill 40% wide that runs from before the left end to 260% of its width, 1.3 s, forever).
private const val POP_MS = 380
private const val RISE_MS = 420
private const val RISE_DELAY_MS = 80
private const val POP_FROM = 0.6f
private const val POP_PEAK = 1.06f
private const val POP_PEAK_AT = 0.7f
private val RISE_FROM = 16.dp
private const val BAR_MS = 1300
private const val BAR_WIDTH = 0.4f
private const val BAR_TRAVEL = 3.6f
private val BAR_EASING = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

/**
 * The start screen, built to the board `Splash`: teal with two soft blobs, the book on a glass tile that pops in, the name and a line that
 * rise in after it, and at the foot a bar that runs and "checking your session". It shows while the stored session is looked at, and for
 * [Motion.SPLASH_MIN_MS] at least. On the light tier (animations off) everything is simply there.
 * The board's blur behind the blobs is drawn as soft-edged gradients, which cost no blur pass at start.
 */
@Composable
fun StartupScreen() {
    val tier = LocalEffectTier.current
    val pop = remember { Animatable(0f) }
    val rise = remember { Animatable(0f) }
    LaunchedEffect(tier) {
        if (tier == EffectTier.LIGHT) {
            pop.snapTo(1f); rise.snapTo(1f)
        } else {
            launch { pop.animateTo(1f, tween(POP_MS, easing = Motion.decelerate)) }
            launch { rise.animateTo(1f, tween(RISE_MS, RISE_DELAY_MS, Motion.decelerate)) }
        }
    }
    val risePx = with(LocalDensity.current) { RISE_FROM.toPx() }
    Box(
        Modifier.fillMaxSize().background(TEAL).drawBehind {
            val d = density
            // Left blob (a 320 dp circle) and right blob (300 dp), at the board's places on a 390 x 844 page.
            drawCircle(Brush.radialGradient(listOf(TEAL_SOFT, TEAL_SOFT.copy(alpha = 0f)), Offset(70f * d, 280f * d), 190f * d), 190f * d, Offset(70f * d, 280f * d))
            val right = Offset(size.width - 40f * d, 570f * d)
            drawCircle(Brush.radialGradient(listOf(GOLD.copy(alpha = GOLD_ALPHA), GOLD.copy(alpha = 0f)), right, 180f * d), 180f * d, right)
        },
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(TOP_SHARE))
            val tile = RoundedCornerShape(TILE_RADIUS)
            Box(
                Modifier.size(TILE)
                    .graphicsLayer {
                        val p = pop.value
                        val scale = popScale(p)
                        alpha = (p / POP_PEAK_AT).coerceAtMost(1f)
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(tile).background(Color.White.copy(alpha = TILE_FILL)).border(BorderStroke(1.dp, Color.White.copy(alpha = TILE_EDGE)), tile),
                contentAlignment = Alignment.Center,
            ) { LucideIcon(LucideIcons.MockupCatalog, tint = Color.White, size = 44.dp) }
            Column(
                Modifier.padding(top = Spacing.l + Spacing.xs).graphicsLayer { alpha = rise.value; translationY = risePx * (1f - rise.value) },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs + Spacing.xs / 2),
            ) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                Text(
                    stringResource(R.string.splash_tagline), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = TEXT_ALPHA),
                    textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = Spacing.xl),
                )
            }
            Spacer(Modifier.weight(BOTTOM_SHARE))
            Column(
                Modifier.navigationBarsPadding().padding(horizontal = 60.dp).padding(bottom = 120.dp).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                RunningBar(Modifier.fillMaxWidth())
                Text(stringResource(R.string.splash_checking), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = TEXT_ALPHA), textAlign = TextAlign.Center)
            }
        }
    }
}

/** The board's bar: a pill 40% of the track wide runs across it again and again; on the light tier it stands still in the middle of the track. */
@Composable
private fun RunningBar(modifier: Modifier = Modifier) {
    val track = modifier.height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = TRACK_ALPHA))
    if (LocalEffectTier.current == EffectTier.LIGHT) {
        Canvas(track) { drawPill(size.width * 0.3f) }
    } else {
        val run by rememberInfiniteTransition(label = "start bar").animateFloat(0f, 1f, infiniteRepeatable(tween(BAR_MS, easing = BAR_EASING), RepeatMode.Restart), label = "run")
        // The position is read while drawing, so nothing recomposes per frame.
        Canvas(track) { drawPill(barPillX(run, size.width)) }
    }
}

/** The tile's scale at [progress] (0 to 1) of its pop: from 60% up to 106% at 70% of the way, then back to 100% (the board's `st-pop`). */
internal fun popScale(progress: Float): Float =
    if (progress < POP_PEAK_AT) POP_FROM + (POP_PEAK - POP_FROM) * (progress / POP_PEAK_AT)
    else POP_PEAK - (POP_PEAK - 1f) * ((progress - POP_PEAK_AT) / (1f - POP_PEAK_AT))

/** The left end of the running pill at [run] (0 to 1) on a track [width] wide: it starts one pill-width before the track and ends 260% of a pill-width past its start (`st-indet`). */
internal fun barPillX(run: Float, width: Float): Float = -width * BAR_WIDTH + run * width * BAR_WIDTH * BAR_TRAVEL

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPill(x: Float) {
    drawRoundRect(Color.White, Offset(x, 0f), Size(size.width * BAR_WIDTH, size.height), CornerRadius(size.height / 2f))
}

private val TILE = 96.dp
private val TILE_RADIUS = 30.dp
private const val TOP_SHARE = 0.36f // the tile's top is about 36% down the page, as in the board (300 of 844)
private const val BOTTOM_SHARE = 0.64f
