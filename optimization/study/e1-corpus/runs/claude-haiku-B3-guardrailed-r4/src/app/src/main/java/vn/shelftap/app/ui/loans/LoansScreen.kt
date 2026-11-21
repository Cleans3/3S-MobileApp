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
import androidx.compose.foundation.lazy.item
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import vn.shelftap.app.R
import vn.shelftap.app.ShelfTapApp
import vn.shelftap.app.ui.common.CenteredProgress
import vn.shelftap.app.ui.common.MessageWithRetry
import vn.shelftap.app.ui.common.SavedDataNote
import vn.shelftap.app.ui.common.messageRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(onOpenBook: (String) -> Unit) {
    val app = LocalContext.current.applicationContext as ShelfTapApp
    val viewModel: LoansViewModel = viewModel(
        factory = viewModelFactory {
            initializer { LoansViewModel(app.container.libraryRepository) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.actionError) {
        val error = state.actionError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(context.getString(error.messageRes()))
        viewModel.actionErrorShown()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.loans_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val loadError = state.loadError
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when {
                state.isLoading && state.rows.isEmpty() -> CenteredProgress()

                loadError != null && state.rows.isEmpty() -> MessageWithRetry(
                    message = stringResource(loadError.messageRes()),
                    onRetry = viewModel::load,
                )

                state.rows.isEmpty() -> Text(
                    text = stringResource(R.string.no_loans),
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("loans_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (state.showingSavedData) {
                        item(key = "saved-data", contentType = "note") {
                            SavedDataNote()
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
                            onOpenBook = { onOpenBook(row.titleId) },
                            onRenew = { viewModel.renew(row.loanId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoanCard(
    row: LoanRow,
    renewing: Boolean,
    onOpenBook: () -> Unit,
    onRenew: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = row.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = 48.dp)
                    .clickable(onClick = onOpenBook),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.due_on, row.dueText))
                    if (row.overdue) {
                        OverdueMarker()
                    } else {
                        val days = row.daysLeft.toInt()
                        Text(pluralStringResource(R.plurals.days_left, days, days))
                    }
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
}

@Composable
private fun OverdueMarker() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = stringResource(R.string.overdue),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
