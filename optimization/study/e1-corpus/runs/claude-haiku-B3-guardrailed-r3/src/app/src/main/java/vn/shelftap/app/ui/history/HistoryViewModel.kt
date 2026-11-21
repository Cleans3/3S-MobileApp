package vn.shelftap.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.shelftap.app.data.HistoryItem
import vn.shelftap.app.data.LibraryRepository
import vn.shelftap.app.data.formatDate
import vn.shelftap.app.data.parseInstant
import vn.shelftap.app.ui.Problem
import vn.shelftap.app.ui.toProblem

data class HistoryRow(
    val key: String,
    val titleId: String,
    val title: String,
    val borrowedDate: String,
    val returnedDate: String?,
)

data class HistoryUiState(
    val loading: Boolean = true,
    val rows: List<HistoryRow> = emptyList(),
    val historyEnabled: Boolean = true,
    val showingSavedData: Boolean = false,
    val confirmTurnOff: Boolean = false,
    val busy: Boolean = false,
    val problem: Problem? = null,
)

class HistoryViewModel(
    private val repository: LibraryRepository,
    private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            try {
                val member = repository.member()
                val history = repository.history()
                val rows = withContext(defaultDispatcher) { history.value.toRows() }
                _state.update {
                    it.copy(
                        loading = false,
                        rows = rows,
                        historyEnabled = member.value.historyEnabled,
                        showingSavedData = member.fromCache || history.fromCache,
                        problem = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, problem = e.toProblem()) }
            }
        }
    }

    /** Turning history on applies at once. Turning it off first asks for confirmation. */
    fun onSwitchTapped(wantEnabled: Boolean) {
        if (_state.value.busy) return
        if (wantEnabled) {
            applyHistorySetting(true)
        } else {
            _state.update { it.copy(confirmTurnOff = true) }
        }
    }

    fun dismissTurnOff() {
        _state.update { it.copy(confirmTurnOff = false) }
    }

    fun confirmTurnOff() {
        _state.update { it.copy(confirmTurnOff = false) }
        applyHistorySetting(false)
    }

    private fun applyHistorySetting(enabled: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, problem = null) }
            try {
                val member = repository.setHistoryEnabled(enabled)
                _state.update { it.copy(busy = false, historyEnabled = member.historyEnabled) }
                load()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, problem = e.toProblem()) }
            }
        }
    }

    /** Newest loan first. */
    private fun List<HistoryItem>.toRows(): List<HistoryRow> =
        sortedByDescending { parseInstant(it.outAt) }.map { item ->
            HistoryRow(
                key = "${item.titleId}|${item.outAt}",
                titleId = item.titleId,
                title = item.title,
                borrowedDate = formatDate(parseInstant(item.outAt)),
                returnedDate = item.returnedAt?.let { formatDate(parseInstant(it)) },
            )
        }
}
