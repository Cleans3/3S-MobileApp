package vn.shelftap.app.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import vn.shelftap.app.R
import vn.shelftap.app.ui.common.CenteredProgress
import vn.shelftap.app.ui.common.FailedState
import vn.shelftap.app.ui.common.SavedDataNote

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onOpenTitle: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            when (message) {
                HistoryMessage.UpdateFailed ->
                    snackbarHostState.showSnackbar(context.getString(R.string.history_update_failed))
            }
        }
    }

    if (state.confirmTurnOff) {
        AlertDialog(
            onDismissRequest = viewModel::dismissTurnOff,
            title = { Text(stringResource(R.string.turn_off_history_title)) },
            text = { Text(stringResource(R.string.turn_off_history_text)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmTurnOff) {
                    Text(stringResource(R.string.turn_off))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissTurnOff) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.history_title)) }) },
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
                item(key = "privacy", contentType = "privacy") {
                    PrivacyRow(
                        enabled = state.historyEnabled,
                        settingsLoaded = state.settingsLoaded,
                        updating = state.updating,
                        onToggle = viewModel::onToggle,
                    )
                }
                if (state.rows.isEmpty()) {
                    item(key = "empty", contentType = "empty") {
                        Text(
                            text = stringResource(
                                if (state.settingsLoaded && !state.historyEnabled) {
                                    R.string.history_off
                                } else {
                                    R.string.no_history
                                },
                            ),
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                items(
                    items = state.rows,
                    key = { it.key },
                    contentType = { "history" },
                ) { row ->
                    HistoryCard(row = row, onOpen = { onOpenTitle(row.titleId) })
                }
            }
        }
    }
}

@Composable
private fun PrivacyRow(
    enabled: Boolean,
    settingsLoaded: Boolean,
    updating: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .toggleable(
                value = enabled,
                enabled = settingsLoaded && !updating,
                role = Role.Switch,
                onValueChange = onToggle,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.keep_history),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = enabled,
            onCheckedChange = null,
            enabled = settingsLoaded && !updating,
        )
    }
}

@Composable
private fun HistoryCard(row: HistoryRow, onOpen: () -> Unit) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = row.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.borrowed_on, row.borrowedText),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = row.returnedText?.let { stringResource(R.string.returned_on, it) }
                    ?: stringResource(R.string.not_returned_yet),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
