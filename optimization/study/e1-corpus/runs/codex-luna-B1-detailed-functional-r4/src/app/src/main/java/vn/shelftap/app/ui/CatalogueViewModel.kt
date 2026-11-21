package vn.shelftap.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.model.Title

data class CatalogueState(val query: String = "", val items: List<Title> = emptyList(), val syncing: Boolean = false, val loaded: Int = 0, val total: Int = 0)

class CatalogueViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CatalogueRepository(application)
    private val _state = MutableStateFlow(CatalogueState())
    val state: StateFlow<CatalogueState> = _state.asStateFlow()
    private var searchJob: Job? = null

    init { refresh(); search("") }

    fun search(value: String) {
        _state.value = _state.value.copy(query = value)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(120)
            _state.value = _state.value.copy(items = repository.local(value))
        }
    }

    fun refresh() = viewModelScope.launch {
        _state.value = _state.value.copy(syncing = true)
        runCatching { repository.refresh { done, total -> _state.value = _state.value.copy(loaded = done, total = total) } }
        _state.value = _state.value.copy(syncing = false, items = repository.local(_state.value.query))
    }
    suspend fun detail(id: String) = repository.detail(id)
}
