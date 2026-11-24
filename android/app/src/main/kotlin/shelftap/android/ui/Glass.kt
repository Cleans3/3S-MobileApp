package shelftap.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import shelftap.android.ui.theme.pressScale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import shelftap.android.ui.theme.coverColors
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import shelftap.android.R
import shelftap.android.ui.icons.LucideIcon
import shelftap.android.ui.icons.LucideIcons
import shelftap.android.ui.icons.LucideMorphs
import shelftap.android.ui.icons.MorphIcon
import shelftap.android.ui.theme.GlassSize
import shelftap.android.ui.theme.Radius
import shelftap.android.ui.theme.ShelfTapText
import shelftap.android.ui.theme.Spacing
import shelftap.android.ui.theme.shelfTap

/*
 * The glass components of the mockup (plan section 5.5): translucent fills with a light edge, large radii, pill shapes.
 * They draw plain translucent fills; the blur lives only on the floating layers (the menu), never on list rows (section 7).
 */

private const val GLASS_FILL = 0.62f
private const val GLASS_FIELD_FILL = 0.75f
private const val GLASS_EDGE = 0.8f
private const val FIELD_EDGE = 0.5f
/** The mockup uses 0.7; over a light cover in the dark theme that left "All copies on loan" at 4.0:1, so the panel is denser. */
private const val CARD_PANEL_FILL = 0.88f
private const val ARROW_FILL = 0.25f

/** A translucent panel with a light edge. */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Radius.card),
    fill: Float = GLASS_FILL,
    content: @Composable () -> Unit,
) {
    val c = MaterialTheme.shelfTap
    Box(modifier.clip(shape).background(c.glass.copy(alpha = fill)).border(BorderStroke(Radius.hairline, c.edge.copy(alpha = GLASS_EDGE)), shape)) { content() }
}

/** A round glass button (menu, account), 48 dp: the smallest touch target. [description] is what TalkBack says. */
@Composable
fun GlassCircleButton(onClick: () -> Unit, description: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    // The shrink is outside the surface's clip and the veil inside it, so a press is never a square around the circle.
    val source = remember { MutableInteractionSource() }
    GlassSurface(modifier.size(GlassSize.round).pressScale(source), shape = CircleShape) {
        Box(
            Modifier.fillMaxSize().semantics { contentDescription = description }
                .clickable(interactionSource = source, indication = LocalIndication.current, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}

/**
 * The search field of the mockup: a 52 dp pill, the magnifier on the left (a cross once there is text, which clears the
 * field), the keyboard's search key searches at once. [label] names the field for TalkBack; [placeholder] is shown while empty.
 */
@Composable
fun SearchPill(value: String, onValueChange: (String) -> Unit, label: String, placeholder: String, onSearch: () -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.shelfTap
    Row(
        modifier.fillMaxWidth().height(GlassSize.search).clip(CircleShape).background(c.glass.copy(alpha = GLASS_FIELD_FILL))
            .border(BorderStroke(Radius.hairline, c.ink.copy(alpha = FIELD_EDGE)), CircleShape).padding(start = Spacing.xs, end = Spacing.l),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        SearchLeadingIcon(hasText = value.isNotEmpty(), onClear = { onValueChange("") })
        BasicTextField(
            value = value, onValueChange = onValueChange, singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.ink),
            cursorBrush = SolidColor(c.ink),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            modifier = Modifier.weight(1f).semantics { contentDescription = label },
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = c.ink3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    inner()
                }
            },
        )
    }
}

/** The magnifier that becomes a cross when the field has text; the cross clears the field. */
@Composable
private fun SearchLeadingIcon(hasText: Boolean, onClear: () -> Unit) {
    val description = stringResource(R.string.search_clear)
    Box(
        Modifier.size(GlassSize.round).then(
            if (hasText) Modifier.semantics { contentDescription = description }.pressable(CircleShape, onClear, shrink = false) else Modifier,
        ),
        contentAlignment = Alignment.Center,
    ) { MorphIcon(LucideMorphs.SearchToX, toEnd = hasText, tint = MaterialTheme.shelfTap.ink3) }
}

/** The title of a strip and, when there is more, a "see all" link with at least a 48 dp touch target. */
@Composable
fun SectionHeader(title: String, actionLabel: String?, onAction: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = ShelfTapText.sectionTitle, modifier = Modifier.weight(1f, fill = false))
        if (actionLabel != null) {
            Box(Modifier.height(GlassSize.round).pressable(RoundedCornerShape(Radius.tile), onAction, shrink = false).padding(start = Spacing.m), contentAlignment = Alignment.CenterEnd) {
                Text(actionLabel, style = ShelfTapText.link, color = MaterialTheme.shelfTap.ink3)
            }
        }
    }
}

/** Content whose size does not follow the system font scale: picture-like lettering such as a jacket. */
@Composable
fun FixedScale(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1f)) { content() }
}

/** Marks a purely decorative block: nothing inside it is read out. */
fun Modifier.clearAndDecorative(): Modifier = this.clearAndSetSemantics {}

/** A book without an image: its colour and its title, so the list never shows an empty box. Decorative: the row names the book. */
@Composable
fun TextCover(title: String, modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(Radius.cover), large: Boolean = false) {
    val (bg, fg) = coverColors(title)
    // Cover art is a picture: its lettering keeps its size when the system font is enlarged, like the print on a real jacket.
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1f)) {
        Box(modifier.clearAndSetSemantics {}.clip(shape).background(bg).padding(if (large) Spacing.m else Spacing.xs)) {
            Text(
                title, color = fg, maxLines = if (large) 4 else 5, overflow = TextOverflow.Clip,
                style = if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/** A book card of a strip: the cover fills it, a glass panel at the bottom carries author and availability. One TalkBack sentence. */
@Composable
fun BookCard(title: String, author: String, status: String, onClick: () -> Unit, cover: @Composable () -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.shelfTap
    Box(
        modifier.size(GlassSize.cardWidth, GlassSize.cardHeight).pressable(RoundedCornerShape(Radius.card), onClick)
            .semantics(mergeDescendants = true) { contentDescription = "$title, $author, $status" },
    ) {
        cover()
        Column(
            Modifier.align(Alignment.BottomCenter).padding(GlassSize.cardInset).fillMaxWidth()
                .clip(RoundedCornerShape(Radius.tile)).background(c.glass.copy(alpha = CARD_PANEL_FILL)).padding(horizontal = GlassSize.panelInset, vertical = Spacing.s),
        ) {
            Text(author, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = c.ink)
            Text(status, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = c.ink2)
        }
    }
}

/** The arrow in the banner's corner. */
@Composable
fun GlassArrow(modifier: Modifier = Modifier) {
    GlassSurface(modifier.size(GlassSize.arrow), shape = CircleShape, fill = ARROW_FILL) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LucideIcon(LucideIcons.ArrowUpRight, tint = Color.White, size = GlassSize.arrowIcon)
        }
    }
}
