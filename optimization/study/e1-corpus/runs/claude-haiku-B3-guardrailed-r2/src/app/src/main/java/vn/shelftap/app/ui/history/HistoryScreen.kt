package vn.shelftap.app.ui.history

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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
fun HistoryScreen(
    library: LibraryRepository,
    onOpenBook: (titleId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = viewModel<HistoryViewModel>(
        factory = viewModelFactory { initializer { HistoryViewModel(library) } },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.confirmTurnOff) {
        AlertDialog(
            onDismissRequest = viewModel::dismissTurnOffDialog,
            title = { Text(stringResource(R.string.turn_off_title)) },
            text = { Text(stringResource(R.string.turn_off_text)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmTurnOff) {
                    Text(stringResource(R.string.turn_off))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissTurnOffDialog) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.showingSaved) {
            item(key = "saved-note", contentType = "note") { SavedDataNote() }
        }
        item(key = "privacy", contentType = "privacy") {
            PrivacyRow(
                checked = state.historyEnabled,
                enabled = !state.loading && !state.saving,
                onToggle = viewModel::onToggleRequested,
            )
        }
        state.message?.let { message ->
            item(key = "message", contentType = "note") { MessageNote(message) }
        }
        if (state.loading && state.rows.isEmpty()) {
            item(key = "progress", contentType = "progress") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
        if (!state.historyEnabled) {
            item(key = "off-notice", contentType = "note") {
                Text(stringResource(R.string.history_off_notice))
            }
        } else if (!state.loading && state.rows.isEmpty()) {
            item(key = "empty", contentType = "note") {
                Text(stringResource(R.string.no_history))
            }
        }
        items(
            state.rows,
            key = { it.key },
            contentType = { "history" },
        ) { row ->
            HistoryCard(row = row, onOpenBook = { onOpenBook(row.titleId) })
        }
    }
}

@Composable
private fun PrivacyRow(
    checked: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 48.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onToggle,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.keep_history),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(end = 12.dp),
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
        )
    }
}

@Composable
private fun HistoryCard(row: HistoryRowUi, onOpenBook: () -> Unit) {
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
            Text(
                text = stringResource(R.string.borrowed_on, row.borrowedText),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = row.returnedText?.let { stringResource(R.string.returned_on, it) }
                    ?: stringResource(R.string.not_returned),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
