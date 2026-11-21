package vn.shelftap.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

data class ShelfState(val loans: List<Loan> = emptyList(), val history: List<HistoryItem> = emptyList(), val member: Member? = null, val offlineLoans: Boolean = false, val offlineHistory: Boolean = false, val loading: Boolean = true, val message: String? = null, val detail: TitleDetail? = null)

class ShelfViewModel(private val repo: ShelfRepository = ShelfTapGraph.repository) : ViewModel() {
    var state = androidx.compose.runtime.mutableStateOf(ShelfState()); private set
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        state.value = state.value.copy(loading = true, message = null)
        val l = async { repo.loans() }; val h = async { repo.history() }; val m = async { runCatching { repo.member() }.getOrNull() }
        val loans = runCatching { l.await() }.getOrElse { emptyList<Loan>() to true }; val history = runCatching { h.await() }.getOrElse { emptyList<HistoryItem>() to true }
        state.value = state.value.copy(loans = loans.first, history = history.first.sortedByDescending { it.outAt }, offlineLoans = loans.second, offlineHistory = history.second, member = m.await(), loading = false)
    }
    fun renew(loan: Loan) = viewModelScope.launch { try { val updated = repo.renew(loan.loanId); state.value = state.value.copy(loans = state.value.loans.map { if (it.loanId == updated.loanId) updated else it }, message = null) } catch (e: ShelfRepository.ApiException) { state.value = state.value.copy(message = if (e.code == "RENEWAL_NOT_ALLOWED") "This loan cannot be renewed" else "Unable to renew this loan") } catch (_: Exception) { state.value = state.value.copy(message = "Unable to renew this loan") } }
    fun loadDetail(id: String) = viewModelScope.launch { state.value = state.value.copy(detail = null); runCatching { repo.title(id) }.onSuccess { state.value = state.value.copy(detail = it) }.onFailure { state.value = state.value.copy(message = "Unable to load book details") } }
    fun setHistory(enabled: Boolean) = viewModelScope.launch { runCatching { repo.setHistory(enabled) }.onSuccess { state.value = state.value.copy(member = it) }.onFailure { state.value = state.value.copy(message = "Unable to update reading history") } }
    fun clearMessage() { state.value = state.value.copy(message = null) }
}
