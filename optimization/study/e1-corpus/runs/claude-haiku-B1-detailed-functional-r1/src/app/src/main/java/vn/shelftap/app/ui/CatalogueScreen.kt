@file:OptIn(ExperimentalMaterial3Api::class)

package vn.shelftap.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import vn.shelftap.app.model.Title

class CatalogueViewModel(repository: CatalogueRepository) : ViewModel() {
    val titles: Flow<PagingData<Title>> = repository.allTitles().cachedIn(viewModelScope)
}

/** Every title, A to Z, one row each. */
@Composable
fun CatalogueScreen(repository: CatalogueRepository, onOpen: (String) -> Unit) {
    val viewModel: CatalogueViewModel = viewModel(
        factory = viewModelFactory { initializer { CatalogueViewModel(repository) } },
    )
    val paging = viewModel.titles.collectAsLazyPagingItems()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Catalogue") }) },
    ) { padding ->
        TitleList(
            paging = paging,
            emptyMessage = "No titles in the catalogue yet.",
            onOpen = onOpen,
            modifier = Modifier.padding(padding),
        )
    }
}
