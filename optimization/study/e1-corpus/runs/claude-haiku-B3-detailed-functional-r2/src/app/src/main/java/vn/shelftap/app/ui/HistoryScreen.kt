@file:OptIn(ExperimentalMaterial3Api::class)

package vn.shelftap.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
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
import vn.shelftap.app.data.HistoryItem
import vn.shelftap.app.data.LibraryRepository

data class HistoryUiState(
    val items: List<HistoryItem> = emptyList(),
    /** Null until the member's settings have loaded at least once. */
    val historyEnabled: Boolean? = null,
    val loading: Boolean = true,
    val showingSavedData: Boolean = false,
    val error: String? = null,
    val saving: Boolean = false,
    val confirmTurnOff: Boolean = false,
    val message: String? = null,
)

class HistoryViewModel(private val library: LibraryRepository) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        _state.update { it.copy(loading = true, error = null) }
        try {
            val member = library.me()
            val history = library.history()
            _state.update {
                it.copy(
                    historyEnabled = member.data.historyEnabled,
                    items = history.data,
                    showingSavedData = member.fromSavedData || history.fromSavedData,
                )
            }
        } catch (e: Exception) {
            _state.update { it.copy(error = e.userMessage()) }
        } finally {
            _state.update { it.copy(loading = false) }
        }
    }

    /** Turning history on applies at once. Turning it off asks for confirmation first. */
    fun onSwitchChanged(enabled: Boolean) {
        if (enabled) {
            setHistoryEnabled(true)
        } else {
            _state.update { it.copy(confirmTurnOff = true) }
        }
    }

    fun confirmTurnOff() {
        _state.update { it.copy(confirmTurnOff = false) }
        setHistoryEnabled(false)
    }

    fun cancelTurnOff() {
        _state.update { it.copy(confirmTurnOff = false) }
    }

    fun messageShown() {
        _state.update { it.copy(message = null) }
    }

    private fun setHistoryEnabled(enabled: Boolean) {
        if (_state.value.saving) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            try {
                val member = library.setHistoryEnabled(enabled)
                _state.update {
                    it.copy(
                        historyEnabled = member.historyEnabled,
                        message = if (member.historyEnabled) "Reading history is on" else "Reading history is off",
                    )
                }
                load()
            } catch (e: Exception) {
                _state.update { it.copy(message = "Couldn't change the setting. ${e.userMessage()}") }
            } finally {
                _state.update { it.copy(saving = false) }
            }
        }
    }
}

@Composable
fun HistoryScreen(
    library: LibraryRepository,
    onOpenTitle: (String) -> Unit,
) {
    val viewModel: HistoryViewModel = viewModel(
        factory = viewModelFactory { initializer { HistoryViewModel(library) } },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    val message = state.message
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHost.showSnackbar(message)
            viewModel.messageShown()
        }
    }

    if (state.confirmTurnOff) {
        AlertDialog(
            onDismissRequest = viewModel::cancelTurnOff,
            title = { Text("Turn off reading history?") },
            text = {
                Text("New loans won't be added to your reading history while it is off.")
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmTurnOff) { Text("Turn off") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelTurnOff) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        val error = state.error
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.showingSavedData) {
                item { SavedDataNote() }
            }
            item {
                PrivacyRow(
                    enabled = state.historyEnabled == true,
                    controlEnabled = state.historyEnabled != null && !state.saving,
                    onChange = viewModel::onSwitchChanged,
                )
            }
            when {
                state.loading && state.items.isEmpty() -> item { LoadingBlock() }
                error != null && state.items.isEmpty() -> item {
                    ErrorState(error, onRetry = viewModel::refresh)
                }
                state.items.isEmpty() -> item {
                    EmptyText(
                        if (state.historyEnabled == false) {
                            "Reading history is off, so new loans are not recorded."
                        } else {
                            "No past loans yet."
                        },
                    )
                }
                else -> items(state.items) { entry ->
                    HistoryRow(entry, onClick = { onOpenTitle(entry.titleId) })
                }
            }
        }
    }
}

@Composable
private fun PrivacyRow(
    enabled: Boolean,
    controlEnabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Keep my reading history",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = if (enabled) "New loans are added to your history." else "Your history is off.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onChange,
                enabled = controlEnabled,
            )
        }
    }
}

@Composable
private fun HistoryRow(entry: HistoryItem, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(entry.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Borrowed ${formatDate(entry.outAt)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = if (entry.returnedAt == null) "Still out" else "Returned ${formatDate(entry.returnedAt)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
