package vn.shelftap.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.shelftap.app.data.*

data class ListState<T>(val items: List<T> = emptyList(), val loading: Boolean = true, val offline: Boolean = false, val error: String? = null)
class ShelfTapViewModel(private val repo: ShelfTapRepository) : ViewModel() {
    private val _loans = MutableStateFlow(ListState<Loan>()); val loans: StateFlow<ListState<Loan>> = _loans.asStateFlow()
    private val _history = MutableStateFlow(ListState<HistoryItem>()); val history: StateFlow<ListState<HistoryItem>> = _history.asStateFlow()
    private val _member = MutableStateFlow<Member?>(null); val member: StateFlow<Member?> = _member.asStateFlow()
    private val _detail = MutableStateFlow<TitleDetail?>(null); val detail: StateFlow<TitleDetail?> = _detail.asStateFlow()
    init { refresh() }
    fun refresh() { viewModelScope.launch { _loans.value = runCatching { repo.loans() }.fold({ ListState(it.value, false, it.offline) }, { ListState(loading = false, error = "Unable to load loans") }); _history.value = runCatching { repo.history() }.fold({ ListState(it.value, false, it.offline) }, { ListState(loading = false, error = "Unable to load history") }); _member.value = runCatching { repo.member() }.getOrNull() } }
    fun renew(id: String) { viewModelScope.launch { runCatching { repo.renew(id) }.onSuccess { updated -> _loans.value = _loans.value.copy(items = _loans.value.items.map { if (it.loanId == id) updated else it }) }.onFailure { if (it is ApiException && it.status == 409) _loans.value = _loans.value.copy(error = "This loan cannot be renewed") } } }
    fun setHistory(enabled: Boolean) { viewModelScope.launch { runCatching { repo.setHistory(enabled) }.onSuccess { _member.value = it }.onFailure { } } }
    fun loadTitle(id: String) { _detail.value = null; viewModelScope.launch { _detail.value = runCatching { repo.title(id) }.getOrNull() } }
}
class ShelfTapViewModelFactory(private val repo: ShelfTapRepository) : ViewModelProvider.Factory { override fun <T : ViewModel> create(modelClass: Class<T>): T = ShelfTapViewModel(repo) as T }
