package vn.shelftap.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import vn.shelftap.app.R
import vn.shelftap.app.data.SyncStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogueScreen(
    viewModel: CatalogueViewModel,
    syncStatus: SyncStatus,
    onRetrySync: () -> Unit,
    onTitleClick: (String) -> Unit,
) {
    val titles = viewModel.titles.collectAsLazyPagingItems()
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        SyncBanner(status = syncStatus, onRetry = onRetrySync)
        TitleList(
            lazyItems = titles,
            onTitleClick = onTitleClick,
            emptyText = stringResource(R.string.catalogue_empty),
            modifier = Modifier.weight(1f),
        )
    }
}
