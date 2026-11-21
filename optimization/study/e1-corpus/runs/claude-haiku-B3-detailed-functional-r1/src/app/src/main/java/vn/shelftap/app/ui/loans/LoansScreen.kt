package vn.shelftap.app.ui.loans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.Instant
import vn.shelftap.app.ShelfTapApp
import vn.shelftap.app.data.Loan
import vn.shelftap.app.ui.SavedDataNote
import vn.shelftap.app.ui.daysUntilDue
import vn.shelftap.app.ui.formatDate
import vn.shelftap.app.ui.parseInstant

@Composable
fun LoansScreen(onOpenTitle: (String) -> Unit) {
    val container = (LocalContext.current.applicationContext as ShelfTapApp).container
    val vm: LoansViewModel = viewModel(
        factory = viewModelFactory { initializer { LoansViewModel(container.library) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Current loans", style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = vm::refresh, enabled = !state.loading) { Text("Refresh") }
            }
        }
        if (state.showingSaved) {
            item { SavedDataNote() }
        }
        state.renewMessage?.let { message ->
            item {
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
        state.error?.let { message ->
            item {
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (state.loading && state.loans.isEmpty()) {
            item { CircularProgressIndicator() }
        }
        if (!state.loading && state.error == null && state.loans.isEmpty()) {
            item { Text("You have no loans right now.", style = MaterialTheme.typography.bodyLarge) }
        }
        items(state.loans, key = { it.loanId }) { loan ->
            LoanCard(
                loan = loan,
                renewing = state.renewingLoanId == loan.loanId,
                onOpen = { onOpenTitle(loan.titleId) },
                onRenew = { vm.renew(loan.loanId) },
            )
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
    val dueAt: Instant? = parseInstant(loan.dueAt)
    val overdue = dueAt != null && dueAt.isBefore(Instant.now())
    val daysLeft = dueAt?.let { daysUntilDue(it) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = loan.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.clickable(onClick = onOpen),
            )
            Spacer(Modifier.height(4.dp))
            Text("Due ${formatDate(loan.dueAt)}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))

            if (overdue) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = "Overdue",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            } else if (daysLeft != null) {
                Text(
                    text = when {
                        daysLeft <= 0L -> "Due today"
                        daysLeft == 1L -> "1 day left"
                        else -> "$daysLeft days left"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onRenew, enabled = loan.renewable && !renewing) {
                    Text(if (renewing) "Renewing…" else "Renew")
                }
            }
        }
    }
}
