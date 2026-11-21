package vn.shelftap.app.ui.catalogue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import vn.shelftap.app.R
import vn.shelftap.app.data.SyncState
import vn.shelftap.app.ui.common.TitleRow

@Composable
fun CatalogueScreen(
    viewModel: CatalogueViewModel,
    onOpenTitle: (String) -> Unit
) {
    val pagingItems = viewModel.titles.collectAsLazyPagingItems()
    val sync by viewModel.syncState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxWidth()) {
        SyncBanner(state = sync, onRetry = viewModel::retrySync)

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(
                count = pagingItems.itemCount,
                key = pagingItems.itemKey { it.id },
                contentType = pagingItems.itemContentType { "title" }
            ) { index ->
                val title = pagingItems[index]
                if (title != null) {
                    TitleRow(title = title, onClick = { onOpenTitle(title.id) })
                    HorizontalDivider()
                }
            }
        }
    }
}

/** Shows download progress, or a retry button when the download is stuck (for example, offline). */
@Composable
fun SyncBanner(state: SyncState, onRetry: () -> Unit) {
    when (state) {
        SyncState.Idle, SyncState.Complete -> Unit
        is SyncState.Running -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.sync_running, state.done, state.total),
                style = MaterialTheme.typography.bodyMedium
            )
            val fraction = if (state.total > 0) state.done.toFloat() / state.total else 0f
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
        SyncState.Failed -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.sync_failed),
                style = MaterialTheme.typography.bodyMedium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(stringResource(R.string.retry))
                }
            }
        }
    }
}
