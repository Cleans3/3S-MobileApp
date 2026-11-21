package vn.shelftap.app.ui.loans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.LibraryRepository
import vn.shelftap.app.data.RenewalNotAllowedException
import vn.shelftap.app.model.Loan
import vn.shelftap.app.ui.common.UiMessage
import vn.shelftap.app.ui.common.toDisplayDate
import vn.shelftap.app.ui.common.toUiMessage
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** One loan row, already formatted for display. */
data class LoanRowUi(
    val loanId: String,
    val titleId: String,
    val title: String,
    val dueText: String,
    val daysLeft: Long,
    val overdue: Boolean,
    val renewable: Boolean,
)

data class LoansUiState(
    val loading: Boolean = true,
    val rows: List<LoanRowUi> = emptyList(),
    val showingSaved: Boolean = false,
    val renewingId: String? = null,
    val message: UiMessage? = null,
)

class LoansViewModel(private val library: LibraryRepository) : ViewModel() {

    private val _state = MutableStateFlow(LoansUiState())
    val state: StateFlow<LoansUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null) }
            try {
                val loaded = library.loans()
                val now = Instant.now()
                _state.update {
                    it.copy(
                        loading = false,
                        rows = loaded.value.map { loan -> toRow(loan, now) },
                        showingSaved = loaded.fromSavedData,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, message = e.toUiMessage(UiMessage.LOAD_FAILED))
                }
            }
        }
    }

    fun renew(loanId: String) {
        if (_state.value.renewingId != null) return
        viewModelScope.launch {
            _state.update { it.copy(renewingId = loanId, message = null) }
            try {
                val updated = library.renew(loanId)
                _state.update { current ->
                    current.copy(
                        renewingId = null,
                        rows = current.rows.map { row ->
                            if (row.loanId == loanId) toRow(updated) else row
                        },
                    )
                }
            } catch (e: RenewalNotAllowedException) {
                _state.update {
                    it.copy(renewingId = null, message = UiMessage.RENEWAL_NOT_ALLOWED)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(renewingId = null, message = e.toUiMessage(UiMessage.RENEW_FAILED))
                }
            }
        }
    }

    private fun toRow(loan: Loan, now: Instant = Instant.now()): LoanRowUi {
        val zone = ZoneId.systemDefault()
        return LoanRowUi(
            loanId = loan.loanId,
            titleId = loan.titleId,
            title = loan.title,
            dueText = loan.dueAt.toDisplayDate(),
            daysLeft = ChronoUnit.DAYS.between(
                now.atZone(zone).toLocalDate(),
                loan.dueAt.atZone(zone).toLocalDate(),
            ),
            overdue = loan.returnedAt == null && loan.dueAt.isBefore(now),
            renewable = loan.renewable,
        )
    }
}
