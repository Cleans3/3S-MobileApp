package shelftap.android.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import shelftap.android.R
import shelftap.android.ui.theme.EffectTier
import shelftap.android.ui.theme.LocalEffectTier
import shelftap.android.ui.theme.MenuSize
import shelftap.android.ui.theme.Motion
import shelftap.android.ui.theme.Spacing

/** How much of the page shows through the glass panel: very little, so the menu stays readable even where the blur is not drawn (0.86 let page text bleed through in the audit screenshot). */
private const val GLASS_ALPHA = 0.94f

/** The panel starts a little smaller than its final size and grows into it. */
private const val MENU_START_SCALE = 0.92f

/** One entry of the ☰ menu: where it goes and its label. */
data class MenuItem(val route: String, @StringRes val label: Int)

/** The bar above every main screen: the ☰ button and the screen's name. */
@Composable
fun AppTopBar(@StringRes title: Int, menuOpen: Boolean, onMenu: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        val label = stringResource(if (menuOpen) R.string.menu_close else R.string.menu_open)
        IconButton(onClick = onMenu, modifier = Modifier.semantics { contentDescription = label }) { MenuGlyph(open = menuOpen) }
        // While the menu is open the title is behind it, and the menu lists the same name: say it once.
        Text(
            stringResource(title), style = MaterialTheme.typography.titleLarge,
            modifier = if (menuOpen) Modifier.clearAndSetSemantics {} else Modifier,
        )
    }
}

/** ☰ when closed, ✕ when open. Drawn from lines, so it needs no icon library and takes the text colour. */
@Composable
private fun MenuGlyph(open: Boolean) {
    val color = LocalContentColor.current
    val tier = LocalEffectTier.current
    val progress by animateFloatAsState(if (open) 1f else 0f, Motion.spec(tier, Motion.GLYPH_MS), label = "menuGlyph")
    Canvas(Modifier.size(MenuSize.icon)) {
        val p = progress // read here, in the draw phase: the glyph redraws each frame without recomposing
        val stroke = size.minDimension / 12f
        val a = size.width * 0.15f
        val b = size.width * 0.85f
        val top = size.height * 0.25f
        val mid = size.height * 0.5f
        val bottom = size.height * 0.75f
        fun line(from: Offset, to: Offset, alpha: Float = 1f) = drawLine(color, from, to, strokeWidth = stroke, cap = StrokeCap.Round, alpha = alpha)
        // top bar becomes the "\" stroke, bottom bar the "/" stroke, the middle bar fades out
        line(lerp(Offset(a, top), Offset(a, a), p), lerp(Offset(b, top), Offset(b, b), p))
        line(lerp(Offset(a, bottom), Offset(a, b), p), lerp(Offset(b, bottom), Offset(b, a), p))
        line(Offset(a, mid), Offset(b, mid), alpha = 1f - p)
    }
}

/**
 * The menu panel under the ☰ button, over a dimmed page. Closes on a tap outside, the back gesture, or a choice.
 * [glass]: the page behind is blurred (MEDIUM and FULL tiers), so the panel can let some of it show through; on the
 * light tier the panel is solid. The panel grows from the button's corner and the page dims in (260 ms, none on the light tier).
 */
@Composable
fun MenuOverlay(items: List<MenuItem>, current: String?, glass: Boolean, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)
    val tier = LocalEffectTier.current
    val enter = remember { Animatable(if (tier == EffectTier.LIGHT) 1f else 0f) }
    LaunchedEffect(Unit) { if (tier != EffectTier.LIGHT) enter.animateTo(1f, Motion.spec(tier, Motion.MENU_MS)) }
    Box(Modifier.fillMaxSize()) {
        Box(
            // A tap outside closes. It is a gesture, not a clickable, so TalkBack does not see an unlabelled button;
            // the ✕ button is the accessible way to close.
            Modifier.fillMaxSize().graphicsLayer { alpha = enter.value }.background(Color.Black.copy(alpha = 0.32f))
                .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        )
        Surface(
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(start = Spacing.l, top = Spacing.touch + Spacing.s)
                .graphicsLayer {
                    val p = enter.value
                    alpha = p
                    scaleX = MENU_START_SCALE + (1f - MENU_START_SCALE) * p
                    scaleY = scaleX
                    transformOrigin = TransformOrigin(0f, 0f) // grows from the ☰ corner
                }
                .width(MenuSize.width),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (glass) GLASS_ALPHA else 1f),
            shadowElevation = Spacing.s,
        ) {
            Column(Modifier.selectableGroup().padding(Spacing.s)) {
                items.forEach { item ->
                    val selected = item.route == current
                    val shape = RoundedCornerShape(Spacing.m)
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = Spacing.touch).clip(shape)
                            .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                            .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(item.route) })
                            .padding(horizontal = Spacing.m),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(item.label), style = if (selected) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
