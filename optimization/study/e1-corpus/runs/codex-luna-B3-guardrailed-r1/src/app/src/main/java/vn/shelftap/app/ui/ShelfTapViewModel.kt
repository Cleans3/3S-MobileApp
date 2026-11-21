package vn.shelftap.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.shelftap.app.data.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class ShelfState(val loans: List<Loan> = emptyList(), val history: List<HistoryItem> = emptyList(), val member: Member? = null, val detail: BookDetail? = null, val loading: Boolean = true, val saved: Boolean = false, val message: String? = null)

class ShelfTapViewModel(private val api: ShelfTapApi, private val cache: EncryptedCache, private val io: CoroutineDispatcher = Dispatchers.IO) : ViewModel() {
    private val _state = MutableStateFlow(ShelfState())
    val state: StateFlow<ShelfState> = _state.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch(io) {
        _state.value = _state.value.copy(loading = true, message = null)
        try { val l = api.loans(); val h = api.history(); cache.save(l, h); _state.value = _state.value.copy(loans = l, history = h, member = api.member(), loading = false, saved = false) }
        catch (_: Exception) { cache.load()?.let { _state.value = _state.value.copy(loans = it.first, history = it.second, loading = false, saved = true) } ?: run { _state.value = _state.value.copy(loading = false, message = "Unable to load your library data") } }
    }
    fun renew(loan: Loan) = viewModelScope.launch(io) {
        try { val updated = api.renew(loan.loanId); _state.value = _state.value.copy(loans = _state.value.loans.map { if (it.loanId == updated.loanId) updated else it }); cache.save(_state.value.loans, _state.value.history) }
        catch (e: ApiException) { if (e.code == "RENEWAL_NOT_ALLOWED") _state.value = _state.value.copy(message = "This loan cannot be renewed") else _state.value = _state.value.copy(message = "Renewal failed") }
    }
    fun openDetail(id: String) = viewModelScope.launch(io) { runCatching { api.detail(id) }.onSuccess { _state.value = _state.value.copy(detail = it) }.onFailure { _state.value = _state.value.copy(message = "Unable to load book details") } }
    fun closeDetail() { _state.value = _state.value.copy(detail = null) }
    fun setHistory(enabled: Boolean) = viewModelScope.launch(io) { runCatching { api.updateHistory(enabled) }.onSuccess { _state.value = _state.value.copy(member = it) }.onFailure { _state.value = _state.value.copy(message = "Privacy setting was not updated") } }
}

class ShelfTapViewModelFactory(private val api: ShelfTapApi, private val cache: EncryptedCache) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ShelfTapViewModel(api, cache) as T
}

fun String.prettyDate(): String = runCatching { Instant.parse(this).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM yyyy")) }.getOrDefault(this)
fun Loan.daysLeft(): Long = runCatching { ChronoUnit.DAYS.between(Instant.now(), Instant.parse(dueAt)) }.getOrDefault(0)
