package shelftap.android.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import shelftap.android.R
import shelftap.android.ui.icons.LucideIcon
import shelftap.android.ui.icons.LucideIcons
import shelftap.android.ui.icons.LucideMorphs
import shelftap.android.ui.icons.MorphIcon
import shelftap.android.ui.theme.EffectTier
import shelftap.android.ui.theme.GlassSize
import shelftap.android.ui.theme.HeroSize
import shelftap.android.ui.theme.LocalEffectTier
import shelftap.android.ui.theme.Motion
import shelftap.android.ui.theme.Spacing
import shelftap.android.ui.theme.pressScale
import shelftap.android.ui.theme.shelfTap

/**
 * What the reserve button of a book's page says and does. The page publishes it ([LocalReserveBar]) so that the dock at the foot of the
 * screen can draw the button: the page slides, the dock does not. [owner] is the page that published it, so a page that is leaving
 * cannot clear what the next page published.
 */
@Immutable
class ReserveAction(
    val owner: Any, val label: String, val fill: Color, val ink: Color, val enabled: Boolean, val reserved: Boolean, val onClick: () -> Unit,
)

/** Where a book's page puts its [ReserveAction] when the button is drawn by the dock (a compact window, a member); null elsewhere. */
val LocalReserveBar = staticCompositionLocalOf<MutableState<ReserveAction?>?> { null }

/** The reserve button: a pill in the action's colour, with the bookmark that turns into a tick once the server confirmed. */
@Composable
fun ReserveButton(action: ReserveAction, modifier: Modifier = Modifier, guest: Boolean = false, clickable: Boolean = true) {
    val source = remember { MutableInteractionSource() }
    Row(
        modifier.pressScale(source).background(action.fill, CircleShape)
            .clickable(interactionSource = source, indication = null, enabled = clickable && action.enabled, role = Role.Button, onClick = action.onClick)
            .padding(horizontal = Spacing.m),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically,
    ) {
        MorphIcon(LucideMorphs.BookmarkToCheck, toEnd = !guest && action.reserved, tint = action.ink)
        Text(action.label, style = MaterialTheme.typography.titleSmall, color = action.ink, maxLines = 2)
    }
}

private val PILL_HEIGHT = 56.dp
private val BAR_HEIGHT = HeroSize.barButton + Spacing.s * 2 // the round button and the bar's padding
private val DOCK_PAD = Spacing.s
private val PILL_TEXT_START = 4.dp
private val PILL_TEXT_END = Spacing.s
private val BAR_LIFT = Spacing.xl - Spacing.l // the bar sits 24 dp above the bottom edge, the floating button 16
private const val PILL_SHADOW_DP = 6f
private const val BAR_FILL = 0.92f
private const val CIRCLE_FILL = 0.16f
private const val LABEL_FADE_OUT_AT = 0.5f

/**
 * The control at the foot of the main screens, one thing that changes shape. On a list it is the "Tap a book to borrow" pill. On a
 * book's page ([onBar]) it grows into the glass bar: the tag circle stays at its left and the reserve button takes the rest, with the
 * same words as the page's own. It does not travel with the page: the page slides over the screen, the dock stays and changes form.
 * On a wide window ([restIcon]) the pill rests as its round icon, at the foot's right corner, and grows leftwards into a bar of at most [maxBarWidth].
 * Size, colours and the two labels are animated for [Motion.SCREEN_MS] (the page's slide), read when measuring and drawing.
 */
@Composable
fun ActionDock(
    onBar: Boolean, reserve: ReserveAction?, onCheckout: () -> Unit, modifier: Modifier = Modifier,
    restIcon: Boolean = false, lift: Boolean = true, sideMargin: Dp = Spacing.l, maxBarWidth: Dp = Dp.Unspecified, maxBarFraction: Float = 1f,
) {
    val tier = LocalEffectTier.current
    val c = MaterialTheme.shelfTap
    val density = LocalDensity.current
    val label = stringResource(R.string.checkout_pill)
    val borrowByTap = stringResource(R.string.title_borrow_by_tap)
    // The pill is named by the screen it opens, not by its sentence: in English that starts with "tap", and a description with an action word is what the checker rejects.
    val checkoutName = stringResource(R.string.tab_checkout)
    val toBar = remember { Animatable(if (onBar) 1f else 0f) } // 0 the pill .. 1 the bar
    LaunchedEffect(onBar, tier) { toBar.animateTo(if (onBar) 1f else 0f, Motion.spec(tier, Motion.SCREEN_MS)) }
    val source = remember { MutableInteractionSource() }

    val textWidth = rememberTextMeasurer().measure(label, MaterialTheme.typography.labelLarge, maxLines = 1).size.width
    val pillWidth = with(density) { (DOCK_PAD * 2 + PILL_TEXT_START + PILL_TEXT_END + GlassSize.pillCircle + Spacing.s).roundToPx() } + textWidth
    val primary = c.primary
    val glass = c.glass.copy(alpha = BAR_FILL)
    val edge = c.edge.copy(alpha = 0.8f)
    val hairline = with(density) { 1.dp.toPx() }

    Box(
        modifier
            .layout { measurable, constraints ->
                val p = toBar.value
                // The bar is as wide as the room allows (less the side margins), up to [maxBarWidth]; at rest it is the pill, or only its round icon.
                val bar = minOf(constraints.maxWidth - sideMargin.roundToPx() * 2, (constraints.maxWidth * maxBarFraction).toInt(), if (maxBarWidth.isSpecified) maxBarWidth.roundToPx() else Int.MAX_VALUE)
                val rest = if (restIcon) PILL_HEIGHT.roundToPx() else pillWidth
                val w = (rest + (bar - rest) * p).toInt().coerceAtLeast(0)
                val h = (PILL_HEIGHT + (BAR_HEIGHT - PILL_HEIGHT) * p).roundToPx()
                val placeable = measurable.measure(Constraints.fixed(w, h))
                layout(w, h) { placeable.place(0, 0) }
            }
            .graphicsLayer {
                val p = toBar.value
                if (lift) translationY = -BAR_LIFT.toPx() * p
                shadowElevation = PILL_SHADOW_DP * density.density * (1f - p)
                shape = CircleShape
                clip = true
            }
            .pressScale(source)
            .drawBehind {
                val p = toBar.value
                drawRect(lerp(primary, glass, p))
                if (p > 0f) drawRoundRect(edge.copy(alpha = edge.alpha * p), cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2), style = androidx.compose.ui.graphics.drawscope.Stroke(hairline))
            }
            // Folded into the pill, the whole dock is the button; as the bar, only its parts are.
            .then(
                if (!onBar) Modifier.clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onCheckout).semantics { contentDescription = checkoutName }
                else Modifier,
            ),
    ) {
        Row(Modifier.fillMaxSize().padding(DOCK_PAD), horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
            // The tag circle: 40 dp on the pill, 52 dp on the bar; translucent white over the pill, solid over the glass.
            Box(
                Modifier
                    .layout { measurable, _ ->
                        val s = (GlassSize.pillCircle + (HeroSize.barButton - GlassSize.pillCircle) * toBar.value).roundToPx()
                        val placeable = measurable.measure(Constraints.fixed(s, s))
                        layout(s, s) { placeable.place(0, 0) }
                    }
                    .drawBehind { drawCircle(lerp(c.onPrimary.copy(alpha = CIRCLE_FILL), primary, toBar.value)) }
                    .then(
                        if (onBar) Modifier.semantics { contentDescription = borrowByTap }.clickable(role = Role.Button, onClick = onCheckout) else Modifier,
                    ),
                contentAlignment = Alignment.Center,
            ) { LucideIcon(LucideIcons.MockupNfc, tint = c.onPrimary, size = GlassSize.pillGlyph) }

            Box(Modifier.weight(1f).height(HeroSize.barButton), contentAlignment = Alignment.CenterStart) {
                // The pill's words fade out before the bar is half open; the reserve button fades in after.
                Text(
                    label, style = MaterialTheme.typography.labelLarge, color = c.onPrimary, maxLines = 1, softWrap = false,
                    modifier = Modifier.padding(start = PILL_TEXT_START, end = PILL_TEXT_END).graphicsLayer { alpha = if (restIcon) 0f else (1f - toBar.value / LABEL_FADE_OUT_AT).coerceIn(0f, 1f) }
                        .then(if (onBar) Modifier.clearAndSetSemantics {} else Modifier), // read once: the dock itself is the pill's button
                )
                if (reserve != null) {
                    ReserveButton(
                        reserve,
                        Modifier.fillMaxSize().graphicsLayer { alpha = ((toBar.value - LABEL_FADE_OUT_AT) / (1f - LABEL_FADE_OUT_AT)).coerceIn(0f, 1f) }
                            .then(if (onBar) Modifier else Modifier.clearAndSetSemantics {}), // not a button until the bar is open
                        guest = false, clickable = onBar,
                    )
                }
            }
        }
    }
}
