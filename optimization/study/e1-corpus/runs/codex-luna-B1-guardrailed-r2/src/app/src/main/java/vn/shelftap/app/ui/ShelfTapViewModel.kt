package vn.shelftap.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.data.TitleEntity

class ShelfTapViewModel(private val repository: CatalogueRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    val queryFlow = query.asStateFlow()
    private val _selectedId = MutableStateFlow<String?>(null)
    val selectedId: StateFlow<String?> = _selectedId.asStateFlow()
    private var nextPage = 0
    private var loading = false
    private var pageJob: Job? = null

    val titles = query
        .debounce(350)
        .distinctUntilChanged()
        .flatMapLatest { text -> if (text.isBlank()) repository.catalogue() else repository.search(text) }
        .cachedIn(viewModelScope)

    init { loadNextPage() }

    fun setQuery(value: String) {
        if (query.value != value) {
            query.value = value
            nextPage = 0
            pageJob?.cancel()
            pageJob = viewModelScope.launch {
                if (value.isBlank()) repository.loadPage(0) else repository.loadPage(0, value)
                nextPage = 1
            }
        }
    }

    fun loadNextPage() {
        if (loading) return
        loading = true
        val current = query.value
        pageJob = viewModelScope.launch {
            repository.loadPage(nextPage, current)
            nextPage++
            loading = false
        }
    }

    fun open(id: String) { _selectedId.value = id }
    fun closeDetail() { _selectedId.value = null }
    fun observeTitle(id: String) = repository.observeTitle(id)
    fun refreshDetail(id: String) { viewModelScope.launch { repository.loadTitle(id) } }

    class Factory(private val repository: CatalogueRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ShelfTapViewModel(repository) as T
    }
}
