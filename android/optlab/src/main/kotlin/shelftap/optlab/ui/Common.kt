package shelftap.optlab.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import shelftap.optlab.RecompositionProbe
import shelftap.optlab.data.Book
import shelftap.optlab.data.BookGenerator

fun coverColor(idx: Int) = Color.hsv(idx * 360f / BookGenerator.COVERS, 0.55f, 0.85f)

/** Marks the first frame the user can use, for StartupTimingMetric's TTFD. The invalidate guarantees one more frame after the report, which the metric needs on a static screen. */
@Composable
fun FullyDrawn(ready: Boolean = true) {
    val activity = LocalActivity.current
    val view = LocalView.current
    LaunchedEffect(ready) {
        if (ready) {
            activity?.reportFullyDrawn()
            view.invalidate()
        }
    }
}

/** The one plain row every list-shaped factor shares, so row cost is identical in A and B. */
@Composable
fun BookRowPlain(book: Book) {
    RecompositionProbe("rc.row")
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp, 60.dp).background(coverColor(book.coverIdx)))
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${book.author} · ${book.year}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
