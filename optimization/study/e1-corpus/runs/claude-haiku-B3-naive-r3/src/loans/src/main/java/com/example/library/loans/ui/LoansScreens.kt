package com.example.library.loans.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.library.loans.R
import com.example.library.loans.domain.LoanRules
import com.example.library.loans.domain.RenewalRejection
import com.example.library.loans.domain.RenewalResult
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyLoansScreen(
    viewModel: MyLoansViewModel,
    onOpenBook: (String) -> Unit,
    onOpenHistory: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val lastRenewal = state.lastRenewal
    val renewalText = lastRenewal?.let { renewalMessage(it) }
    LaunchedEffect(lastRenewal) {
        if (renewalText != null) {
            snackbarHostState.showSnackbar(renewalText)
            viewModel.onRenewalShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_loans_title)) },
                actions = {
                    TextButton(onClick = onOpenHistory) {
                        Text(stringResource(R.string.history_action))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.items.isEmpty()) {
            CenteredMessage(padding, stringResource(R.string.no_active_loans))
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.items, key = { it.loan.id }) { item ->
                    LoanCard(
                        item = item,
                        onOpen = { onOpenBook(item.loan.bookId) },
                        onRenew = { viewModel.renew(item.loan.id) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanHistoryScreen(
    viewModel: HistoryViewModel,
    onOpenBook: (String) -> Unit,
    onBack: () -> Unit,
) {
    val historyItems by viewModel.items.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.back_action)) }
                },
            )
        },
    ) { padding ->
        if (historyItems.isEmpty()) {
            CenteredMessage(padding, stringResource(R.string.no_history))
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(historyItems, key = { it.loan.id }) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = item.book?.title ?: stringResource(R.string.unknown_book),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = item.book?.author.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(
                                    R.string.returned_on,
                                    item.loan.returnedOn?.format(DATE_FORMAT).orEmpty(),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            TextButton(onClick = { onOpenBook(item.loan.bookId) }) {
                                Text(stringResource(R.string.details_action))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailsScreen(
    viewModel: BookDetailsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.book_details_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.back_action)) }
                },
            )
        },
    ) { padding ->
        val book = state.book
        when {
            state.isLoading -> Box(Modifier.padding(padding).fillMaxSize()) {}
            book == null -> CenteredMessage(padding, stringResource(R.string.book_not_found))
            else -> Column(
                modifier = Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(book.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.label_author, book.author),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    stringResource(R.string.label_isbn, book.isbn),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(R.string.label_pages, book.pages),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(book.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun LoanCard(
    item: LoanItem,
    onOpen: () -> Unit,
    onRenew: () -> Unit,
) {
    val today = LocalDate.now()
    val blocker = LoanRules.renewalBlocker(item.loan, today)
    val overdue = item.loan.isOverdue(today)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.book?.title ?: stringResource(R.string.unknown_book),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = item.book?.author.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.due_on, item.loan.dueDate.format(DATE_FORMAT)),
                style = MaterialTheme.typography.bodyMedium,
                color = if (overdue) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = stringResource(
                    R.string.renewals_used,
                    item.loan.renewalCount,
                    LoanRules.MAX_RENEWALS,
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onOpen) {
                    Text(stringResource(R.string.details_action))
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onRenew, enabled = blocker == null) {
                    Text(stringResource(R.string.renew_action))
                }
            }
            blocker?.let {
                Text(
                    text = stringResource(rejectionTextRes(it)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun CenteredMessage(padding: PaddingValues, text: String) {
    Box(
        modifier = Modifier.padding(padding).fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text)
    }
}

@Composable
private fun renewalMessage(result: RenewalResult): String = when (result) {
    is RenewalResult.Renewed -> stringResource(
        R.string.renew_success,
        result.loan.dueDate.format(DATE_FORMAT),
    )
    is RenewalResult.Rejected -> stringResource(rejectionTextRes(result.reason))
}

private fun rejectionTextRes(reason: RenewalRejection): Int = when (reason) {
    RenewalRejection.ALREADY_RETURNED -> R.string.reject_returned
    RenewalRejection.OVERDUE -> R.string.reject_overdue
    RenewalRejection.LIMIT_REACHED -> R.string.reject_limit
    RenewalRejection.NOT_FOUND -> R.string.reject_not_found
}
