package vn.shelftap.app.ui.loans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.ApiException
import vn.shelftap.app.data.LibraryRepository
import vn.shelftap.app.data.Loan
import vn.shelftap.app.ui.toUserMessage

data class LoansUiState(
    val loans: List<Loan> = emptyList(),
    val loading: Boolean = true,
    val showingSaved: Boolean = false,
    val error: String? = null,
    val renewingLoanId: String? = null,
    val renewMessage: String? = null,
)

class LoansViewModel(private val library: LibraryRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoansUiState())
    val state: StateFlow<LoansUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val snapshot = library.loans()
                _state.update {
                    it.copy(loans = snapshot.data, showingSaved = snapshot.isSaved, loading = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "Could not load loans: ${e.toUserMessage()}") }
            }
        }
    }

    fun renew(loanId: String) {
        if (_state.value.renewingLoanId != null) return
        viewModelScope.launch {
            _state.update { it.copy(renewingLoanId = loanId, renewMessage = null) }
            try {
                val updated = library.renew(loanId)
                _state.update { s ->
                    s.copy(
                        loans = s.loans.map { if (it.loanId == loanId) updated else it },
                        renewingLoanId = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiException) {
                val message = if (e.errorCode == "RENEWAL_NOT_ALLOWED") {
                    "This loan cannot be renewed"
                } else {
                    "Could not renew this loan. Please try again."
                }
                _state.update { it.copy(renewingLoanId = null, renewMessage = message) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(renewingLoanId = null, renewMessage = "Could not renew: ${e.toUserMessage()}")
                }
            }
        }
    }
}
