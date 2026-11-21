package vn.shelftap.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class LoanRow(val loan: Loan, val dueLabel: String, val daysLabel: String, val overdue: Boolean)
data class HistoryRow(val item: HistoryItem, val borrowedLabel: String, val returnedLabel: String)
data class ShelfState(val loans: List<LoanRow> = emptyList(), val history: List<HistoryRow> = emptyList(), val historyEnabled: Boolean = true, val saved: Boolean = false, val loading: Boolean = false, val message: String? = null)

class ShelfTapViewModel(private val api: ShelfTapApi, private val cache: SecureCache, private val io: CoroutineDispatcher = Dispatchers.IO) : ViewModel() {
    private val _state = MutableStateFlow(ShelfState(loading = true)); val state: StateFlow<ShelfState> = _state.asStateFlow()
    private val _detail = MutableStateFlow<TitleDetail?>(null); val detail: StateFlow<TitleDetail?> = _detail.asStateFlow()
    private val dateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.systemDefault())
    init { load() }
    private fun loanRows(items: List<Loan>) = items.map { val due=Instant.parse(it.dueAt); val days=ChronoUnit.DAYS.between(Instant.now().atZone(ZoneId.systemDefault()).toLocalDate(), due.atZone(ZoneId.systemDefault()).toLocalDate()); LoanRow(it,dateFormat.format(due),if(days<0) "${-days} days overdue" else "$days days left", due.isBefore(Instant.now())) }
    private fun historyRows(items: List<HistoryItem>) = items.sortedByDescending { it.outAt }.map { HistoryRow(it,dateFormat.format(Instant.parse(it.outAt)),it.returnedAt?.let { dateFormat.format(Instant.parse(it)) } ?: "Not returned") }
    fun load() = viewModelScope.launch { _state.value = _state.value.copy(loading=true, message=null); val cached = withContext(io) { cache.read() }; if (cached != null) applyCache(cached, true)
        try { val loans=async { api.loans() }; val history=async { api.history() }; val member=async { api.member() }; val next=ShelfState(loanRows(loans.await()),historyRows(history.await()),member.await().historyEnabled,false,false); _state.value=next; withContext(io) { cache.write(encode(next)) } } catch (_: Exception) { _state.value=_state.value.copy(loading=false, saved=cached!=null, message=if(cached==null) "Unable to load data" else null) }
    }
    private fun applyCache(raw: String, saved: Boolean) { runCatching { val o=JSONObject(raw); val loans=JSONArray(o.getString("loans")).toLoans(); val history=JSONArray(o.getString("history")).toHistory(); _state.value=ShelfState(loanRows(loans),historyRows(history),o.optBoolean("historyEnabled",true),saved,false) } }
    private fun encode(s: ShelfState) = JSONObject().put("loans", JSONArray(s.loans.map { loanJson(it.loan) })).put("history", JSONArray(s.history.map { historyJson(it.item) })).put("historyEnabled",s.historyEnabled).toString()
    private fun loanJson(l: Loan)=JSONObject().put("loanId",l.loanId).put("copyId",l.copyId).put("titleId",l.titleId).put("title",l.title).put("barcode",l.barcode).put("outAt",l.outAt).put("dueAt",l.dueAt).put("returnedAt",l.returnedAt).put("renewals",l.renewals).put("renewable",l.renewable)
    private fun historyJson(h: HistoryItem)=JSONObject().put("titleId",h.titleId).put("title",h.title).put("outAt",h.outAt).put("returnedAt",h.returnedAt)
    fun renew(loan: Loan) = viewModelScope.launch { try { val updated=api.renew(loan.loanId); _state.value=_state.value.copy(loans=loanRows(_state.value.loans.map { if(it.loan.loanId==loan.loanId) updated else it.loan }),message=null) } catch(e: ApiException) { if(e.code=="RENEWAL_NOT_ALLOWED") _state.value=_state.value.copy(message="This loan cannot be renewed") else _state.value=_state.value.copy(message="Unable to renew this loan") } catch(_:Exception) { _state.value=_state.value.copy(message="Unable to renew this loan") } }
    fun setHistory(enabled: Boolean) = viewModelScope.launch { try { val member=api.setHistory(enabled); _state.value=_state.value.copy(historyEnabled=member.historyEnabled); withContext(io){cache.write(encode(_state.value))} } catch(_:Exception){ _state.value=_state.value.copy(message="Unable to update privacy settings") } }
    fun loadTitle(id: String) = viewModelScope.launch { _detail.value=null; runCatching { api.title(id) }.onSuccess { _detail.value=it }.onFailure { _state.value=_state.value.copy(message="Unable to load book details") } }
}
class ShelfTapViewModelFactory(private val api: ShelfTapApi, private val cache: SecureCache) : ViewModelProvider.Factory { override fun <T : ViewModel> create(modelClass: Class<T>): T = ShelfTapViewModel(api,cache) as T }
