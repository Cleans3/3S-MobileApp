package com.example.library.loans.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.library.loans.R
import com.example.library.loans.domain.Loan
import com.example.library.loans.domain.RenewOutcome
import com.example.library.loans.domain.RenewRejection
import com.example.library.loans.domain.isOverdue
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Composable
internal fun LoanCard(
    loan: Loan,
    today: LocalDate,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    onRenew: (() -> Unit)? = null,
) {
    Card(onClick = onOpen, modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(loan.book.title, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.loans_author, loan.book.author),
                style = MaterialTheme.typography.bodyMedium,
            )

            val returnedOn = loan.returnedOn
            if (returnedOn != null) {
                Text(
                    stringResource(R.string.loans_returned_on, returnedOn.toString()),
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Text(
                    stringResource(R.string.loans_due, loan.dueDate.toString()),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (loan.isOverdue(today)) {
                    Text(
                        stringResource(R.string.loans_overdue),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            if (onRenew != null) {
                TextButton(onClick = onRenew) {
                    Text(stringResource(R.string.loans_renew))
                }
            }
        }
    }
}

/** Shows a snackbar for each renewal result. */
@Composable
internal fun RenewResultEffect(
    results: Flow<RenewOutcome>,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    LaunchedEffect(results) {
        results.collect { outcome ->
            val message = when (outcome) {
                is RenewOutcome.Renewed ->
                    context.getString(R.string.loans_renew_success, outcome.loan.dueDate.toString())
                is RenewOutcome.Rejected ->
                    context.getString(outcome.reason.messageRes())
            }
            snackbarHostState.showSnackbar(message)
        }
    }
}

private fun RenewRejection.messageRes(): Int = when (this) {
    RenewRejection.ALREADY_RETURNED -> R.string.loans_renew_returned
    RenewRejection.OVERDUE -> R.string.loans_renew_overdue
    RenewRejection.RENEWAL_LIMIT_REACHED -> R.string.loans_renew_limit
    RenewRejection.NOT_FOUND -> R.string.loans_renew_not_found
}
