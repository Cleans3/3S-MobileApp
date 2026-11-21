package vn.shelftap.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import vn.shelftap.app.ShelfTapApp
import vn.shelftap.app.data.HistoryItem
import vn.shelftap.app.ui.SavedDataNote
import vn.shelftap.app.ui.formatDate

@Composable
fun HistoryScreen(onOpenTitle: (String) -> Unit) {
    val container = (LocalContext.current.applicationContext as ShelfTapApp).container
    val vm: HistoryViewModel = viewModel(
        factory = viewModelFactory { initializer { HistoryViewModel(container.library) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()

    if (state.confirmTurnOff) {
        AlertDialog(
            onDismissRequest = vm::cancelTurnOff,
            title = { Text("Turn off reading history?") },
            text = { Text("Your reading history will stop being kept. You can turn it back on at any time.") },
            confirmButton = { TextButton(onClick = vm::confirmTurnOff) { Text("Turn off") } },
            dismissButton = { TextButton(onClick = vm::cancelTurnOff) { Text("Cancel") } },
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        item {
            Text("Reading history", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            val enabled = state.historyEnabled
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = enabled != null && !state.saving,
                        role = Role.Switch,
                        onClick = { vm.onSwitchChanged(enabled != true) },
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Keep my reading history", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = enabled == true,
                    onCheckedChange = null,
                    enabled = enabled != null && !state.saving,
                )
            }
        }
        if (state.showingSaved) {
            item { SavedDataNote() }
        }
        state.error?.let { message ->
            item {
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (state.loading && state.items.isEmpty()) {
            item { CircularProgressIndicator() }
        }
        if (!state.loading && state.error == null && state.historyEnabled == false) {
            item {
                Text(
                    "Reading history is off. Turn it on to see past loans here.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else if (!state.loading && state.error == null && state.items.isEmpty()) {
            item { Text("No past loans yet.", style = MaterialTheme.typography.bodyMedium) }
        }
        items(state.items) { item ->
            HistoryCard(item = item, onOpen = { onOpenTitle(item.titleId) })
        }
    }
}

@Composable
private fun HistoryCard(item: HistoryItem, onOpen: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("Borrowed ${formatDate(item.outAt)}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = if (item.returnedAt == null) "Not returned yet" else "Returned ${formatDate(item.returnedAt)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
