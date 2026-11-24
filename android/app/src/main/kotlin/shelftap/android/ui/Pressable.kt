package shelftap.android.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import shelftap.android.ui.theme.PRESS_SCALE_CARD
import shelftap.android.ui.theme.PRESS_SCALE_ROW
import shelftap.android.ui.theme.pressScale

/**
 * A tappable control the way the mockup presses it: it shrinks (0.96; [row] 0.98 for a full-width row; not at all when [shrink] is
 * false, or on the light tier), clipped to [shape], with the soft veil of the app's indication inside the clip, so neither the veil nor
 * the shrink is ever a square around a round control. Put it in the chain where the control's own size is already set.
 */
@Composable
fun Modifier.pressable(
    shape: Shape, onClick: () -> Unit, role: Role? = Role.Button, enabled: Boolean = true, shrink: Boolean = true, row: Boolean = false,
): Modifier {
    val source = remember { MutableInteractionSource() }
    return this
        .then(if (shrink) Modifier.pressScale(source, if (row) PRESS_SCALE_ROW else PRESS_SCALE_CARD) else Modifier)
        .clip(shape)
        .clickable(interactionSource = source, indication = LocalIndication.current, enabled = enabled, role = role, onClick = onClick)
}
