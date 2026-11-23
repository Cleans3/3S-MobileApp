package shelftap.optlab.corpus.p01

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import shelftap.optlab.RecompositionProbe

// Ported from claude-haiku-B3-detailed-functional-r1 app/src/main/java/vn/shelftap/app/ui/history/HistoryScreen.kt: the LazyColumn of
// HistoryScreen (its items(state.items) call is the P01 finding, no key) and HistoryCard. The ViewModel, switch and dialog around it are
// not part of the construct and are left out; HistoryItem and formatDate come from Shims.kt (copied from data/Models.kt and ui/Common.kt).

@Composable
fun HistoryListA(items: List<HistoryItem>, onOpenTitle: (String) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(items) { item ->
            HistoryCard(item = item, onOpen = { onOpenTitle(item.titleId) })
        }
    }
}

/** B of P01 (tuned): the same list and the same HistoryCard, with a stable key and a content type. */
@Composable
fun HistoryListB(items: List<HistoryItem>, onOpenTitle: (String) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(items, key = { it.titleId }, contentType = { "history" }) { item ->
            HistoryCard(item = item, onOpen = { onOpenTitle(item.titleId) })
        }
    }
}

@Composable
private fun HistoryCard(item: HistoryItem, onOpen: () -> Unit) {
    RecompositionProbe("rc.row")
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("Borrowed ${formatDate(item.outAt)}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = if (item.returnedAt == null) "Not returned yet" else "Returned ${formatDate(item.returnedAt)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
