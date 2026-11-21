package vn.shelftap.app.ui.catalogue

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import vn.shelftap.app.R
import vn.shelftap.app.data.repository.SyncState
import vn.shelftap.app.ui.catalogue.components.SearchBarComponent
import vn.shelftap.app.ui.catalogue.components.TitleItemRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogueScreen(
    viewModel: CatalogueViewModel,
    onTitleClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val titles = viewModel.titlesPagingData.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }

    val syncFailedMsg = stringResource(R.string.sync_failed)
    LaunchedEffect(syncState) {
        if (syncState is SyncState.Error) {
            snackbarHostState.showSnackbar(syncFailedMsg)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    // Rule 20: Icon-only button has meaningful contentDescription from strings.xml
                    // Rule 21: Clickable element is at least 48dp by 48dp
                    IconButton(
                        onClick = { viewModel.onDownloadCatalogueClicked() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.cd_sync_catalogue),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search field above the list
            SearchBarComponent(
                query = searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                onClearQuery = viewModel::onClearSearch
            )

            // Progress indicator during catalogue sync
            when (val state = syncState) {
                is SyncState.Syncing -> {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                    Text(
                        text = stringResource(R.string.syncing_catalogue, state.downloadedCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                }
                else -> { /* idle or completed */ }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                val isRefreshing = titles.loadState.refresh is LoadState.Loading

                if (isRefreshing && titles.itemCount == 0) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (titles.itemCount == 0 && !isRefreshing) {
                    // Empty result shows message "No books found"
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_books_found),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Rule 11: Stable, unique key for every item
                    // Rule 12: Set contentType on lazy list items
                    // Rule 13: Lazy layout for long/unbounded collection
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("catalogue_list"),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(
                            count = titles.itemCount,
                            key = titles.itemKey { it.id },
                            contentType = titles.itemContentType { "title_item" }
                        ) { index ->
                            val title = titles[index]
                            if (title != null) {
                                TitleItemRow(
                                    title = title,
                                    onClick = { onTitleClick(title.id) }
                                )
                            }
                        }

                        if (titles.loadState.append is LoadState.Loading) {
                            item(
                                key = "loading_append_indicator",
                                contentType = "loading_indicator"
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
