@file:OptIn(ExperimentalMaterial3Api::class)

package vn.shelftap.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.ApiException
import vn.shelftap.app.data.LibraryRepository
import vn.shelftap.app.data.Loan
import vn.shelftap.app.data.parseInstant
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class LoansUiState(
    val loans: List<Loan> = emptyList(),
    val loading: Boolean = true,
    val showingSavedData: Boolean = false,
    val error: String? = null,
    val renewingLoanIds: Set<String> = emptySet(),
    val message: String? = null,
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
                    it.copy(loans = snapshot.data, showingSavedData = snapshot.fromSavedData)
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.userMessage()) }
            } finally {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    fun renew(loan: Loan) {
        if (loan.loanId in _state.value.renewingLoanIds) return
        viewModelScope.launch {
            _state.update { it.copy(renewingLoanIds = it.renewingLoanIds + loan.loanId) }
            try {
                val updated = library.renew(loan.loanId)
                _state.update { s ->
                    s.copy(
                        loans = s.loans.map { if (it.loanId == updated.loanId) updated else it },
                        message = "Loan renewed",
                    )
                }
            } catch (e: ApiException) {
                val text = if (e.code == "RENEWAL_NOT_ALLOWED" || e.status == 409) {
                    "This loan cannot be renewed"
                } else {
                    e.userMessage()
                }
                _state.update { it.copy(message = text) }
            } catch (e: Exception) {
                _state.update { it.copy(message = e.userMessage()) }
            } finally {
                _state.update { it.copy(renewingLoanIds = it.renewingLoanIds - loan.loanId) }
            }
        }
    }

    fun messageShown() {
        _state.update { it.copy(message = null) }
    }
}

@Composable
fun LoansScreen(
    library: LibraryRepository,
    onOpenTitle: (String) -> Unit,
) {
    val viewModel: LoansViewModel = viewModel(
        factory = viewModelFactory { initializer { LoansViewModel(library) } },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    val message = state.message
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHost.showSnackbar(message)
            viewModel.messageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loans") },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        val error = state.error
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.showingSavedData) {
                SavedDataNote(Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            when {
                state.loans.isEmpty() && state.loading -> LoadingBlock()
                state.loans.isEmpty() && error != null -> ErrorState(error, onRetry = viewModel::refresh)
                state.loans.isEmpty() -> EmptyText("No books are on loan.")
                else -> LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (error != null) {
                        item {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                    }
                    items(state.loans, key = { it.loanId }) { loan ->
                        LoanCard(
                            loan = loan,
                            renewing = loan.loanId in state.renewingLoanIds,
                            onOpen = { onOpenTitle(loan.titleId) },
                            onRenew = { viewModel.renew(loan) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoanCard(
    loan: Loan,
    renewing: Boolean,
    onOpen: () -> Unit,
    onRenew: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val due = parseInstant(loan.dueAt)
    val daysLeft = due?.let { ChronoUnit.DAYS.between(LocalDate.now(zone), it.atZone(zone).toLocalDate()) }
    val overdue = due != null && due.isBefore(Instant.now())

    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = loan.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (overdue) {
                    OverdueBadge()
                }
            }
            Text(
                text = "Due ${formatDate(loan.dueAt)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = daysLeftText(daysLeft),
                style = MaterialTheme.typography.bodySmall,
                color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedButton(
                    onClick = onRenew,
                    enabled = loan.renewable && !renewing,
                ) {
                    if (renewing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Renew")
                    }
                }
            }
        }
    }
}

@Composable
private fun OverdueBadge() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = "Overdue",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

private fun daysLeftText(days: Long?): String = when {
    days == null -> "Due date unknown"
    days < 0 -> "Overdue by ${-days} ${dayWord(-days)}"
    days == 0L -> "Due today"
    else -> "$days ${dayWord(days)} left"
}

private fun dayWord(count: Long): String = if (count == 1L) "day" else "days"
