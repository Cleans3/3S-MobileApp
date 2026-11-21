package vn.shelftap.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CatalogueViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = CatalogueRepository(app); var items = androidx.compose.runtime.mutableStateOf<List<Title>>(emptyList()); var query = androidx.compose.runtime.mutableStateOf(""); var selected = androidx.compose.runtime.mutableStateOf<Title?>(null); var syncing = androidx.compose.runtime.mutableStateOf(false); var error = androidx.compose.runtime.mutableStateOf<String?>(null); private var job: Job? = null
    init { searchLocal(); sync() }
    fun searchLocal() { job?.cancel(); job = viewModelScope.launch { delay(120); items.value = repo.local(query.value) } }
    fun sync() { if (syncing.value) return; viewModelScope.launch { syncing.value = true; try { repo.refreshAll(); items.value = repo.local(query.value) } catch (_: Exception) { error.value = "Offline mode: showing saved catalogue" } finally { syncing.value = false } } }
    fun open(id: String) { viewModelScope.launch { selected.value = repo.localById(id) ?: runCatching { repo.fetchOne(id) }.getOrNull() } }
    fun close() { selected.value = null }
}
