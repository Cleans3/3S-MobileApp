package vn.shelftap.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.api.HistoryItem
import vn.shelftap.app.data.LibraryRepository

enum class HistoryMessage { UpdateFailed }

data class HistoryRow(
    val key: String,
    val titleId: String,
    val title: String,
    val borrowedText: String,
    val returnedText: String?,
)

data class HistoryUiState(
    val rows: List<HistoryRow> = emptyList(),
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val showingSaved: Boolean = false,
    val settingsLoaded: Boolean = false,
    val historyEnabled: Boolean = false,
    val updating: Boolean = false,
    val confirmTurnOff: Boolean = false,
)

class HistoryViewModel(
    private val repository: LibraryRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    private val _messages = Channel<HistoryMessage>(Channel.BUFFERED)
    val messages: Flow<HistoryMessage> = _messages.receiveAsFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, loadFailed = false) }
            try {
                val (me, history) = coroutineScope {
                    val meDeferred = async { repository.me() }
                    val historyDeferred = async { repository.history() }
                    meDeferred.await() to historyDeferred.await()
                }
                _state.update {
                    it.copy(
                        rows = rowsFor(history.data),
                        loading = false,
                        showingSaved = me.fromCache || history.fromCache,
                        settingsLoaded = true,
                        historyEnabled = me.data.historyEnabled,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, loadFailed = true) }
            }
        }
    }

    /** Turning the switch on applies at once; turning it off first asks for confirmation. */
    fun onToggle(enabled: Boolean) {
        if (enabled) {
            setHistoryEnabled(true)
        } else {
            _state.update { it.copy(confirmTurnOff = true) }
        }
    }

    fun dismissTurnOff() {
        _state.update { it.copy(confirmTurnOff = false) }
    }

    fun confirmTurnOff() {
        dismissTurnOff()
        setHistoryEnabled(false)
    }

    private fun setHistoryEnabled(enabled: Boolean) {
        if (_state.value.updating) return
        _state.update { it.copy(updating = true) }
        viewModelScope.launch {
            try {
                val me = repository.setHistoryEnabled(enabled)
                _state.update { it.copy(historyEnabled = me.historyEnabled) }
                refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.send(HistoryMessage.UpdateFailed)
            } finally {
                _state.update { it.copy(updating = false) }
            }
        }
    }

    private fun rowsFor(items: List<HistoryItem>): List<HistoryRow> {
        val format = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(clock.zone)
        return items.mapIndexed { index, item ->
            HistoryRow(
                key = "$index-${item.outAt}",
                titleId = item.titleId,
                title = item.title,
                borrowedText = format.format(Instant.parse(item.outAt)),
                returnedText = item.returnedAt?.let { format.format(Instant.parse(it)) },
            )
        }
    }
}
