package com.example.library.loans.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.library.loans.R
import com.example.library.loans.domain.LoanPolicy
import com.example.library.loans.model.Book
import com.example.library.loans.model.Loan

@Composable
internal fun BookDetailScreen(
    book: Book?,
    loan: Loan?,
    state: LoansUiState,
    onMessageShown: () -> Unit,
    onBack: () -> Unit,
    onRenew: (String) -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    ShowLoanMessage(state.message, snackbar, onMessageShown)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.book_details)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (book == null) {
            EmptyState(R.string.book_not_found, Modifier.padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(book.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.author_label, book.author),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(stringResource(R.string.isbn_label, book.isbn))
                Text(stringResource(R.string.pages_label, book.pages))
                Text(book.description, style = MaterialTheme.typography.bodyLarge)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                when {
                    loan == null -> Text(stringResource(R.string.not_on_loan))
                    loan.isReturned -> Text(stringResource(R.string.returned_on, loan.returnedOn!!.display()))
                    else -> {
                        if (loan.isOverdue(state.today)) {
                            Text(
                                stringResource(R.string.due_on, loan.dueOn.display()),
                                color = MaterialTheme.colorScheme.error,
                            )
                            Text(stringResource(R.string.overdue), color = MaterialTheme.colorScheme.error)
                        } else {
                            Text(stringResource(R.string.due_on, loan.dueOn.display()))
                        }
                        Text(stringResource(R.string.renewals_used, loan.renewalCount, LoanPolicy.MAX_RENEWALS))
                        Button(onClick = { onRenew(loan.id) }) {
                            Text(stringResource(R.string.renew))
                        }
                    }
                }
            }
        }
    }
}
