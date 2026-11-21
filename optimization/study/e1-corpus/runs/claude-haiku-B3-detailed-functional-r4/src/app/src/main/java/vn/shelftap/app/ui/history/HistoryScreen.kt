package vn.shelftap.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import vn.shelftap.app.data.HistoryItem
import vn.shelftap.app.shelfRepository
import vn.shelftap.app.ui.common.ErrorState
import vn.shelftap.app.ui.common.SavedDataNote
import vn.shelftap.app.util.Dates

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onOpenBook: (String) -> Unit) {
    val repo = LocalContext.current.shelfRepository()
    val vm: HistoryViewModel = viewModel(factory = viewModelFactory { initializer { HistoryViewModel(repo) } })
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val text = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(text)
        vm.messageShown()
    }

    if (state.confirmTurnOff) {
        AlertDialog(
            onDismissRequest = vm::dismissTurnOff,
            title = { Text("Turn off reading history?") },
            text = {
                Text("Your past loans will stop being recorded and will no longer be shown here.")
            },
            confirmButton = {
                TextButton(onClick = vm::confirmTurnOff) { Text("Turn off") }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissTurnOff) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                actions = {
                    IconButton(onClick = vm::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.fromCache) {
                SavedDataNote(Modifier.padding(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Keep my reading history",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = state.historyEnabled,
                    enabled = !state.loading && !state.savingPrivacy && state.error == null,
                    onCheckedChange = vm::onToggle,
                )
            }
            HorizontalDivider()

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.loading && state.items.isEmpty() ->
                        CircularProgressIndicator(Modifier.align(Alignment.Center))

                    state.error != null && state.items.isEmpty() ->
                        ErrorState(
                            message = state.error.orEmpty(),
                            onRetry = vm::refresh,
                            modifier = Modifier.align(Alignment.Center),
                        )

                    state.items.isEmpty() -> Text(
                        if (state.historyEnabled) "No past loans yet" else "Reading history is off",
                        modifier = Modifier.align(Alignment.Center),
                    )

                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.items, key = { "${it.titleId}|${it.outAt}" }) { item ->
                            HistoryCard(item = item, onOpen = { onOpenBook(item.titleId) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(item: HistoryItem, onOpen: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column(Modifier.padding(16.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Borrowed ${Dates.formatDate(Dates.parse(item.outAt))}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                item.returnedAt?.let { "Returned ${Dates.formatDate(Dates.parse(it))}" } ?: "Not returned yet",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
