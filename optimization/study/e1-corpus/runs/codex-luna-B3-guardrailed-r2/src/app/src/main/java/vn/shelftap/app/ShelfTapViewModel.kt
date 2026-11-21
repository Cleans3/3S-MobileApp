package vn.shelftap.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class ShelfState(val loans: List<Loan> = emptyList(), val history: List<HistoryItem> = emptyList(), val member: Member? = null, val loading: Boolean = true, val saved: Boolean = false, val message: String? = null)
class ShelfTapViewModel(app: Application) : AndroidViewModel(app) {
    private val api = ShelfTapApi(SignedInAuthRepository())
    private val cache = SecureSnapshotStore(app)
    private val _state = MutableStateFlow(ShelfState())
    val state: StateFlow<ShelfState> = _state.asStateFlow()
    val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy").withZone(ZoneId.systemDefault())
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, message = null)
        try { val loans = api.loans(); val history = api.history(); val member = api.member(); cache.saveLoans(loans); cache.saveHistory(history); _state.value = ShelfState(loans, history, member, false, false) }
        catch (_: Exception) { val loans = cache.loadLoans(); val history = cache.loadHistory(); _state.value = _state.value.copy(loans = loans ?: _state.value.loans, history = history ?: _state.value.history, loading = false, saved = loans != null || history != null, message = if (loans == null && history == null) "Unable to load your library data" else null) }
    }
    fun renew(loan: Loan) = viewModelScope.launch { try { val updated = api.renew(loan.loanId); _state.value = _state.value.copy(loans = _state.value.loans.map { if (it.loanId == updated.loanId) updated else it }, message = null); cache.saveLoans(_state.value.loans) } catch (e: ApiException) { if (e.code == "RENEWAL_NOT_ALLOWED") _state.value = _state.value.copy(message = "This loan cannot be renewed") else _state.value = _state.value.copy(message = "Renewal failed") } catch (_: Exception) { _state.value = _state.value.copy(message = "Renewal failed") } }
    fun setHistory(enabled: Boolean) = viewModelScope.launch { try { _state.value = _state.value.copy(member = api.updateHistory(enabled)) } catch (_: Exception) { _state.value = _state.value.copy(message = "Privacy setting could not be updated") } }
    suspend fun book(id: String) = api.title(id)
    fun daysLeft(date: String): Long = runCatching { ChronoUnit.DAYS.between(Instant.now(), Instant.parse(date)) }.getOrDefault(0)
    fun format(date: String): String = runCatching { dateFormatter.format(Instant.parse(date)) }.getOrDefault(date)
}
