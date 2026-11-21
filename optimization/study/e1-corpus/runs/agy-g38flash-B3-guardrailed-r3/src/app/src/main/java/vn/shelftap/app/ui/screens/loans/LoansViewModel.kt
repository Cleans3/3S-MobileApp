package vn.shelftap.app.ui.screens.loans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.shelftap.app.data.model.Loan
import vn.shelftap.app.data.repository.RenewalNotAllowedException
import vn.shelftap.app.data.repository.ShelfTapRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class LoanItemUiState(
    val loan: Loan,
    val formattedDueDate: String,
    val daysLeft: Long,
    val isOverdue: Boolean,
    val daysLeftText: String,
    val isRenewing: Boolean = false,
    val renewalMessage: String? = null
)

data class LoansUiState(
    val isLoading: Boolean = true,
    val loans: List<LoanItemUiState> = emptyList(),
    val isOffline: Boolean = false,
    val errorMessage: String? = null
)

class LoansViewModel(
    private val repository: ShelfTapRepository,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoansUiState())
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
                onSuccess = { dataResult ->
                    val uiItems = withContext(defaultDispatcher) {
                        dataResult.data.map { mapToUiModel(it) }
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loans = uiItems,
                            isOffline = dataResult.isFromCache,
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
            // Set renewing state for the specific loan
            _uiState.update { current ->
                current.copy(
                    loans = current.loans.map {
                        if (it.loan.loanId == loanId) it.copy(isRenewing = true, renewalMessage = null)
                        else it
                    }
                )
            }

            val result = repository.renewLoan(loanId)
            result.fold(
                onSuccess = { updatedLoan ->
                    val updatedItem = withContext(defaultDispatcher) {
                        mapToUiModel(updatedLoan)
                    }
                    _uiState.update { current ->
                        current.copy(
                            loans = current.loans.map {
                                if (it.loan.loanId == loanId) updatedItem
                                else it
                            }
                        )
                    }
                },
                onFailure = { error ->
                    val message = if (error is RenewalNotAllowedException) {
                        "This loan cannot be renewed"
                    } else {
                        error.localizedMessage ?: "Renewal failed"
                    }
                    _uiState.update { current ->
                        current.copy(
                            loans = current.loans.map {
                                if (it.loan.loanId == loanId) {
                                    it.copy(isRenewing = false, renewalMessage = message)
                                } else it
                            }
                        )
                    }
                }
            )
        }
    }

    private fun mapToUiModel(loan: Loan): LoanItemUiState {
        val (formattedDueDate, daysLeft, isOverdue, daysLeftText) = try {
            val dueInstant = Instant.parse(loan.dueAt)
            val now = Instant.now()
            val localDueDate = dueInstant.atZone(ZoneId.systemDefault()).toLocalDate()
            val today = LocalDate.now()
            val days = ChronoUnit.DAYS.between(today, localDueDate)
            val overdue = days < 0 || now.isAfter(dueInstant)
            val text = when {
                days < 0 -> "${-days} days overdue"
                days == 0L -> "Due today"
                days == 1L -> "1 day left"
                else -> "$days days left"
            }
            val formatted = dateFormatter.format(localDueDate)
            Tuple4(formatted, days, overdue, text)
        } catch (_: Exception) {
            Tuple4(loan.dueAt, 0L, false, "")
        }

        return LoanItemUiState(
            loan = loan,
            formattedDueDate = formattedDueDate,
            daysLeft = daysLeft,
            isOverdue = isOverdue,
            daysLeftText = daysLeftText
        )
    }

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    companion object {
        fun provideFactory(repository: ShelfTapRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LoansViewModel(repository) as T
                }
            }
    }
}
