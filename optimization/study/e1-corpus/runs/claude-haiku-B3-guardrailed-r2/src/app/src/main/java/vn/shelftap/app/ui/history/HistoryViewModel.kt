package vn.shelftap.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.LibraryRepository
import vn.shelftap.app.model.HistoryItem
import vn.shelftap.app.ui.common.UiMessage
import vn.shelftap.app.ui.common.toDisplayDate
import vn.shelftap.app.ui.common.toUiMessage

/** One history row, already formatted for display. */
data class HistoryRowUi(
    val key: String,
    val titleId: String,
    val title: String,
    val borrowedText: String,
    val returnedText: String?,
)

data class HistoryUiState(
    val loading: Boolean = true,
    val rows: List<HistoryRowUi> = emptyList(),
    val historyEnabled: Boolean = true,
    val showingSaved: Boolean = false,
    val saving: Boolean = false,
    val confirmTurnOff: Boolean = false,
    val message: UiMessage? = null,
)

class HistoryViewModel(private val library: LibraryRepository) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null) }
            try {
                val loaded = library.history()
                _state.update {
                    it.copy(
                        loading = false,
                        rows = loaded.value.items.map(::toRow),
                        historyEnabled = loaded.value.historyEnabled,
                        showingSaved = loaded.fromSavedData,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, message = e.toUiMessage(UiMessage.LOAD_FAILED))
                }
            }
        }
    }

    /** Turning history on saves at once; turning it off asks for confirmation first. */
    fun onToggleRequested(enabled: Boolean) {
        if (enabled) {
            save(true)
        } else {
            _state.update { it.copy(confirmTurnOff = true) }
        }
    }

    fun dismissTurnOffDialog() {
        _state.update { it.copy(confirmTurnOff = false) }
    }

    fun confirmTurnOff() {
        _state.update { it.copy(confirmTurnOff = false) }
        save(false)
    }

    private fun save(enabled: Boolean) {
        if (_state.value.saving) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, message = null) }
            try {
                library.setHistoryEnabled(enabled)
                _state.update { it.copy(saving = false) }
                refresh()
            } catch (e: Exception) {
                _state.update {
                    it.copy(saving = false, message = e.toUiMessage(UiMessage.PRIVACY_FAILED))
                }
            }
        }
    }

    private fun toRow(item: HistoryItem): HistoryRowUi = HistoryRowUi(
        key = "${item.titleId}|${item.outAt}",
        titleId = item.titleId,
        title = item.title,
        borrowedText = item.outAt.toDisplayDate(),
        returnedText = item.returnedAt?.toDisplayDate(),
    )
}
