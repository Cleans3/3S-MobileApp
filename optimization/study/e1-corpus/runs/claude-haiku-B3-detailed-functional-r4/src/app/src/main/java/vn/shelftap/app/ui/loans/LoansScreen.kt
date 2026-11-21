package vn.shelftap.app.ui.loans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import vn.shelftap.app.data.Loan
import vn.shelftap.app.shelfRepository
import vn.shelftap.app.ui.common.ErrorState
import vn.shelftap.app.ui.common.SavedDataNote
import vn.shelftap.app.util.Dates
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(onOpenBook: (String) -> Unit) {
    val repo = LocalContext.current.shelfRepository()
    val vm: LoansViewModel = viewModel(factory = viewModelFactory { initializer { LoansViewModel(repo) } })
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val text = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(text)
        vm.messageShown()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loans") },
                actions = {
                    IconButton(onClick = vm::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.fromCache) {
                SavedDataNote(Modifier.padding(16.dp))
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.loading && state.loans.isEmpty() ->
                        CircularProgressIndicator(Modifier.align(Alignment.Center))

                    state.error != null && state.loans.isEmpty() ->
                        ErrorState(
                            message = state.error.orEmpty(),
                            onRetry = vm::refresh,
                            modifier = Modifier.align(Alignment.Center),
                        )

                    state.loans.isEmpty() ->
                        Text("No current loans", modifier = Modifier.align(Alignment.Center))

                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.loans, key = { it.loanId }) { loan ->
                            LoanCard(
                                loan = loan,
                                renewing = state.renewingId == loan.loanId,
                                onOpen = { onOpenBook(loan.titleId) },
                                onRenew = { vm.renew(loan) },
                            )
                        }
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
    val due = Dates.parse(loan.dueAt)
    val overdue = loan.returnedAt == null && due.isBefore(Instant.now())

    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column(Modifier.padding(16.dp)) {
            Text(loan.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Due ${Dates.formatDate(due)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(4.dp))
            if (overdue) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        "Overdue",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            } else {
                Text(daysLeftText(Dates.daysUntil(due)), style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Renewals: ${loan.renewals}", style = MaterialTheme.typography.bodySmall)
                Button(onClick = onRenew, enabled = loan.renewable && !renewing) {
                    Text(if (renewing) "Renewing…" else "Renew")
                }
            }
        }
    }
}

private fun daysLeftText(days: Long): String = when {
    days <= 0L -> "Due today"
    days == 1L -> "1 day left"
    else -> "$days days left"
}
