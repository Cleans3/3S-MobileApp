package vn.shelftap.app.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.data.TitleEntity
import vn.shelftap.app.ui.components.SyncBanner
import vn.shelftap.app.ui.components.TitleList

class SearchViewModel(private val repository: CatalogueRepository) : ViewModel() {

    private val queryState = MutableStateFlow("")
    val query: StateFlow<String> = queryState.asStateFlow()

    /** Results update as the member types, with a short pause so each keystroke does not query. */
    @OptIn(FlowPreview::class)
    val results: Flow<PagingData<TitleEntity>> = queryState
        .debounce(SEARCH_DEBOUNCE_MS)
        .distinctUntilChanged()
        .flatMapLatest { repository.pagedSearch(it) }
        .cachedIn(viewModelScope)

    fun onQueryChange(text: String) {
        queryState.value = text
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 200L
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    repository: CatalogueRepository,
    onOpenTitle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = viewModel<SearchViewModel>(
        factory = viewModelFactory { initializer { SearchViewModel(repository) } },
    )
    val query by viewModel.query.collectAsState()
    val pagingItems = viewModel.results.collectAsLazyPagingItems()

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Search") })
        SyncBanner(repository = repository)
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            label = { Text("Title, author, ISBN or subject") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        TitleList(
            pagingItems = pagingItems,
            emptyText = "No books found",
            onOpenTitle = onOpenTitle,
        )
    }
}
