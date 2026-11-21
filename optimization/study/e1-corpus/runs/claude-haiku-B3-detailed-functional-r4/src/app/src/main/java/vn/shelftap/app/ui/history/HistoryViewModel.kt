package vn.shelftap.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.HistoryItem
import vn.shelftap.app.data.ShelfRepository
import vn.shelftap.app.data.userMessage
import vn.shelftap.app.util.Dates

data class HistoryUiState(
    val loading: Boolean = true,
    val items: List<HistoryItem> = emptyList(),
    val historyEnabled: Boolean = false,
    val fromCache: Boolean = false,
    val error: String? = null,
    val savingPrivacy: Boolean = false,
    val confirmTurnOff: Boolean = false,
    val message: String? = null,
)

class HistoryViewModel(private val repo: ShelfRepository) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val me = repo.me()
                val history = repo.history()
                _state.update {
                    it.copy(
                        loading = false,
                        historyEnabled = me.data.historyEnabled,
                        items = history.data.sortedByDescending { item -> Dates.parse(item.outAt) },
                        fromCache = me.fromCache || history.fromCache,
                    )
                }
            } catch (e: Exception) {
                val msg = e.userMessage()
                _state.update { s ->
                    if (s.items.isEmpty()) s.copy(loading = false, error = msg)
                    else s.copy(loading = false, message = msg)
                }
            }
        }
    }

    /** Turning history on applies at once; turning it off asks for confirmation first. */
    fun onToggle(enabled: Boolean) {
        if (_state.value.savingPrivacy) return
        if (enabled) {
            setPrivacy(true)
        } else {
            _state.update { it.copy(confirmTurnOff = true) }
        }
    }

    fun dismissTurnOff() {
        _state.update { it.copy(confirmTurnOff = false) }
    }

    fun confirmTurnOff() {
        _state.update { it.copy(confirmTurnOff = false) }
        setPrivacy(false)
    }

    fun messageShown() {
        _state.update { it.copy(message = null) }
    }

    private fun setPrivacy(enabled: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(savingPrivacy = true) }
            try {
                val me = repo.setHistoryEnabled(enabled)
                _state.update {
                    it.copy(
                        savingPrivacy = false,
                        historyEnabled = me.historyEnabled,
                        message = if (me.historyEnabled) "Reading history is on" else "Reading history is off",
                    )
                }
                refresh()
            } catch (e: Exception) {
                _state.update {
                    it.copy(savingPrivacy = false, message = "Setting not changed. ${e.userMessage()}")
                }
            }
        }
    }
}
