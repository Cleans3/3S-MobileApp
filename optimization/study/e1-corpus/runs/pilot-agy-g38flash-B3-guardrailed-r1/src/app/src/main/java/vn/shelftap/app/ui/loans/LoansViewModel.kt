package vn.shelftap.app.ui.loans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.model.Loan
import vn.shelftap.app.data.repository.RenewResult
import vn.shelftap.app.data.repository.ShelfTapRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class LoansViewModel(
    private val repository: ShelfTapRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoansUiState(isLoading = true))
    val uiState: StateFlow<LoansUiState> = _uiState.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    init {
        loadLoans()
    }

    fun loadLoans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.getLoans()
            result.fold(
                onSuccess = { loansData ->
                    val uiItems = loansData.loans.map { mapToUiItem(it) }
                    _uiState.update {
                        it.copy(
                            loans = uiItems,
                            isLoading = false,
                            isSavedData = loansData.isSavedData,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to load loans"
                        )
                    }
                }
            )
        }
    }

    fun renewLoan(loanId: String) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    loans = state.loans.map {
                        if (it.loanId == loanId) it.copy(isRenewing = true) else it
                    },
                    userMessage = null
                )
            }

            when (val result = repository.renewLoan(loanId)) {
                is RenewResult.Success -> {
                    val updatedUi = mapToUiItem(result.updatedLoan)
                    _uiState.update { state ->
                        state.copy(
                            loans = state.loans.map {
                                if (it.loanId == loanId) updatedUi else it
                            },
                            userMessage = "Loan renewed successfully"
                        )
                    }
                }
                is RenewResult.NotAllowed -> {
                    _uiState.update { state ->
                        state.copy(
                            loans = state.loans.map {
                                if (it.loanId == loanId) it.copy(isRenewing = false) else it
                            },
                            userMessage = "This loan cannot be renewed"
                        )
                    }
                }
                is RenewResult.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            loans = state.loans.map {
                                if (it.loanId == loanId) it.copy(isRenewing = false) else it
                            },
                            userMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    private fun mapToUiItem(loan: Loan): LoanUiItem {
        val now = Instant.now()
        val zoneId = ZoneId.systemDefault()
        var isOverdue = false
        var formattedDueDate = loan.dueAt
        var daysLeftText = ""

        try {
            val dueInstant = Instant.parse(loan.dueAt)
            val dueZoned = dueInstant.atZone(zoneId)
            val nowZoned = now.atZone(zoneId)
            formattedDueDate = dateFormatter.format(dueZoned)

            isOverdue = dueInstant.isBefore(now)
            val daysBetween = ChronoUnit.DAYS.between(nowZoned.toLocalDate(), dueZoned.toLocalDate())

            daysLeftText = when {
                daysBetween < 0 -> {
                    val absDays = -daysBetween
                    if (absDays == 1L) "1 day overdue" else "$absDays days overdue"
                }
                daysBetween == 0L -> "Due today"
                daysBetween == 1L -> "1 day left"
                else -> "$daysBetween days left"
            }
        } catch (e: Exception) {
            formattedDueDate = loan.dueAt
            daysLeftText = ""
        }

        return LoanUiItem(
            loanId = loan.loanId,
            copyId = loan.copyId,
            titleId = loan.titleId,
            title = loan.title,
            barcode = loan.barcode,
            formattedDueDate = formattedDueDate,
            daysLeftText = daysLeftText,
            isOverdue = isOverdue,
            renewable = loan.renewable,
            isRenewing = false
        )
    }
}
