package vn.shelftap.app.ui.loans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.ApiException
import vn.shelftap.app.data.Loan
import vn.shelftap.app.data.RENEWAL_NOT_ALLOWED
import vn.shelftap.app.data.ShelfRepository
import vn.shelftap.app.data.userMessage

data class LoansUiState(
    val loading: Boolean = true,
    val loans: List<Loan> = emptyList(),
    val fromCache: Boolean = false,
    val error: String? = null,
    val renewingId: String? = null,
    val message: String? = null,
)

class LoansViewModel(private val repo: ShelfRepository) : ViewModel() {

    private val _state = MutableStateFlow(LoansUiState())
    val state: StateFlow<LoansUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val snapshot = repo.loans()
                _state.update {
                    it.copy(loading = false, loans = snapshot.data, fromCache = snapshot.fromCache)
                }
            } catch (e: Exception) {
                val msg = e.userMessage()
                _state.update { s ->
                    if (s.loans.isEmpty()) s.copy(loading = false, error = msg)
                    else s.copy(loading = false, message = msg)
                }
            }
        }
    }

    fun renew(loan: Loan) {
        if (!loan.renewable || _state.value.renewingId != null) return
        viewModelScope.launch {
            _state.update { it.copy(renewingId = loan.loanId) }
            val message = try {
                val updated = repo.renew(loan.loanId)
                _state.update { s ->
                    s.copy(loans = s.loans.map { if (it.loanId == updated.loanId) updated else it })
                }
                "Loan renewed"
            } catch (e: ApiException) {
                if (e.code == RENEWAL_NOT_ALLOWED) "This loan cannot be renewed" else e.userMessage()
            } catch (e: Exception) {
                e.userMessage()
            }
            _state.update { it.copy(renewingId = null, message = message) }
        }
    }

    fun messageShown() {
        _state.update { it.copy(message = null) }
    }
}
