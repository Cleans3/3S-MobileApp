package vn.shelftap.app.ui.catalogue

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.data.TitleEntity
import vn.shelftap.app.ui.components.SyncBanner
import vn.shelftap.app.ui.components.TitleList

class CatalogueViewModel(repository: CatalogueRepository) : ViewModel() {
    val titles: Flow<PagingData<TitleEntity>> =
        repository.pagedTitles().cachedIn(viewModelScope)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogueScreen(
    repository: CatalogueRepository,
    onOpenTitle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = viewModel<CatalogueViewModel>(
        factory = viewModelFactory { initializer { CatalogueViewModel(repository) } },
    )
    val pagingItems = viewModel.titles.collectAsLazyPagingItems()

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Catalogue") })
        SyncBanner(repository = repository)
        TitleList(
            pagingItems = pagingItems,
            emptyText = "No titles yet. Connect to the internet to download the catalogue.",
            onOpenTitle = onOpenTitle,
        )
    }
}
