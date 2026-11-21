package vn.shelftap.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.HistoryItem
import vn.shelftap.app.data.LibraryRepository
import vn.shelftap.app.ui.parseInstant
import vn.shelftap.app.ui.toUserMessage

data class HistoryUiState(
    val items: List<HistoryItem> = emptyList(),
    val historyEnabled: Boolean? = null,
    val loading: Boolean = true,
    val showingSaved: Boolean = false,
    val error: String? = null,
    val saving: Boolean = false,
    val confirmTurnOff: Boolean = false,
)

class HistoryViewModel(private val library: LibraryRepository) : ViewModel() {
    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val member = library.member()
                val history = library.history()
                _state.update {
                    it.copy(
                        items = history.data.sortedByDescending { item -> parseInstant(item.outAt) },
                        historyEnabled = member.data.historyEnabled,
                        showingSaved = member.isSaved || history.isSaved,
                        loading = false,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "Could not load history: ${e.toUserMessage()}") }
            }
        }
    }

    /** Turning the switch on applies at once. Turning it off first asks for confirmation. */
    fun onSwitchChanged(enabled: Boolean) {
        if (enabled) {
            setHistoryEnabled(true)
        } else {
            _state.update { it.copy(confirmTurnOff = true) }
        }
    }

    fun cancelTurnOff() {
        _state.update { it.copy(confirmTurnOff = false) }
    }

    fun confirmTurnOff() {
        _state.update { it.copy(confirmTurnOff = false) }
        setHistoryEnabled(false)
    }

    private fun setHistoryEnabled(enabled: Boolean) {
        if (_state.value.saving) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            try {
                val member = library.setHistoryEnabled(enabled)
                _state.update { it.copy(historyEnabled = member.historyEnabled, saving = false) }
                refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(saving = false, error = "Could not change this setting: ${e.toUserMessage()}")
                }
            }
        }
    }
}
