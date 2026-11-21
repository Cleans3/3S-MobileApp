package vn.shelftap.app.ui.loans

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
import vn.shelftap.app.data.repository.LoanRepository
import vn.shelftap.app.data.repository.LoansResult
import vn.shelftap.app.data.repository.RenewResult
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

class LoansViewModel(
    private val loanRepository: LoanRepository,
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoansUiState(isLoading = true))
    val uiState: StateFlow<LoansUiState> = _uiState.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    init {
        loadLoans()
    }

    fun loadLoans() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = loanRepository.getLoans()) {
                is LoansResult.Success -> {
                    val mappedItems = withContext(computationDispatcher) {
                        result.loans.map { mapToUi(it) }
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loans = mappedItems,
                            isSavedData = result.isSavedData,
                            error = null
                        )
                    }
                }
                is LoansResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun renewLoan(loanId: String) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    loans = state.loans.map {
                        if (it.loanId == loanId) it.copy(isRenewing = true) else it
                    }
                )
            }

            when (val result = loanRepository.renewLoan(loanId)) {
                is RenewResult.Success -> {
                    val updatedUiItem = withContext(computationDispatcher) {
                        mapToUi(result.updatedLoan)
                    }
                    _uiState.update { state ->
                        state.copy(
                            loans = state.loans.map {
                                if (it.loanId == loanId) updatedUiItem else it
                            },
                            userMessage = null
                        )
                    }
                }
                is RenewResult.RenewalNotAllowed -> {
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

    fun dismissUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    private fun mapToUi(loan: Loan): LoanItemUi {
        val (formattedDueDate, daysLeft, isOverdue, daysLeftText) = try {
            val dueInstant = Instant.parse(loan.dueAt)
            val dueDate = dueInstant.atZone(ZoneId.systemDefault()).toLocalDate()
            val today = LocalDate.now()
            val days = ChronoUnit.DAYS.between(today, dueDate)
            val formatted = dueDate.format(dateFormatter)

            val overdue = days < 0
            val text = when {
                days < 0 -> {
                    val count = abs(days)
                    if (count == 1L) "1 day overdue" else "$count days overdue"
                }
                days == 0L -> "Due today"
                days == 1L -> "1 day left"
                else -> "$days days left"
            }
            Quad(formatted, days, overdue, text)
        } catch (e: Exception) {
            Quad(loan.dueAt, 0L, false, "")
        }

        return LoanItemUi(
            loanId = loan.loanId,
            titleId = loan.titleId,
            title = loan.title,
            formattedDueDate = formattedDueDate,
            daysLeft = daysLeft,
            isOverdue = isOverdue,
            daysLeftText = daysLeftText,
            renewable = loan.renewable,
            isRenewing = false
        )
    }

    private data class Quad(
        val formattedDueDate: String,
        val daysLeft: Long,
        val isOverdue: Boolean,
        val daysLeftText: String
    )

    companion object {
        fun provideFactory(loanRepository: LoanRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LoansViewModel(loanRepository) as T
                }
            }
    }
}
