package vn.shelftap.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class CatalogueState(val titles: List<Title> = emptyList(), val query: String = "", val selected: Title? = null, val loading: Boolean = true, val offline: Boolean = false)

class ShelfTapViewModel(private val repository: CatalogueRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val _state = MutableStateFlow(CatalogueState())
    val state: StateFlow<CatalogueState> = _state.asStateFlow()

    init { viewModelScope.launch {
        query.debounce(250).distinctUntilChanged().collectLatest { value ->
            _state.value = _state.value.copy(query = value, loading = true)
            val cached = repository.cached(value)
            _state.value = _state.value.copy(titles = cached, loading = false)
            runCatching { repository.refresh(value) }.onSuccess { _state.value = _state.value.copy(titles = repository.cached(value), offline = false) }.onFailure { _state.value = _state.value.copy(offline = true) }
        }
    } }
    fun setQuery(value: String) { query.value = value }
    fun open(title: Title) { _state.value = _state.value.copy(selected = title) }
    fun close() { _state.value = _state.value.copy(selected = null) }
}

class ShelfTapViewModelFactory(private val repository: CatalogueRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ShelfTapViewModel(repository) as T
}
