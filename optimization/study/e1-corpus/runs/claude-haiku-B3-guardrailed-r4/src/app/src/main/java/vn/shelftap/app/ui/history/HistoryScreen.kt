package vn.shelftap.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
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
fun HistoryScreen(onOpenBook: (String) -> Unit) {
    val app = LocalContext.current.applicationContext as ShelfTapApp
    val viewModel: HistoryViewModel = viewModel(
        factory = viewModelFactory {
            initializer { HistoryViewModel(app.container.libraryRepository) }
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

    if (state.confirmTurnOff) {
        AlertDialog(
            onDismissRequest = viewModel::dismissTurnOffConfirmation,
            title = { Text(stringResource(R.string.history_off_title)) },
            text = { Text(stringResource(R.string.history_off_text)) },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmTurnOff,
                    modifier = Modifier.sizeIn(minHeight = 48.dp),
                ) {
                    Text(stringResource(R.string.turn_off))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::dismissTurnOffConfirmation,
                    modifier = Modifier.sizeIn(minHeight = 48.dp),
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.history_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val loadError = state.loadError
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            PrivacySwitchRow(
                checked = state.historyEnabled,
                enabled = !state.isLoading && !state.saving,
                onToggle = viewModel::onHistoryToggled,
            )
            if (state.showingSavedData) {
                SavedDataNote()
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.isLoading && state.rows.isEmpty() -> CenteredProgress()

                    loadError != null && state.rows.isEmpty() -> MessageWithRetry(
                        message = stringResource(loadError.messageRes()),
                        onRetry = viewModel::load,
                    )

                    state.rows.isEmpty() -> Text(
                        text = stringResource(
                            if (state.historyEnabled) R.string.no_history else R.string.history_off_empty,
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    )

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            items = state.rows,
                            key = { it.key },
                            contentType = { "history" },
                        ) { row ->
                            HistoryCard(row = row, onOpenBook = { onOpenBook(row.titleId) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacySwitchRow(
    checked: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onToggle,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.history_switch),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun HistoryCard(row: HistoryRow, onOpenBook: () -> Unit) {
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
            Text(stringResource(R.string.borrowed_on, row.borrowedText))
            Text(
                text = row.returnedText?.let { stringResource(R.string.returned_on, it) }
                    ?: stringResource(R.string.not_returned_yet),
            )
        }
    }
}
