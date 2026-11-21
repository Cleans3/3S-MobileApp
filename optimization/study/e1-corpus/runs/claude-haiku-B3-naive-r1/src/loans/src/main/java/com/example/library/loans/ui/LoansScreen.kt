package com.example.library.loans.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.example.library.loans.R
import com.example.library.loans.domain.LoanPolicy
import com.example.library.loans.model.Loan
import java.time.LocalDate

@Composable
internal fun LoansScreen(
    viewModel: LoansViewModel,
    onBookClick: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    ShowLoanMessage(state.message, snackbar, viewModel::consumeMessage)

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.loans_title)) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text(stringResource(R.string.tab_current)) },
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text(stringResource(R.string.tab_history)) },
                )
            }

            if (tab == 0) {
                if (state.active.isEmpty()) {
                    EmptyState(R.string.empty_active, Modifier.weight(1f))
                } else {
                    LoanList(
                        loans = state.active,
                        today = state.today,
                        onBookClick = onBookClick,
                        onRenew = viewModel::renew,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                if (state.history.isEmpty()) {
                    EmptyState(R.string.empty_history, Modifier.weight(1f))
                } else {
                    LoanList(
                        loans = state.history,
                        today = state.today,
                        onBookClick = onBookClick,
                        onRenew = null,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun LoanList(
    loans: List<Loan>,
    today: LocalDate,
    onBookClick: (String) -> Unit,
    onRenew: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(loans, key = { it.id }) { loan ->
            LoanCard(
                loan = loan,
                today = today,
                onOpen = { onBookClick(loan.book.id) },
                onRenew = if (onRenew == null) null else ({ onRenew(loan.id) }),
            )
        }
    }
}

@Composable
private fun LoanCard(
    loan: Loan,
    today: LocalDate,
    onOpen: () -> Unit,
    onRenew: (() -> Unit)?,
) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(loan.book.title, style = MaterialTheme.typography.titleMedium)
            Text(loan.book.author, style = MaterialTheme.typography.bodyMedium)

            val returnedOn = loan.returnedOn
            if (returnedOn != null) {
                Text(stringResource(R.string.returned_on, returnedOn.display()))
            } else {
                if (loan.isOverdue(today)) {
                    Text(
                        stringResource(R.string.due_on, loan.dueOn.display()),
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(stringResource(R.string.overdue), color = MaterialTheme.colorScheme.error)
                } else {
                    Text(stringResource(R.string.due_on, loan.dueOn.display()))
                }
                Text(stringResource(R.string.renewals_used, loan.renewalCount, LoanPolicy.MAX_RENEWALS))
                if (onRenew != null) {
                    Button(onClick = onRenew) {
                        Text(stringResource(R.string.renew))
                    }
                }
            }
        }
    }
}

@Composable
internal fun EmptyState(textRes: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(textRes), textAlign = TextAlign.Center)
    }
}
