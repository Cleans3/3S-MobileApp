package vn.shelftap.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import vn.shelftap.app.data.CatalogueApi
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.data.Title
import vn.shelftap.app.data.TitleDatabase
import okhttp3.OkHttpClient

class CatalogueViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CatalogueRepository(TitleDatabase.create(application).titles(), CatalogueApi(OkHttpClient()), Dispatchers.IO)
    private val query = MutableStateFlow("")
    val search: StateFlow<String> = query
    val titles = query.debounce(250).flatMapLatest { repository.observe(it) }.map { it.map { entity -> entity.asDomain() } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _selected = MutableStateFlow<Title?>(null)
    val selected: StateFlow<Title?> = _selected
    init { viewModelScope.launch(Dispatchers.IO) { runCatching { repository.syncIfNeeded() } } }
    fun setSearch(value: String) { query.value = value }
    fun open(id: String) { viewModelScope.launch { _selected.value = repository.get(id) } }
    fun closeDetail() { _selected.value = null }
}
