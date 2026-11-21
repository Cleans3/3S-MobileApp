package vn.shelftap.app.ui.loans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.api.ApiException
import vn.shelftap.app.api.Loan
import vn.shelftap.app.data.LibraryRepository

private const val RENEWAL_NOT_ALLOWED = "RENEWAL_NOT_ALLOWED"

enum class LoanMessage { CannotRenew, RenewFailed }

data class LoanRow(
    val loanId: String,
    val titleId: String,
    val title: String,
    val dueText: String,
    val daysLeft: Int,
    val overdue: Boolean,
    val renewable: Boolean,
)

data class LoansUiState(
    val rows: List<LoanRow> = emptyList(),
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val showingSaved: Boolean = false,
    val renewingIds: Set<String> = emptySet(),
)

class LoansViewModel(
    private val repository: LibraryRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val _state = MutableStateFlow(LoansUiState())
    val state: StateFlow<LoansUiState> = _state.asStateFlow()

    private val _messages = Channel<LoanMessage>(Channel.BUFFERED)
    val messages: Flow<LoanMessage> = _messages.receiveAsFlow()

    private var loans: List<Loan> = emptyList()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, loadFailed = false) }
            try {
                val snapshot = repository.loans()
                loans = snapshot.data
                _state.update {
                    it.copy(
                        rows = rowsFor(loans),
                        loading = false,
                        showingSaved = snapshot.fromCache,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, loadFailed = true) }
            }
        }
    }

    fun renew(loanId: String) {
        if (loanId in _state.value.renewingIds) return
        _state.update { it.copy(renewingIds = it.renewingIds + loanId) }
        viewModelScope.launch {
            try {
                val updated = repository.renew(loanId)
                loans = loans.map { if (it.loanId == loanId) updated else it }
                _state.update { it.copy(rows = rowsFor(loans)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                val message = if (e.errorCode == RENEWAL_NOT_ALLOWED) {
                    LoanMessage.CannotRenew
                } else {
                    LoanMessage.RenewFailed
                }
                _messages.send(message)
            } catch (e: Exception) {
                _messages.send(LoanMessage.RenewFailed)
            } finally {
                _state.update { it.copy(renewingIds = it.renewingIds - loanId) }
            }
        }
    }

    private fun rowsFor(source: List<Loan>): List<LoanRow> {
        val now = clock.instant()
        val today = LocalDate.now(clock)
        val format = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        return source.map { loan ->
            val due = Instant.parse(loan.dueAt)
            val dueDay = due.atZone(clock.zone)
            LoanRow(
                loanId = loan.loanId,
                titleId = loan.titleId,
                title = loan.title,
                dueText = format.format(dueDay),
                daysLeft = ChronoUnit.DAYS.between(today, dueDay.toLocalDate()).toInt(),
                overdue = loan.returnedAt == null && due.isBefore(now),
                renewable = loan.renewable && loan.returnedAt == null,
            )
        }
    }
}
