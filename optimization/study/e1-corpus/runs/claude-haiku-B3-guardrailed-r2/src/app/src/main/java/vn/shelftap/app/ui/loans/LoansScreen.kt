package vn.shelftap.app.ui.loans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import vn.shelftap.app.R
import vn.shelftap.app.data.LibraryRepository
import vn.shelftap.app.ui.common.MessageNote
import vn.shelftap.app.ui.common.SavedDataNote

@Composable
fun LoansScreen(
    library: LibraryRepository,
    onOpenBook: (titleId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = viewModel<LoansViewModel>(
        factory = viewModelFactory { initializer { LoansViewModel(library) } },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.showingSaved) {
            item(key = "saved-note", contentType = "note") { SavedDataNote() }
        }
        state.message?.let { message ->
            item(key = "message", contentType = "note") { MessageNote(message) }
        }
        item(key = "refresh", contentType = "action") {
            TextButton(
                onClick = viewModel::refresh,
                modifier = Modifier.sizeIn(minHeight = 48.dp),
            ) {
                Text(stringResource(R.string.refresh))
            }
        }
        if (state.loading && state.rows.isEmpty()) {
            item(key = "progress", contentType = "progress") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
        if (!state.loading && state.rows.isEmpty()) {
            item(key = "empty", contentType = "note") {
                Text(stringResource(R.string.no_loans))
            }
        }
        items(
            state.rows,
            key = { it.loanId },
            contentType = { "loan" },
        ) { row ->
            LoanCard(
                row = row,
                renewing = state.renewingId == row.loanId,
                onOpenBook = { onOpenBook(row.titleId) },
                onRenew = { viewModel.renew(row.loanId) },
            )
        }
    }
}

@Composable
private fun LoanCard(
    row: LoanRowUi,
    renewing: Boolean,
    onOpenBook: () -> Unit,
    onRenew: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = row.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = 48.dp)
                    .clickable(onClick = onOpenBook),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.due_on, row.dueText),
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (row.overdue) {
                    OverdueMarker()
                }
            }
            if (!row.overdue) {
                Text(
                    text = stringResource(R.string.days_left, row.daysLeft),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(
                onClick = onRenew,
                enabled = row.renewable && !renewing,
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            ) {
                Text(stringResource(if (renewing) R.string.renewing else R.string.renew))
            }
        }
    }
}

@Composable
private fun OverdueMarker() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = stringResource(R.string.overdue),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}
