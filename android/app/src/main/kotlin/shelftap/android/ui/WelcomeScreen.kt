package shelftap.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import shelftap.android.R
import shelftap.android.ui.theme.AdaptiveSize
import shelftap.android.ui.theme.LoginSize
import shelftap.android.ui.icons.LucideIcon
import shelftap.android.ui.icons.LucideIcons
import shelftap.android.ui.theme.Radius
import shelftap.android.ui.theme.Spacing
import shelftap.android.ui.theme.shelfTap

private val DARKEN = Color(0xFF1C1610)
private const val DARKEN_MID = 0.5f
private const val DARKEN_END = 0.8f
private val DARKEN_HEIGHT = 470.dp
private const val GLASS_BUTTON_FILL = 0.16f
private const val GLASS_BUTTON_EDGE = 0.45f
private const val BODY_ALPHA = 0.92f
private val BUTTON_HEIGHT = 56.dp
private val CONTENT_MAX = 520.dp // a wide window keeps the text and buttons in a column; only the shelves fill it

/**
 * The welcome screen, built to the board `Main` and shown to someone who is not signed in: shelves of books fill the screen and darken
 * towards the bottom, where the headline, one line about the app and the ways in sit: a round button for the library card, "Get started"
 * (the login) and a link to look at the books without logging in. The board's progressive blur under the text is drawn as the darkening
 * alone (a screen-sized blur on every start is not worth it; real backdrop blur is a separate step, plan §7).
 */
@Composable
fun WelcomeScreen(onStart: () -> Unit, onCard: () -> Unit, onGuest: () -> Unit) {
    val c = MaterialTheme.shelfTap
    BoxWithConstraints(Modifier.fillMaxSize().background(c.bg2)) {
        if (maxWidth >= AdaptiveSize.signInSplitMin) {
            // A wide window: the shelves are a column on the left and the welcome is on the page beside them (the boards `Mid*` and `Wide*` sign-in, step "Chào").
            SignInSplitFrame { WideWelcome(onStart, onCard, onGuest) }
            return@BoxWithConstraints
        }
        WelcomeShelves(Modifier.fillMaxSize())
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(DARKEN_HEIGHT).clearAndSetSemantics {}
                .background(Brush.verticalGradient(0f to DARKEN.copy(alpha = 0f), 0.25f to DARKEN.copy(alpha = DARKEN_MID), 1f to DARKEN.copy(alpha = DARKEN_END))),
        )
        Row(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = Spacing.l + Spacing.xs, start = GUTTER, end = GUTTER).widthIn(max = CONTENT_MAX).fillMaxWidth()) { NamePill() }
        // The text and buttons sit at the foot of the screen and scroll when a big font makes them taller than the room under the name pill; the shelves stay.
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = GUTTER).widthIn(max = CONTENT_MAX)
                .heightIn(max = maxHeight - NAME_ROW).verticalScroll(rememberScrollState()).padding(top = Spacing.l, bottom = Spacing.xl + Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.l + Spacing.xs / 2),
        ) {
            Text(
                stringResource(R.string.welcome_title), color = Color.White, fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp,
                modifier = Modifier.semantics { heading() },
            )
            Text(stringResource(R.string.welcome_body), style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = BODY_ALPHA))
            Row(Modifier.padding(top = Spacing.xs + Spacing.xs / 2), horizontalArrangement = Arrangement.spacedBy(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
                val cardLabel = stringResource(R.string.login_with_card)
                GlassButton(Modifier.size(BUTTON_HEIGHT).semantics { contentDescription = cardLabel }, onCard) {
                    LucideIcon(LucideIcons.CreditCard, tint = Color.White, size = 24.dp)
                }
                GlassButton(Modifier.weight(1f).heightIn(min = BUTTON_HEIGHT), onStart, padding = Spacing.s) {
                    Row(Modifier.fillMaxWidth().padding(start = Spacing.l + Spacing.s), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.welcome_start), style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                            LucideIcon(LucideIcons.ArrowUpRight, tint = c.ink, size = 20.dp)
                        }
                    }
                }
            }
            // The link is a 48 dp high touch target with its text in the middle.
            Box(
                Modifier.fillMaxWidth().heightIn(min = Spacing.touch).clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onGuest),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.welcome_guest), style = MaterialTheme.typography.bodyMedium, color = Color.White, textDecoration = TextDecoration.Underline,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** The welcome on the page beside the shelves of a wide window: the name, the headline, the line about the app and the three ways in, in the page's own colours. */
@Composable
private fun WideWelcome(onStart: () -> Unit, onCard: () -> Unit, onGuest: () -> Unit) {
    val c = MaterialTheme.shelfTap
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.l + Spacing.xs / 2),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleSmall, color = c.ink2)
        Text(
            stringResource(R.string.welcome_title), color = c.ink, fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp,
            modifier = Modifier.semantics { heading() },
        )
        Text(stringResource(R.string.welcome_body), style = MaterialTheme.typography.bodyLarge, color = c.ink2)
        PillAction(stringResource(R.string.welcome_start), onClick = onStart, container = c.primary, ink = c.onPrimary, height = LoginSize.button)
        PillAction(
            stringResource(R.string.login_with_card), onClick = onCard, container = c.glass.copy(alpha = 0.85f), ink = c.ink, height = LoginSize.button,
            icon = LucideIcons.CreditCard,
        )
        TextAction(stringResource(R.string.welcome_guest), onClick = onGuest, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

private val GUTTER = Spacing.xl
private val NAME_ROW = 112.dp // the status bar, the name pill and some air: the most the text block may take away from the top

/** The library's name on a glass pill, top left. */
@Composable
private fun NamePill() {
    val c = MaterialTheme.shelfTap
    Row(
        Modifier.clip(CircleShape).background(c.glass.copy(alpha = 0.42f)).border(BorderStroke(Radius.hairline, c.edge.copy(alpha = 0.6f)), CircleShape)
            .padding(horizontal = Spacing.l, vertical = Spacing.m - Spacing.xs / 2),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        LucideIcon(LucideIcons.MockupCatalog, tint = c.ink, size = 20.dp)
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleSmall, color = c.ink)
    }
}

/** A button of the welcome screen: a pill (or circle) of white at 16%, with a light edge, over the darkened shelves. */
@Composable
private fun GlassButton(modifier: Modifier, onClick: () -> Unit, padding: androidx.compose.ui.unit.Dp = 0.dp, content: @Composable () -> Unit) {
    Box(
        modifier.pressable(CircleShape, onClick).background(Color.White.copy(alpha = GLASS_BUTTON_FILL))
            .border(BorderStroke(Radius.hairline, Color.White.copy(alpha = GLASS_BUTTON_EDGE)), CircleShape).padding(padding),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Shelves of books from the top to the bottom of the screen (the palette and sizes of the login's shelves, so every run draws the same ones). Decorative. */
@Composable
private fun WelcomeShelves(modifier: Modifier = Modifier) {
    Canvas(modifier.clearAndSetSemantics {}) {
        val dp = density
        val rowHeight = 168f * dp
        val plank = 10f * dp
        var top = 24f * dp
        var shelf = 0
        while (top < size.height) {
            var x = 14f * dp
            var i = 0
            while (x < size.width) {
                val w = BOOK_WIDTHS[(i + shelf * 3) % BOOK_WIDTHS.size] * dp
                val h = BOOK_HEIGHTS[(i * 5 + shelf) % BOOK_HEIGHTS.size] * dp
                drawRoundRect(BOOK_COLORS[(i * 3 + shelf * 7) % BOOK_COLORS.size], Offset(x, top + rowHeight - h), Size(w, h), CornerRadius(2f * dp))
                x += w + 4f * dp
                i++
            }
            drawRect(PLANK, Offset(0f, top + rowHeight), Size(size.width, plank))
            top += rowHeight + plank
            shelf++
        }
    }
}
