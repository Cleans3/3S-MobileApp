package vn.shelftap.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import vn.shelftap.app.R
import vn.shelftap.app.ui.EmptyMessage
import vn.shelftap.app.ui.LoadingBox
import vn.shelftap.app.ui.ProblemText
import vn.shelftap.app.ui.SavedDataNote

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onSwitchChanged: (Boolean) -> Unit,
    onConfirmTurnOff: () -> Unit,
    onDismissTurnOff: () -> Unit,
    onRefresh: () -> Unit,
    onOpenBook: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
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
            item(key = "setting", contentType = "setting") {
                PrivacySwitchRow(
                    checked = state.historyEnabled,
                    enabled = !state.busy,
                    onChange = onSwitchChanged,
                )
            }
            if (state.showingSavedData) {
                item(key = "saved-note", contentType = "note") { SavedDataNote() }
            }
            state.problem?.let { problem ->
                item(key = "problem", contentType = "problem") { ProblemText(problem) }
            }
            when {
                state.loading && state.rows.isEmpty() ->
                    item(key = "loading", contentType = "progress") { LoadingBox() }
                state.rows.isEmpty() -> item(key = "empty", contentType = "empty") {
                    EmptyMessage(
                        if (state.historyEnabled) R.string.history_empty else R.string.history_disabled,
                    )
                }
                else -> items(
                    items = state.rows,
                    key = { it.key },
                    contentType = { "history" },
                ) { row ->
                    HistoryItemCard(row = row, onOpen = { onOpenBook(row.titleId) })
                }
            }
        }
    }

    if (state.confirmTurnOff) {
        AlertDialog(
            onDismissRequest = onDismissTurnOff,
            title = { Text(stringResource(R.string.history_off_title)) },
            text = { Text(stringResource(R.string.history_off_text)) },
            confirmButton = {
                TextButton(
                    onClick = onConfirmTurnOff,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Text(stringResource(R.string.turn_off))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissTurnOff,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun PrivacySwitchRow(checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onChange,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.keep_history),
            modifier = Modifier.weight(1f).padding(end = 16.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
        // The whole row is the toggle target, so the switch itself takes no clicks.
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun HistoryItemCard(row: HistoryRow, onOpen: () -> Unit) {
    val borrowed = stringResource(R.string.borrowed_on, row.borrowedDate)
    val returned = if (row.returnedDate != null) {
        stringResource(R.string.returned_on, row.returnedDate)
    } else {
        stringResource(R.string.not_returned)
    }
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(16.dp),
        ) {
            Text(row.title, style = MaterialTheme.typography.titleMedium)
            Text(borrowed, style = MaterialTheme.typography.bodyMedium)
            Text(returned, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
