package vn.shelftap.app.ui.loans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import vn.shelftap.app.data.Loan
import vn.shelftap.app.data.LibraryRepository
import vn.shelftap.app.data.formatDate
import vn.shelftap.app.data.parseInstant
import vn.shelftap.app.ui.Problem
import vn.shelftap.app.ui.toProblem

data class LoanRow(
    val loanId: String,
    val titleId: String,
    val title: String,
    val dueDate: String,
    /** Negative when the loan is overdue; then its absolute value is the days past due. */
    val daysLeft: Int,
    val overdue: Boolean,
    val renewable: Boolean,
    val renewing: Boolean,
)

data class LoansUiState(
    val loading: Boolean = true,
    val rows: List<LoanRow> = emptyList(),
    val showingSavedData: Boolean = false,
    val problem: Problem? = null,
)

class LoansViewModel(
    private val repository: LibraryRepository,
    private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow(LoansUiState())
    val state: StateFlow<LoansUiState> = _state.asStateFlow()

    private var loans: List<Loan> = emptyList()
    private var renewingIds: Set<String> = emptySet()
    private var showingSavedData = false
    private var problem: Problem? = null

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            try {
                val loaded = repository.loans()
                loans = loaded.value
                showingSavedData = loaded.fromCache
                problem = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                problem = e.toProblem()
            }
            publish()
        }
    }

    fun renew(loanId: String) {
        if (loanId in renewingIds) return
        viewModelScope.launch {
            renewingIds = renewingIds + loanId
            problem = null
            publish()
            try {
                val updated = repository.renew(loanId)
                loans = loans.map { if (it.loanId == loanId) updated else it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                problem = e.toProblem()
            }
            renewingIds = renewingIds - loanId
            publish()
        }
    }

    private suspend fun publish() {
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val currentLoans = loans
        val renewing = renewingIds
        val rows = withContext(defaultDispatcher) {
            currentLoans.map { it.toRow(now, zone, renewing) }
        }
        _state.value = LoansUiState(
            loading = false,
            rows = rows,
            showingSavedData = showingSavedData,
            problem = problem,
        )
    }
}

private fun Loan.toRow(now: Instant, zone: ZoneId, renewing: Set<String>): LoanRow {
    val due = parseInstant(dueAt)
    val today = now.atZone(zone).toLocalDate()
    val dueDay = due.atZone(zone).toLocalDate()
    val overdue = returnedAt == null && due.isBefore(now)
    val days: Long = if (overdue) {
        -maxOf(1L, ChronoUnit.DAYS.between(dueDay, today))
    } else {
        ChronoUnit.DAYS.between(today, dueDay)
    }
    return LoanRow(
        loanId = loanId,
        titleId = titleId,
        title = title,
        dueDate = formatDate(due),
        daysLeft = days.toInt(),
        overdue = overdue,
        renewable = renewable,
        renewing = loanId in renewing,
    )
}
