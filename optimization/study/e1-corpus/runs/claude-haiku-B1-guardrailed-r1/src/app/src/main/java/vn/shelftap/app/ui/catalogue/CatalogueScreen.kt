package vn.shelftap.app.ui.catalogue

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import vn.shelftap.app.R
import vn.shelftap.app.data.SyncStatus
import vn.shelftap.app.ui.components.LoadingIndicator
import vn.shelftap.app.ui.components.PagedTitleList
import vn.shelftap.app.ui.components.StatusMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogueScreen(
    viewModel: CatalogueViewModel,
    onTitleClick: (String) -> Unit,
    onSearchClick: () -> Unit,
) {
    val titles = viewModel.titles.collectAsLazyPagingItems()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.catalogue_title)) },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(R.string.action_search),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SyncBanner(status = syncStatus, onRetry = viewModel::retrySync)

            val refresh = titles.loadState.refresh
            when {
                refresh is LoadState.Loading && titles.itemCount == 0 ->
                    LoadingIndicator(Modifier.weight(1f))

                refresh is LoadState.Error && titles.itemCount == 0 ->
                    StatusMessage(stringResource(R.string.catalogue_error), Modifier.weight(1f))

                titles.itemCount == 0 ->
                    StatusMessage(stringResource(R.string.catalogue_empty), Modifier.weight(1f))

                else -> PagedTitleList(
                    titles = titles,
                    onTitleClick = onTitleClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SyncBanner(status: SyncStatus, onRetry: () -> Unit) {
    when (status) {
        is SyncStatus.Running -> Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.sync_running, status.loaded, status.total),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (status.total > 0) {
                LinearProgressIndicator(
                    progress = { status.loaded.toFloat() / status.total },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            }
        }

        SyncStatus.Failed -> Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.sync_failed),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.action_retry))
            }
        }

        SyncStatus.Idle, SyncStatus.Complete -> Unit
    }
}
