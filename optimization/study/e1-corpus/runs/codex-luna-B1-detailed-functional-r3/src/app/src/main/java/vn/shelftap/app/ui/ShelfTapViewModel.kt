package vn.shelftap.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.data.TitleDto

class ShelfTapViewModel(private val repository: CatalogueRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    val search: StateFlow<String> = query
    val titles = query.flatMapLatest { repository.titles(it, viewModelScope) }.cachedIn(viewModelScope)
    private val _syncing = MutableStateFlow(true)
    val syncing: StateFlow<Boolean> = _syncing

    init { refresh() }

    fun setSearch(value: String) { query.value = value }

    private fun refresh() = viewModelScope.launch {
        _syncing.value = true
        repository.refreshEntireCatalogue()
        _syncing.value = false
    }

    fun title(id: String): StateFlow<TitleDto?> = repository.title(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
