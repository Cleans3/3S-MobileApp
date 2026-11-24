package shelftap.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import shelftap.android.R
import shelftap.android.ui.icons.LucideIcon
import shelftap.android.ui.icons.LucideIcons
import shelftap.android.ui.theme.EffectTier
import shelftap.android.ui.theme.GlassSize
import shelftap.android.ui.theme.HeroSize
import shelftap.android.ui.theme.LocalEffectTier
import shelftap.android.ui.theme.Motion
import shelftap.android.ui.theme.Spacing
import shelftap.android.ui.theme.pressScale

/** The glass circle inside the pill: the NFC glyph on a lighter disc (the mockup). */
private const val PILL_CIRCLE = 0.16f

private val PILL_HEIGHT = 56.dp
private val PILL_LIFT = 16.dp // the gap the scaffold leaves between the floating button and the bottom edge
private val PILL_PAD = (PILL_HEIGHT - GlassSize.pillCircle) / 2 // the same all round the disc, so folded it is a circle

/**
 * The "Tap a book to borrow" button. [collapsed] (on a book's page): the words fold away and the button shrinks to its icon and moves to
 * where the round "borrow" button sits in the reserve bar at the foot of the page, so it is that button, not a second one. The move
 * and the fold take as long as the page's slide, and the animated values are read when drawing.
 */
@Composable
fun CheckoutPill(collapsed: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tier = LocalEffectTier.current
    val label = stringResource(R.string.checkout_pill)
    val source = remember { MutableInteractionSource() }
    val toBar = remember { Animatable(if (collapsed) 1f else 0f) } // 0 the pill .. 1 the bar's round button
    LaunchedEffect(collapsed, tier) { toBar.animateTo(if (collapsed) 1f else 0f, Motion.spec(tier, Motion.SCREEN_MS)) }
    val density = LocalDensity.current
    val windowWidth = LocalWindowInfo.current.containerSize.width
    // Where the bar's round button is: inset by the bar's margin and padding from the left edge, and above the bottom edge by the bar's margin and padding.
    val barCentreX = with(density) { (Spacing.l + Spacing.s + HeroSize.barButton / 2).toPx() }
    val barCentreY = with(density) { (Spacing.xl + Spacing.s + HeroSize.barButton / 2).toPx() }
    val pillCentreY = with(density) { (PILL_LIFT + PILL_HEIGHT / 2).toPx() }
    val endScale = HeroSize.barButton / PILL_HEIGHT
    val words = tier != EffectTier.LIGHT
    Surface(
        onClick = onClick, interactionSource = source, shape = CircleShape,
        color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, shadowElevation = 6.dp,
        modifier = modifier
            .pressScale(source)
            .graphicsLayer {
                val p = toBar.value
                translationX = (barCentreX - windowWidth / 2f) * p
                translationY = (pillCentreY - barCentreY) * p
                val s = 1f + (endScale - 1f) * p
                scaleX = s
                scaleY = s
            }
            .then(if (collapsed) Modifier.semantics { contentDescription = label } else Modifier),
    ) {
        Row(Modifier.height(PILL_HEIGHT).padding(PILL_PAD), verticalAlignment = Alignment.CenterVertically) {
            // The NFC glyph in a small glass circle (as in the mockup).
            Box(
                Modifier.size(GlassSize.pillCircle).clip(CircleShape).background(MaterialTheme.colorScheme.onPrimary.copy(alpha = PILL_CIRCLE)),
                contentAlignment = Alignment.Center,
            ) { LucideIcon(LucideIcons.MockupNfc, tint = MaterialTheme.colorScheme.onPrimary, size = GlassSize.pillGlyph) }
            AnimatedVisibility(
                visible = !collapsed,
                enter = if (words) expandHorizontally(Motion.spec(tier, Motion.SCREEN_MS)) + fadeIn(Motion.spec(tier, Motion.SCREEN_MS)) else androidx.compose.animation.EnterTransition.None,
                exit = if (words) shrinkHorizontally(Motion.spec(tier, Motion.SCREEN_MS)) + fadeOut(Motion.spec(tier, Motion.SCREEN_MS)) else androidx.compose.animation.ExitTransition.None,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(Spacing.m))
                    Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    Spacer(Modifier.width(Spacing.l))
                }
            }
        }
    }
}
