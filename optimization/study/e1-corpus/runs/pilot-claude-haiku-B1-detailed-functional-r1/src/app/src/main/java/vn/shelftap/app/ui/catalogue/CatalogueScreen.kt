package vn.shelftap.app.ui.catalogue

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import androidx.paging.cachedIn
import vn.shelftap.app.R
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.data.SyncState
import vn.shelftap.app.data.db.TitleEntity
import vn.shelftap.app.ui.common.TitleList
import vn.shelftap.app.ui.common.simpleFactory
import androidx.compose.foundation.layout.Column as ColumnAlias
import androidx.compose.runtime.collectAsState

class CatalogueViewModel(private val repository: CatalogueRepository) : ViewModel() {
    val titles: Flow<PagingData<TitleEntity>> = repository.catalogue().cachedIn(viewModelScope)
    val sync: StateFlow<SyncState> = repository.sync

    fun refresh() = repository.startSync()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogueScreen(
    repository: CatalogueRepository,
    onOpenSearch: () -> Unit,
    onOpenTitle: (String) -> Unit,
) {
    val viewModel = viewModel<CatalogueViewModel>(
        factory = simpleFactory { CatalogueViewModel(repository) },
    )
    val paging = viewModel.titles.collectAsLazyPagingItems()
    val sync by viewModel.sync.collectAsState()
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.catalogue_title)) },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.action_search))
                    }
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_refresh))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            SyncStatus(state = sync)
            TitleList(
                paging = paging,
                listState = listState,
                emptyMessage = stringResource(R.string.catalogue_empty),
                onOpenTitle = onOpenTitle,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SyncStatus(state: SyncState) {
    when (state) {
        is SyncState.Running -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.sync_progress, state.done, state.total),
                style = MaterialTheme.typography.bodySmall,
            )
            if (state.total > 0) {
                LinearProgressIndicator(
                    progress = { state.done.toFloat() / state.total },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
        SyncState.Failed -> Text(
            text = stringResource(R.string.sync_failed),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp),
        )
        else -> Unit
    }
}
