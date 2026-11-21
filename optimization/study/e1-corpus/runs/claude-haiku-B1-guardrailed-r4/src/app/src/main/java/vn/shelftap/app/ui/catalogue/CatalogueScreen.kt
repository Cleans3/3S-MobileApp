package vn.shelftap.app.ui.catalogue

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import vn.shelftap.app.R
import vn.shelftap.app.ui.components.TitleList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogueScreen(
    onTitleClick: (String) -> Unit,
    viewModel: CatalogueViewModel = viewModel(),
) {
    val pagingItems = viewModel.titles.collectAsLazyPagingItems()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_catalogue)) }) },
    ) { innerPadding ->
        Box(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when {
                pagingItems.itemCount > 0 -> TitleList(pagingItems, onTitleClick)
                pagingItems.loadState.refresh is LoadState.Loading -> CircularProgressIndicator()
                else -> Text(
                    text = stringResource(R.string.catalogue_empty),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
    }
}
