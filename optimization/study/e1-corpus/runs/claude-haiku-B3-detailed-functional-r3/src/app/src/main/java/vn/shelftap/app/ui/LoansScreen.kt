package vn.shelftap.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.AppGraph
import vn.shelftap.app.data.ApiException
import vn.shelftap.app.data.Loaded
import vn.shelftap.app.data.Loan
import vn.shelftap.app.data.ShelfTapRepository
import vn.shelftap.app.data.parseIsoInstant

data class LoansUiState(
    val loading: Boolean = true,
    val loans: List<Loan> = emptyList(),
    val showingSaved: Boolean = false,
    val error: String? = null,
    val renewingLoanId: String? = null,
    val message: String? = null,
)

class LoansViewModel(private val repo: ShelfTapRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoansUiState())
    val state: StateFlow<LoansUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val result = repo.loadLoans()
                _state.update {
                    it.copy(
                        loading = false,
                        loans = result.data,
                        showingSaved = result is Loaded.Saved,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = describeError(e)) }
            }
        }
    }

    fun renew(loanId: String) {
        if (_state.value.renewingLoanId != null) return
        viewModelScope.launch {
            _state.update { it.copy(renewingLoanId = loanId) }
            val message = try {
                val updated = repo.renewLoan(loanId)
                _state.update { s ->
                    s.copy(loans = s.loans.map { if (it.loanId == loanId) updated else it })
                }
                "Loan renewed. New due date saved."
            } catch (e: ApiException) {
                if (e.code == "RENEWAL_NOT_ALLOWED") "This loan cannot be renewed" else describeError(e)
            } catch (e: Exception) {
                describeError(e)
            }
            _state.update { it.copy(renewingLoanId = null, message = message) }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }
}

@Composable
fun LoansScreen(
    onOpenTitle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: LoansViewModel = viewModel(
        factory = viewModelFactory { initializer { LoansViewModel(AppGraph.repository) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Current loans",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = vm::refresh, enabled = !state.loading) {
                    Text("Refresh")
                }
            }
            if (state.showingSaved) {
                SavedDataNote()
            }
            state.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            when {
                state.loans.isNotEmpty() -> {
                    val now = Instant.now()
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.loans, key = { it.loanId }) { loan ->
                            LoanRow(
                                loan = loan,
                                now = now,
                                renewing = state.renewingLoanId == loan.loanId,
                                onOpen = { onOpenTitle(loan.titleId) },
                                onRenew = { vm.renew(loan.loanId) },
                            )
                        }
                    }
                }
                state.loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
                state.error == null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("You have no current loans.")
                }
            }
        }
    }
}

@Composable
private fun LoanRow(
    loan: Loan,
    now: Instant,
    renewing: Boolean,
    onOpen: () -> Unit,
    onRenew: () -> Unit,
) {
    val due = parseIsoInstant(loan.dueAt)
    val overdue = due.isBefore(now)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = loan.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.clickable(onClick = onOpen),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Due ${formatDate(due)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = daysLeftText(due, now),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                if (overdue) {
                    OverdueMarker()
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedButton(
                    onClick = onRenew,
                    enabled = loan.renewable && !renewing,
                ) {
                    Text(if (renewing) "Renewing..." else "Renew")
                }
            }
        }
    }
}

@Composable
private fun OverdueMarker() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = "Overdue",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
