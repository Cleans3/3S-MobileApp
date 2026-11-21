package vn.shelftap.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONException
import vn.shelftap.app.data.TitleRepository
import vn.shelftap.app.model.Title
import java.io.IOException

class CatalogueViewModel(repository: TitleRepository) : ViewModel() {
    val titles: Flow<PagingData<Title>> = repository.pageAll().cachedIn(viewModelScope)
}

class SearchViewModel(private val repository: TitleRepository) : ViewModel() {

    private val _query = MutableStateFlow("")

    /** The text field shows this immediately; only the search itself waits for the debounce. */
    val query: StateFlow<String> = _query.asStateFlow()

    /** Each new query cancels the previous one (flatMapLatest); the search runs on the local index. */
    val results: Flow<PagingData<Title>> = _query
        .debounce(SEARCH_DEBOUNCE_MS)
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { repository.search(it) }
        .cachedIn(viewModelScope)

    fun onQueryChange(text: String) {
        _query.value = text
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 250L
    }
}

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Loaded(val title: Title) : DetailUiState
    data object NotFound : DetailUiState
    data object Unavailable : DetailUiState
}

class DetailViewModel(
    private val repository: TitleRepository,
    private val titleId: String,
) : ViewModel() {

    private val _state = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                repository.getTitle(titleId)?.let { DetailUiState.Loaded(it) } ?: DetailUiState.NotFound
            } catch (e: IOException) {
                DetailUiState.Unavailable
            } catch (e: JSONException) {
                DetailUiState.Unavailable
            }
        }
    }
}

/** Builds a ViewModel from a lambda, so screens can pass their constructor arguments. */
class SimpleViewModelFactory(private val creator: () -> ViewModel) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
}
