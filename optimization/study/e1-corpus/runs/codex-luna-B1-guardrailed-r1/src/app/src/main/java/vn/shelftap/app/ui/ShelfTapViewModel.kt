package vn.shelftap.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.data.Title

data class ShelfTapState(
    val query: String = "",
    val titles: List<Title> = emptyList(),
    val selected: Title? = null,
    val loading: Boolean = false,
    val hasMore: Boolean = true,
    val offline: Boolean = false
)

sealed interface ShelfTapAction {
    data class QueryChanged(val value: String) : ShelfTapAction
    data class OpenTitle(val id: String) : ShelfTapAction
    data object Back : ShelfTapAction
    data object LoadMore : ShelfTapAction
}

class ShelfTapViewModel(private val repository: CatalogueRepository) : ViewModel() {
    private val _state = MutableStateFlow(ShelfTapState())
    val state: StateFlow<ShelfTapState> = _state.asStateFlow()
    private var queryJob: Job? = null
    private var page = 0
    private val pageSize = 50

    init { load(reset = true) }

    fun onAction(action: ShelfTapAction) {
        when (action) {
            is ShelfTapAction.QueryChanged -> {
                _state.value = _state.value.copy(query = action.value)
                queryJob?.cancel()
                queryJob = viewModelScope.launch { delay(250); load(reset = true) }
            }
            is ShelfTapAction.OpenTitle -> viewModelScope.launch {
                _state.value = _state.value.copy(loading = true)
                _state.value = _state.value.copy(selected = repository.detail(action.id), loading = false)
            }
            ShelfTapAction.Back -> _state.value = _state.value.copy(selected = null)
            ShelfTapAction.LoadMore -> if (!_state.value.loading && _state.value.hasMore) load(reset = false)
        }
    }

    private fun load(reset: Boolean) {
        if (!reset && _state.value.loading) return
        if (reset) page = 0
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, titles = if (reset) emptyList() else _state.value.titles)
            val currentPage = page
            val query = _state.value.query
            val result = repository.page(query, currentPage, pageSize)
            val previous = if (reset) emptyList() else _state.value.titles
            page = currentPage + 1
            _state.value = _state.value.copy(
                titles = previous + result,
                loading = false,
                hasMore = result.size == pageSize,
                offline = result.isNotEmpty() && result.size < pageSize
            )
        }
    }
}

class ShelfTapViewModelFactory(private val repository: CatalogueRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ShelfTapViewModel(repository) as T
}
