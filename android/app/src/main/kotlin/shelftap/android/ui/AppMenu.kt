package shelftap.android.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
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
import shelftap.android.ui.icons.LucideIcon
import shelftap.android.ui.icons.LucideIcons
import shelftap.android.ui.icons.LucideMorphs
import shelftap.android.ui.icons.MorphIcon
import shelftap.android.ui.theme.EffectTier
import shelftap.android.ui.theme.GlassSize
import shelftap.android.ui.theme.LocalEffectTier
import shelftap.android.ui.theme.MenuSize
import shelftap.android.ui.theme.Radius
import shelftap.android.ui.theme.ShelfTapText
import shelftap.android.ui.theme.menuScrim
import shelftap.android.ui.theme.menuShadow
import shelftap.android.ui.theme.shelfTap
import shelftap.android.ui.theme.Motion
import shelftap.android.ui.theme.Spacing

/** The active entry of the menu: a white (dark: smoky) pill, 0.85 as in the mockup. */
private const val ACTIVE_PILL = 0.85f

/**
 * The ☰ panel's glass: 0.62 in the mockup; 0.70 here, so the worst case behind it (a white page under the dark theme's smoky glass,
 * a black one under the white glass) still gives the text 4.5:1 once the page is blurred. The light tier has no blur and a solid panel.
 */
private const val MENU_FILL = 0.70f

/** The rail and the side menu have nothing scrolling behind them, so no blur is needed: the mockup's 0.62. */
private const val NAV_FILL = 0.62f

/** The edge of the glass: 0.85 in the mockup. */
private const val MENU_EDGE = 0.85f

/** The warm dimming behind the open menu: 0.16 in the mockup. */
private const val SCRIM_ALPHA = 0.16f

/** CSS `saturate(160%)` behind the panel. */
private const val BACKDROP_SATURATION = 1.6f


/** One entry of the ☰ menu: where it goes, its label and its Lucide icon (path data from [shelftap.android.ui.icons.LucideIcons]). */
data class MenuItem(val route: String, @StringRes val label: Int, val icon: String)

/**
 * The bar above every main screen: the ☰ button and the screen's name.
 * On a book's page ([back]) the same round button stays where it is, and its glyph turns from ☰ into ← while the library's name and the
 * account button fade out, so the button does not jump and the two screens share it; [onBack] is what it does then.
 */
@Composable
fun AppTopBar(
    @StringRes title: Int, menuOpen: Boolean, onMenu: () -> Unit,
    centred: Boolean = false, accountDescription: String? = null, onAccount: (() -> Unit)? = null,
    back: Boolean = false, onBack: () -> Unit = {},
) {
    if (centred) {
        val tier = LocalEffectTier.current
        val toBack = remember { Animatable(if (back) 1f else 0f) } // 0 the menu button .. 1 the back button
        LaunchedEffect(back, tier) { toBack.animateTo(if (back) 1f else 0f, Motion.spec(tier, Motion.GLYPH_MS)) }
        val accountShown by remember { derivedStateOf { toBack.value < 1f } }
        // The catalog's bar in the mockup: the menu button, the library's name in the middle, an account button.
        Row(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassCircleButton(
                onClick = if (back) onBack else onMenu,
                description = stringResource(if (back) R.string.back_to_catalog else if (menuOpen) R.string.menu_close else R.string.menu_open),
            ) {
                // Both glyphs are drawn, one turning out of sight as the other turns in: only draw-phase values change per frame.
                Box(contentAlignment = Alignment.Center) {
                    Box(Modifier.graphicsLayer { alpha = 1f - toBack.value; rotationZ = -QUARTER_TURN * toBack.value }) { MenuGlyph(open = menuOpen) }
                    Box(Modifier.graphicsLayer { alpha = toBack.value; rotationZ = QUARTER_TURN * (1f - toBack.value) }) {
                        LucideIcon(LucideIcons.ArrowLeft, tint = MaterialTheme.shelfTap.ink, size = GlassSize.menuIcon)
                    }
                }
            }
            Text(
                stringResource(title), style = ShelfTapText.barTitle, color = MaterialTheme.shelfTap.ink2, textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).graphicsLayer { alpha = 1f - toBack.value }
                    .then(if (menuOpen || back) Modifier.clearAndSetSemantics {} else Modifier),
            )
            if (onAccount != null && accountDescription != null && accountShown) {
                GlassCircleButton(
                    onClick = onAccount, description = accountDescription,
                    modifier = Modifier.graphicsLayer { alpha = 1f - toBack.value }.then(if (back) Modifier.clearAndSetSemantics {} else Modifier),
                ) {
                    LucideIcon(LucideIcons.User, tint = MaterialTheme.shelfTap.ink, size = GlassSize.menuIcon)
                }
            } else {
                Spacer(Modifier.size(GlassSize.round))
            }
        }
        return
    }
    Row(
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        val label = stringResource(if (menuOpen) R.string.menu_close else R.string.menu_open)
        GlassCircleButton(onClick = onMenu, description = label) { MenuGlyph(open = menuOpen) }
        // While the menu is open the title is behind it, and the menu lists the same name: say it once.
        Text(
            stringResource(title), style = MaterialTheme.typography.titleLarge,
            modifier = if (menuOpen) Modifier.clearAndSetSemantics {} else Modifier,
        )
    }
}

/** The glyphs turn a quarter of a turn as they swap. */
private const val QUARTER_TURN = 90f

/** At or under this window height the side menu is dense. */
private const val DENSE_HEIGHT_DP = 480

/** ☰ when closed, ✕ when open: the Lucide "menu" and "x" icons morphing with the baked spring (full tier), cross-fading (medium) or switching (light). */
@Composable
private fun MenuGlyph(open: Boolean) {
    MorphIcon(LucideMorphs.MenuToX, toEnd = open)
}

/**
 * The menu panel under the ☰ button, over a page that is blurred and dimmed as a whole. Closes on a tap outside, the back gesture, or a choice.
 * Everything comes in at once and by the same amount: [open] runs from 0 (shut) to 1 (open) and is driven by [MenuLayer], which also drives
 * it back to 0 for the exit. The dimming fades over the whole screen (the page's blur, in [MainNav], runs with it), and the panel fades in
 * where it stands with a small rise; no rectangle grows from a corner and nothing is scaled. With [glass] (the blur tiers) the panel is
 * translucent glass over the blurred page, else solid. [interactive] is false while the menu is closing.
 */
@Composable
fun MenuOverlay(items: List<MenuItem>, current: String?, glass: Boolean, open: () -> Float, interactive: Boolean, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    BackHandler(enabled = interactive, onBack = onDismiss)
    Box(Modifier.fillMaxSize()) {
        Box(
            // A tap outside closes. It is a gesture, not a clickable, so TalkBack does not see an unlabelled button;
            // the ✕ button is the accessible way to close.
            Modifier.fillMaxSize().graphicsLayer { alpha = open() }.background(menuScrim.copy(alpha = SCRIM_ALPHA))
                .pointerInput(interactive) { if (interactive) detectTapGestures { onDismiss() } },
        )
        val shape = RoundedCornerShape(Radius.menu)
        val c = MaterialTheme.shelfTap
        val risePx = with(LocalDensity.current) { MenuSize.rise.toPx() }
        Box(
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(start = Spacing.l, top = Spacing.touch + Spacing.s)
                .graphicsLayer {
                    val p = open()
                    // Alpha is applied to each thing drawn, not to an off-screen copy of the panel: that copy ended at the panel's rectangle and
                    // cut the shadow there, which showed as a dark square around the panel while it faded in.
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                    alpha = p
                    translationY = -risePx * (1f - p)
                }
                .width(MenuSize.width)
                .shadow(MenuSize.shadow, shape, ambientColor = menuShadow, spotColor = menuShadow)
                .clip(shape)
                .background(if (glass) c.glass.copy(alpha = MENU_FILL) else MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(BorderStroke(Radius.hairline, c.edge.copy(alpha = MENU_EDGE)), shape),
        ) {
            Column(Modifier.selectableGroup().padding(Spacing.s)) {
                items.forEach { item ->
                    val selected = item.route == current
                    val shape = RoundedCornerShape(Spacing.m)
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = Spacing.touch).clip(shape)
                            .background(if (selected) MaterialTheme.shelfTap.glass.copy(alpha = ACTIVE_PILL) else Color.Transparent)
                            .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(item.route) })
                            .padding(horizontal = Spacing.s),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                    ) {
                        MenuIconTile(item.icon, selected)
                        Text(stringResource(item.label), style = if (selected) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

/** The title of a main screen when the menu is always on screen: no ☰ button. */
@Composable
fun AppTitleBar(@StringRes title: Int) {
    Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = Spacing.l, vertical = Spacing.s)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
    }
}

/**
 * The menu as a column that stays on screen: the rail (96 dp, labels centred) on a medium window and the fixed menu
 * (216 dp) on a wide one. Members get the "Tap a book to borrow" button at the top. Scrolls, so large text never cuts it.
 */
@Composable
fun SideNav(
    items: List<MenuItem>, current: String?, width: androidx.compose.ui.unit.Dp, centred: Boolean, showPill: Boolean, onPill: () -> Unit, onSelect: (String) -> Unit,
    onToggle: (() -> Unit)? = null,
) {
    // On a window under 480 dp tall (a phone held sideways) the entries sit closer, so all five fit without scrolling.
    val dense = LocalConfiguration.current.screenHeightDp <= DENSE_HEIGHT_DP

    val c = MaterialTheme.shelfTap
    val edge = c.edge.copy(alpha = MENU_EDGE)
    val hairline = with(LocalDensity.current) { Radius.hairline.toPx() }
    // The mockup's glass: the rail is a strip with an edge on its right side; the wide menu floats, inset, with the panel's radius.
    // Nothing scrolls behind either, so there is no blur to pay for: a translucent fill over the page colour.
    val shape = if (centred) androidx.compose.ui.graphics.RectangleShape else RoundedCornerShape(Radius.menu)
    val glassModifier = if (centred) {
        Modifier.width(width).fillMaxHeight().background(c.glass.copy(alpha = NAV_FILL))
            .drawBehind { drawLine(edge, Offset(size.width - hairline / 2, 0f), Offset(size.width - hairline / 2, size.height), hairline) }
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Bottom + WindowInsetsSides.Start))
    } else {
        Modifier.windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Bottom + WindowInsetsSides.Start))
            .padding(MenuSize.sideInset).width(width).fillMaxHeight().clip(shape).background(c.glass.copy(alpha = NAV_FILL))
            .border(BorderStroke(Radius.hairline, edge), shape)
    }
    Column(
        glassModifier.verticalScroll(rememberScrollState()).padding(Spacing.s).selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(if (dense) Spacing.none else Spacing.xs),
    ) {
        if (onToggle != null) {
            // The button at the head of the wide menu, the same round glass as the top bar's buttons: it folds the menu into the rail
            // (labels under icons) and opens it again. Its icon is a panel with an arrow in the direction the panel will go.
            val label = stringResource(if (centred) R.string.nav_expand else R.string.nav_collapse)
            GlassCircleButton(
                onClick = onToggle, description = label,
                modifier = Modifier.align(if (centred) Alignment.CenterHorizontally else Alignment.Start),
            ) { LucideIcon(if (centred) LucideIcons.PanelLeftOpen else LucideIcons.PanelLeftClose, tint = c.ink, size = GlassSize.menuIcon) }
        }
        if (showPill) {
            // The rail is too narrow for the full sentence: it shows one word, and TalkBack hears the name of the screen it opens.
            // (Not the sentence: it starts with "tap", which Accessibility Test Framework rejects in a description.)
            val full = stringResource(R.string.tab_checkout)
            PrimaryButton(
                onClick = onPill,
                modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch).then(if (centred) Modifier.semantics { contentDescription = full } else Modifier),
                contentPadding = PaddingValues(horizontal = if (centred) Spacing.xs else Spacing.l, vertical = Spacing.s), // a button's default side padding leaves no room in 96 dp
            ) {
                Text(
                    stringResource(if (centred) R.string.checkout_pill_short else R.string.checkout_pill), textAlign = TextAlign.Center,
                    maxLines = if (centred) 1 else Int.MAX_VALUE, style = MaterialTheme.typography.labelLarge,
                    modifier = if (centred) Modifier.clearAndSetSemantics {} else Modifier,
                )
            }
        }
        items.forEach { item ->
            val selected = item.route == current
            // The screen's own title and its buttons carry the same names as these entries, and are on screen at the same time:
            // the entry is read as "Go to <name>" so no two controls sound alike.
            val goTo = stringResource(R.string.nav_go_to, stringResource(item.label))
            Box(
                Modifier.fillMaxWidth().heightIn(min = Spacing.touch).clip(RoundedCornerShape(Spacing.m))
                    .background(if (selected) MaterialTheme.shelfTap.glass.copy(alpha = ACTIVE_PILL) else Color.Transparent)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(item.route) })
                    .semantics { contentDescription = goTo }
                    .padding(horizontal = Spacing.s, vertical = if (dense) Spacing.xs else Spacing.s),
                contentAlignment = if (centred) Alignment.Center else Alignment.CenterStart,
            ) {
                val label: @Composable () -> Unit = {
                    Text(
                        stringResource(item.label), textAlign = if (centred) TextAlign.Center else TextAlign.Start,
                        style = if (selected) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clearAndSetSemantics {},
                    )
                }
                if (centred) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        MenuIconTile(item.icon, selected)
                        label()
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                        MenuIconTile(item.icon, selected)
                        label()
                    }
                }
            }
        }
    }
}

/** The entry's icon, drawn plainly as in the mockup (no tile): the active entry is marked by its pill, not by the icon. */
@Composable
private fun MenuIconTile(pathData: String, selected: Boolean) {
    LucideIcon(pathData, tint = MaterialTheme.shelfTap.ink, size = GlassSize.menuIcon)
}
