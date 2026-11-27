package shelftap.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import shelftap.android.R
import shelftap.android.ui.icons.LucideIcon
import shelftap.android.ui.icons.LucideIcons
import shelftap.android.ui.theme.Radius
import shelftap.android.ui.theme.Spacing
import shelftap.android.ui.theme.pressScale
import shelftap.android.ui.theme.shelfTap

private val PILL_HEIGHT = 56.dp
private val CIRCLE = 40.dp
private const val CIRCLE_FILL = 0.16f
private val LOGIN_HEIGHT = 40.dp
private const val NOTE_EDGE = 0.2f

/**
 * What a guest sees in the place of the floating "Tap a book to borrow" pill (the board `Guest`): a pill in the primary colour that says why
 * borrowing is out of reach and takes the guest to the login. A guest cannot borrow or reserve, so there is nothing else for the pill to do.
 */
@Composable
fun GuestLoginPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.shelfTap
    val source = remember { MutableInteractionSource() }
    Row(
        modifier.defaultMinSize(minHeight = PILL_HEIGHT).pressScale(source).clip(CircleShape).background(c.primary)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .padding(start = Spacing.s, end = Spacing.l + Spacing.xs / 2, top = Spacing.s, bottom = Spacing.s),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(CIRCLE).clip(CircleShape).background(c.onPrimary.copy(alpha = CIRCLE_FILL)), contentAlignment = Alignment.Center) {
            LucideIcon(LucideIcons.LogIn, tint = c.onPrimary, size = Spacing.l + Spacing.xs)
        }
        Text(stringResource(R.string.guest_pill), style = MaterialTheme.typography.labelLarge, color = c.onPrimary)
    }
}

/** The "Log in" button on the right of a guest's top bar: a 40 dp pill in the primary colour, with a touch target of 48 dp. */
@Composable
fun LoginPill(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.shelfTap
    val source = remember { MutableInteractionSource() }
    Box(modifier.defaultMinSize(minHeight = Spacing.touch).pressScale(source).clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(
            label, style = MaterialTheme.typography.titleSmall, color = c.onPrimary,
            modifier = Modifier.defaultMinSize(minHeight = LOGIN_HEIGHT).clip(CircleShape).background(c.primary).padding(horizontal = Spacing.m + Spacing.xs / 2).wrapVertical(),
        )
    }
}

/** Puts the label in the middle of the 40 dp pill. */
private fun Modifier.wrapVertical(): Modifier = this.padding(vertical = Spacing.s)

/** The note under a guest's search field: what a guest can and cannot do, in a dashed box (the board's). */
@Composable
fun GuestNote(modifier: Modifier = Modifier) {
    val c = MaterialTheme.shelfTap
    Text(
        stringResource(R.string.guest_note), style = MaterialTheme.typography.bodyMedium, color = c.ink2,
        modifier = modifier.fillMaxWidth().drawBehind {
            val r = Radius.card.toPx()
            drawRoundRect(c.ink.copy(alpha = NOTE_EDGE), cornerRadius = CornerRadius(r), style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))))
        }.padding(horizontal = Spacing.l, vertical = Spacing.m + Spacing.xs / 2),
    )
}
