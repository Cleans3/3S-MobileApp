package vn.shelftap.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import vn.shelftap.app.data.ShelfTapRepository
import vn.shelftap.app.model.*

data class LoansUiState(val items: List<Loan> = emptyList(), val loading: Boolean = true, val saved: Boolean = false, val error: String? = null, val renewing: String? = null, val renewMessage: String? = null)
class LoansViewModel(private val repo: ShelfTapRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoansUiState())
    val state: StateFlow<LoansUiState> = _state.asStateFlow()
    init { refresh() }
    fun refresh() { viewModelScope.launch { _state.value = _state.value.copy(loading = true, error = null); val result = repo.loans(); _state.value = _state.value.copy(items = result.first, loading = false, saved = result.second, error = if (result.first.isEmpty() && result.second) "No saved loans yet" else null) } }
    fun renew(loanId: String) { viewModelScope.launch { _state.value = _state.value.copy(renewing = loanId, renewMessage = null); try { val updated = repo.renew(loanId); _state.value = _state.value.copy(items = _state.value.items.map { if (it.loanId == loanId) updated else it }, renewing = null) } catch (e: HttpException) { _state.value = _state.value.copy(renewing = null, renewMessage = if (e.code() == 409) "This loan cannot be renewed" else "Unable to renew this loan") } catch (_: Exception) { _state.value = _state.value.copy(renewing = null, renewMessage = "Unable to renew this loan") } } }
    fun clearMessage() { _state.value = _state.value.copy(renewMessage = null) }
    class Factory(private val repo: ShelfTapRepository) : ViewModelProvider.Factory { override fun <T : ViewModel> create(modelClass: Class<T>): T = LoansViewModel(repo) as T }
}

data class HistoryUiState(val items: List<HistoryItem> = emptyList(), val historyEnabled: Boolean = true, val loading: Boolean = true, val saved: Boolean = false, val error: String? = null, val updating: Boolean = false)
class HistoryViewModel(private val repo: ShelfTapRepository) : ViewModel() {
    private val _state = MutableStateFlow(HistoryUiState()); val state = _state.asStateFlow()
    init { refresh() }
    fun refresh() { viewModelScope.launch { _state.value = _state.value.copy(loading = true, error = null); try { val member = repo.member(); val result = repo.history(); _state.value = _state.value.copy(items = result.first, historyEnabled = member.historyEnabled, loading = false, saved = result.second, error = if (result.first.isEmpty() && result.second) "No saved history yet" else null) } catch (_: Exception) { val result = repo.history(); _state.value = _state.value.copy(items = result.first, loading = false, saved = result.second, error = if (result.first.isEmpty() && result.second) "No saved history yet" else null) } } }
    fun updatePrivacy(enabled: Boolean) { viewModelScope.launch { _state.value = _state.value.copy(updating = true); try { val member = repo.updatePrivacy(enabled); _state.value = _state.value.copy(historyEnabled = member.historyEnabled, updating = false) } catch (_: Exception) { _state.value = _state.value.copy(updating = false) } } }
    class Factory(private val repo: ShelfTapRepository) : ViewModelProvider.Factory { override fun <T : ViewModel> create(modelClass: Class<T>): T = HistoryViewModel(repo) as T }
}
