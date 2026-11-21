package com.example.library.loans.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.library.loans.R
import com.example.library.loans.domain.DenialReason
import com.example.library.loans.domain.RenewalPolicy
import com.example.library.loans.domain.RenewalResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    state: LoanDetailUiState,
    onRenew: () -> Unit,
    onResultShown: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val resultMessage = when (val result = state.lastResult) {
        null -> null
        is RenewalResult.Renewed -> stringResource(R.string.renew_success, result.loan.dueDate.toString())
        is RenewalResult.Rejected -> stringResource(denialMessageRes(result.reason))
        RenewalResult.NotFound -> stringResource(R.string.renew_not_found)
    }

    LaunchedEffect(resultMessage) {
        if (resultMessage != null) {
            snackbarHostState.showSnackbar(resultMessage)
            onResultShown()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(state.loan?.book?.title ?: stringResource(R.string.loan_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val loan = state.loan
        if (loan == null) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.loan_not_found))
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(loan.book.title, style = MaterialTheme.typography.headlineSmall)
            Text(loan.book.author, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Text(loan.book.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))

            DetailRow(stringResource(R.string.label_isbn), loan.book.isbn)
            DetailRow(stringResource(R.string.label_published), loan.book.publishedYear.toString())
            DetailRow(stringResource(R.string.label_pages), loan.book.pageCount.toString())

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            DetailRow(stringResource(R.string.label_borrowed_on), loan.borrowedOn.toString())
            if (loan.isReturned) {
                DetailRow(stringResource(R.string.label_returned_on), loan.returnedOn.toString())
            } else {
                DetailRow(stringResource(R.string.label_due_date), loan.dueDate.toString())
            }
            DetailRow(
                stringResource(R.string.label_renewals),
                "${loan.renewalCount}/${RenewalPolicy.MAX_RENEWALS}",
            )

            if (!loan.isReturned) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRenew,
                    enabled = state.canRenew && !state.isRenewing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.renew_button))
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun denialMessageRes(reason: DenialReason): Int = when (reason) {
    DenialReason.ALREADY_RETURNED -> R.string.renew_denied_returned
    DenialReason.OVERDUE -> R.string.renew_denied_overdue
    DenialReason.RENEWAL_LIMIT_REACHED -> R.string.renew_denied_limit
}
