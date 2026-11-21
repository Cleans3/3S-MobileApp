package vn.shelftap.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Spacer
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import vn.shelftap.app.R
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.data.SyncState
import vn.shelftap.app.model.Title

class CatalogueViewModel(private val repository: CatalogueRepository) : ViewModel() {
    val titles: Flow<PagingData<Title>> = repository.pagingAll().cachedIn(viewModelScope)
    val sync: StateFlow<SyncState> = repository.sync

    fun refresh() {
        repository.startSyncIfNeeded(force = true)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogueScreen(repository: CatalogueRepository, onOpenTitle: (String) -> Unit) {
    val vm = viewModel<CatalogueViewModel>(
        factory = viewModelFactory { initializer { CatalogueViewModel(repository) } },
    )
    val paged = vm.titles.collectAsLazyPagingItems()
    val sync by vm.sync.collectAsStateWithLifecycle()
    val isEmpty = paged.itemCount == 0 && paged.loadState.refresh is LoadState.NotLoading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.catalogue_title)) },
                actions = {
                    IconButton(
                        onClick = { vm.refresh() },
                        enabled = sync !is SyncState.Running,
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (isEmpty) {
                EmptyCatalogue(sync = sync, onRetry = { vm.refresh() })
            } else {
                SyncStatus(sync)
                TitleList(
                    paged = paged,
                    onOpen = onOpenTitle,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun EmptyCatalogue(sync: SyncState, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (sync) {
            is SyncState.Running -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                if (sync.total > 0) {
                    Text(
                        stringResource(R.string.downloading_catalogue, sync.done, sync.total),
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Text(stringResource(R.string.downloading_catalogue_start), textAlign = TextAlign.Center)
                }
            }
            is SyncState.Failed -> {
                Text(stringResource(R.string.download_failed), textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
            }
            else -> {
                Text(stringResource(R.string.catalogue_empty), textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
            }
        }
    }
}

@Composable
private fun SyncStatus(sync: SyncState) {
    when (sync) {
        is SyncState.Running -> Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (sync.total > 0) {
                LinearProgressIndicator(
                    progress = { sync.done.toFloat() / sync.total },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.downloading_catalogue, sync.done, sync.total),
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
        is SyncState.Failed -> Text(
            stringResource(R.string.update_failed),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp),
        )
        else -> Unit
    }
}
