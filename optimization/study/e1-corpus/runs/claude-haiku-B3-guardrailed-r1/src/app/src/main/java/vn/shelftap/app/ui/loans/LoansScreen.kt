package vn.shelftap.app.ui.loans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import vn.shelftap.app.R
import vn.shelftap.app.ui.common.CenteredProgress
import vn.shelftap.app.ui.common.FailedState
import vn.shelftap.app.ui.common.SavedDataNote

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    viewModel: LoansViewModel,
    onOpenTitle: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            val text = when (message) {
                LoanMessage.CannotRenew -> context.getString(R.string.loan_cannot_renew)
                LoanMessage.RenewFailed -> context.getString(R.string.renew_failed)
            }
            snackbarHostState.showSnackbar(text)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.loans_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val modifier = Modifier.fillMaxSize().padding(innerPadding)
        when {
            state.loading && state.rows.isEmpty() -> CenteredProgress(modifier)
            state.loadFailed && state.rows.isEmpty() -> FailedState(
                onRetry = viewModel::refresh,
                modifier = modifier,
            )
            else -> LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                if (state.showingSaved) {
                    item(key = "saved-note", contentType = "note") {
                        SavedDataNote()
                    }
                }
                if (state.rows.isEmpty()) {
                    item(key = "empty", contentType = "empty") {
                        Text(
                            text = stringResource(R.string.no_loans),
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                items(
                    items = state.rows,
                    key = { it.loanId },
                    contentType = { "loan" },
                ) { row ->
                    LoanCard(
                        row = row,
                        renewing = row.loanId in state.renewingIds,
                        onOpen = { onOpenTitle(row.titleId) },
                        onRenew = { viewModel.renew(row.loanId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LoanCard(
    row: LoanRow,
    renewing: Boolean,
    onOpen: () -> Unit,
    onRenew: () -> Unit,
) {
    val resources = LocalContext.current.resources
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(role = Role.Button, onClick = onOpen),
            ) {
                Text(
                    text = row.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.due_on, row.dueText),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (row.overdue) {
                    OverdueMarker()
                } else {
                    Text(
                        text = if (row.daysLeft == 0) {
                            stringResource(R.string.due_today)
                        } else {
                            resources.getQuantityString(R.plurals.days_left, row.daysLeft, row.daysLeft)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onRenew,
                enabled = row.renewable && !renewing,
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Text(stringResource(R.string.renew))
            }
        }
    }
}

@Composable
private fun OverdueMarker() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = stringResource(R.string.overdue),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
