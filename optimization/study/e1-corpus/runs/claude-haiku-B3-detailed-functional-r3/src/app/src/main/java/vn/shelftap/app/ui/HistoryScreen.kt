package vn.shelftap.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.AppGraph
import vn.shelftap.app.data.HistoryItem
import vn.shelftap.app.data.Loaded
import vn.shelftap.app.data.ShelfTapRepository
import vn.shelftap.app.data.parseIsoInstant

data class HistoryUiState(
    val loading: Boolean = true,
    val items: List<HistoryItem> = emptyList(),
    val historyEnabled: Boolean = true,
    val savingPrivacy: Boolean = false,
    val confirmTurnOff: Boolean = false,
    val showingSaved: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

class HistoryViewModel(private val repo: ShelfTapRepository) : ViewModel() {
    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val member = repo.loadMember()
                val history = repo.loadHistory()
                _state.update {
                    it.copy(
                        loading = false,
                        items = history.data.sortedByDescending { item -> parseIsoInstant(item.outAt) },
                        historyEnabled = member.data.historyEnabled,
                        showingSaved = member is Loaded.Saved || history is Loaded.Saved,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = describeError(e)) }
            }
        }
    }

    /** Turning history on applies immediately; turning it off asks for confirmation first. */
    fun onSwitchChanged(enabled: Boolean) {
        if (enabled) {
            changePrivacy(true)
        } else {
            _state.update { it.copy(confirmTurnOff = true) }
        }
    }

    fun dismissTurnOff() {
        _state.update { it.copy(confirmTurnOff = false) }
    }

    fun confirmTurnOff() {
        _state.update { it.copy(confirmTurnOff = false) }
        changePrivacy(false)
    }

    private fun changePrivacy(enabled: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(savingPrivacy = true) }
            try {
                val member = repo.setHistoryEnabled(enabled)
                _state.update {
                    it.copy(historyEnabled = member.historyEnabled, savingPrivacy = false)
                }
                refresh()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        savingPrivacy = false,
                        message = "Could not change setting. ${describeError(e)}",
                    )
                }
            }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }
}

@Composable
fun HistoryScreen(
    onOpenTitle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: HistoryViewModel = viewModel(
        factory = viewModelFactory { initializer { HistoryViewModel(AppGraph.repository) } },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    if (state.confirmTurnOff) {
        AlertDialog(
            onDismissRequest = vm::dismissTurnOff,
            title = { Text("Turn off reading history?") },
            text = {
                Text("While this is off, your reading history list will be empty.")
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
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Keep my reading history",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = state.historyEnabled,
                    onCheckedChange = vm::onSwitchChanged,
                    enabled = !state.loading && !state.savingPrivacy,
                )
            }
            if (state.showingSaved) {
                SavedDataNote()
            }
            state.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            when {
                state.items.isNotEmpty() -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.items) { item ->
                        HistoryRow(item = item, onOpen = { onOpenTitle(item.titleId) })
                    }
                }
                state.loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
                state.error == null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (state.historyEnabled) "No past loans yet."
                        else "Reading history is off.",
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(item: HistoryItem, onOpen: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.clickable(onClick = onOpen),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Borrowed ${formatDate(parseIsoInstant(item.outAt))}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Returned " + (item.returnedAt?.let { formatDate(parseIsoInstant(it)) }
                    ?: "not yet"),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
