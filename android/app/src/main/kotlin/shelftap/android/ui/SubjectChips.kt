package shelftap.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import shelftap.android.ui.theme.GlassSize
import shelftap.android.ui.theme.Radius
import shelftap.android.ui.theme.Spacing
import shelftap.android.ui.theme.shelfTap

/**
 * The row of subject chips under the search field of the list (the `List` board): "All" first, then the subjects the catalog shows most.
 * A chip is not a new filter on the server: it types its subject into the search field, which already finds books by subject (`q`), so the
 * results, the paging and "back" are the ones of any search. The chip of the current search is the selected one.
 */
@Composable
fun SubjectChipRow(
    subjects: List<String>, selected: String?, allLabel: String, onAll: () -> Unit, onSubject: (String) -> Unit, modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier, contentPadding = PaddingValues(horizontal = GlassSize.page), horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item(key = "all") { Chip(allLabel, selected = selected == null, onClick = onAll) }
        items(subjects, key = { it }) { subject -> Chip(subject, selected = subject.equals(selected, ignoreCase = true), onClick = { onSubject(subject) }) }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.shelfTap
    val fill = if (selected) MaterialTheme.colorScheme.primary else c.glass.copy(alpha = CHIP_FILL)
    val ink = if (selected) MaterialTheme.colorScheme.onPrimary else c.ink
    // The touch target is the full 48 dp row; the chip drawn inside it is 36 dp tall.
    Box(
        Modifier.heightIn(min = Spacing.touch).semantics { this.selected = selected }.pressable(CircleShape, onClick, role = Role.RadioButton),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.clip(CircleShape).background(fill).then(if (selected) Modifier else Modifier.border(BorderStroke(Radius.hairline, c.ink.copy(alpha = CHIP_EDGE)), CircleShape))
                .heightIn(min = CHIP_HEIGHT).padding(horizontal = Spacing.l),
            contentAlignment = Alignment.Center,
        ) { Text(label, style = MaterialTheme.typography.labelLarge, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

private val CHIP_HEIGHT = Spacing.xl + Spacing.s
private const val CHIP_FILL = 0.7f
private const val CHIP_EDGE = 0.12f

