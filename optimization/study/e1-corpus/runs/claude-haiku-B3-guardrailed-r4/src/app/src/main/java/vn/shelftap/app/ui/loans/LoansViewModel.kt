package vn.shelftap.app.ui.loans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.ApiException
import vn.shelftap.app.data.LibraryRepository
import vn.shelftap.app.data.Loan
import vn.shelftap.app.ui.common.UiError
import vn.shelftap.app.ui.common.formatDate

data class LoanRow(
    val loanId: String,
    val titleId: String,
    val title: String,
    val dueText: String,
    val daysLeft: Long,
    val overdue: Boolean,
    val renewable: Boolean,
)

data class LoansUiState(
    val isLoading: Boolean = true,
    val rows: List<LoanRow> = emptyList(),
    val showingSavedData: Boolean = false,
    /** Set when there is nothing to show; the screen offers a retry. */
    val loadError: UiError? = null,
    /** One-shot message for a failed action; the screen shows it and calls actionErrorShown(). */
    val actionError: UiError? = null,
    val renewingIds: Set<String> = emptySet(),
)

class LoansViewModel(
    private val repository: LibraryRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val _state = MutableStateFlow(LoansUiState())
    val state: StateFlow<LoansUiState> = _state.asStateFlow()

    private var loans: List<Loan> = emptyList()
    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        _state.update { it.copy(isLoading = true, loadError = null) }
        loadJob = viewModelScope.launch {
            try {
                val loaded = repository.loans()
                loans = loaded.value
                publish(savedData = loaded.fromCache)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.update { it.copy(isLoading = false, loadError = UiError.NO_SAVED_DATA) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, loadError = UiError.GENERIC) }
            }
        }
    }

    fun renew(loanId: String) {
        if (loanId in _state.value.renewingIds) return
        _state.update { it.copy(renewingIds = it.renewingIds + loanId) }
        viewModelScope.launch {
            try {
                val updated = repository.renew(loanId)
                loans = loans.map { if (it.loanId == updated.loanId) updated else it }
                publish(savedData = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                val error = if (e.code == RENEWAL_NOT_ALLOWED) {
                    UiError.RENEWAL_NOT_ALLOWED
                } else {
                    UiError.GENERIC
                }
                reportActionError(error)
            } catch (e: IOException) {
                reportActionError(UiError.OFFLINE)
            } catch (e: Exception) {
                reportActionError(UiError.GENERIC)
            } finally {
                _state.update { it.copy(renewingIds = it.renewingIds - loanId) }
            }
        }
    }

    fun actionErrorShown() {
        _state.update { it.copy(actionError = null) }
    }

    private fun publish(savedData: Boolean) {
        val zone = clock.zone
        val now = Instant.now(clock)
        _state.update {
            it.copy(
                isLoading = false,
                rows = loans.map { loan -> loan.toRow(now, zone) },
                showingSavedData = savedData,
                loadError = null,
            )
        }
    }

    private fun reportActionError(error: UiError) {
        _state.update { it.copy(actionError = error) }
    }

    private fun Loan.toRow(now: Instant, zone: ZoneId): LoanRow {
        val today = now.atZone(zone).toLocalDate()
        val dueDay = dueAt.atZone(zone).toLocalDate()
        return LoanRow(
            loanId = loanId,
            titleId = titleId,
            title = title,
            dueText = formatDate(dueAt, zone),
            daysLeft = ChronoUnit.DAYS.between(today, dueDay),
            overdue = dueAt.isBefore(now),
            renewable = renewable,
        )
    }

    private companion object {
        const val RENEWAL_NOT_ALLOWED = "RENEWAL_NOT_ALLOWED"
    }
}
