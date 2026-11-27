package shelftap.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import shelftap.android.ui.theme.AdaptiveSize
import shelftap.android.ui.theme.GlassSize
import shelftap.android.ui.theme.Radius
import shelftap.android.ui.theme.Spacing
import shelftap.android.ui.theme.shelfTap

/**
 * A list with the detail of the chosen item beside it, in a pane at least [AdaptiveSize.twoPaneMin] wide (the boards `Wide*`: the list on the
 * left, the item's own card on the right). [list] is told whether it is drawn beside a detail, so its rows can show which one is chosen;
 * in a narrower pane only the list is drawn and the detail is not composed at all.
 */
@Composable
fun TwoPane(detailTitle: String, list: @Composable (twoPane: Boolean) -> Unit, detail: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth < AdaptiveSize.twoPaneMin) {
            list(false)
        } else {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxHeight()) { list(true) }
                Box(Modifier.weight(DETAIL_WEIGHT).fillMaxHeight().padding(pagePadding(Spacing.none, Spacing.s)).padding(end = GlassSize.page, start = Spacing.xs)) {
                    DetailPanel(detailTitle, detail)
                }
            }
        }
    }
}

private const val DETAIL_WEIGHT = 1.15f
private const val PANEL_FILL = 0.7f

/** The glass panel of the detail: its small title, then the content, scrolling when it is taller than the panel. */
@Composable
private fun DetailPanel(title: String, content: @Composable () -> Unit) {
    val c = MaterialTheme.shelfTap
    val shape = RoundedCornerShape(Radius.sheet)
    Column(
        Modifier.fillMaxSize().clip(shape).background(c.glass.copy(alpha = PANEL_FILL)).border(BorderStroke(Radius.hairline, c.edge.copy(alpha = 0.9f)), shape)
            .verticalScroll(rememberScrollState()).padding(Spacing.l),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.m),
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = c.ink3, modifier = Modifier.semantics { heading() })
        content()
    }
}
