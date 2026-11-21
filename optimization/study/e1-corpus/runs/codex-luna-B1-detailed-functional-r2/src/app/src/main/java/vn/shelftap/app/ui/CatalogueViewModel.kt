package vn.shelftap.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.data.Title

class CatalogueViewModel(private val repository: CatalogueRepository) : ViewModel() {
    val query = MutableStateFlow("")
    val titles: StateFlow<List<Title>> = query.flatMapLatest(repository::observe)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing

    init { sync() }
    fun sync() { if (_syncing.value) return; viewModelScope.launch { _syncing.value = true; runCatching { repository.syncAll() }; _syncing.value = false } }
    fun title(id: String) = repository.observeById(id)
    fun fetchMissing(id: String) { viewModelScope.launch { repository.fetchMissing(id) } }
}

class CatalogueViewModelFactory(private val repository: CatalogueRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = CatalogueViewModel(repository) as T
}
