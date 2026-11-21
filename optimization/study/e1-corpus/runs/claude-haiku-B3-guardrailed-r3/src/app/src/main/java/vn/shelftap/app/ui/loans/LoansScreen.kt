package vn.shelftap.app.ui.loans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import vn.shelftap.app.R
import vn.shelftap.app.ui.EmptyMessage
import vn.shelftap.app.ui.LoadingBox
import vn.shelftap.app.ui.ProblemText
import vn.shelftap.app.ui.SavedDataNote

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    state: LoansUiState,
    onRefresh: () -> Unit,
    onRenew: (String) -> Unit,
    onOpenBook: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.loans_title)) },
                actions = {
                    TextButton(
                        onClick = onRefresh,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                    ) {
                        Text(stringResource(R.string.action_refresh))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            if (state.showingSavedData) {
                item(key = "saved-note", contentType = "note") { SavedDataNote() }
            }
            state.problem?.let { problem ->
                item(key = "problem", contentType = "problem") { ProblemText(problem) }
            }
            when {
                state.loading && state.rows.isEmpty() ->
                    item(key = "loading", contentType = "progress") { LoadingBox() }
                state.rows.isEmpty() ->
                    item(key = "empty", contentType = "empty") { EmptyMessage(R.string.loans_empty) }
                else -> items(
                    items = state.rows,
                    key = { it.loanId },
                    contentType = { "loan" },
                ) { row ->
                    LoanItem(
                        row = row,
                        onOpen = { onOpenBook(row.titleId) },
                        onRenew = { onRenew(row.loanId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LoanItem(row: LoanRow, onOpen: () -> Unit, onRenew: () -> Unit) {
    val renewLabel = stringResource(R.string.renew_for, row.title)
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).clickable(onClick = onOpen)) {
                Text(row.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.due_on, row.dueDate),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(dueStatusText(row.daysLeft), style = MaterialTheme.typography.bodyMedium)
                if (row.overdue) {
                    Surface(
                        modifier = Modifier.padding(top = 4.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Text(
                            text = stringResource(R.string.overdue),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
            Button(
                onClick = onRenew,
                enabled = row.renewable && !row.renewing,
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .semantics { contentDescription = renewLabel },
            ) {
                Text(stringResource(R.string.action_renew))
            }
        }
    }
}

@Composable
private fun dueStatusText(daysLeft: Int): String =
    if (daysLeft >= 0) {
        pluralStringResource(R.plurals.days_left, daysLeft, daysLeft)
    } else {
        pluralStringResource(R.plurals.days_overdue, -daysLeft, -daysLeft)
    }
