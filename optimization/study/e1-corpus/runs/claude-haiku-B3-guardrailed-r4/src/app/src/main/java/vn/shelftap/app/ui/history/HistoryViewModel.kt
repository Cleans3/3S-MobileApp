package vn.shelftap.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import java.time.Clock
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.HistoryItem
import vn.shelftap.app.data.HistorySnapshot
import vn.shelftap.app.data.LibraryRepository
import vn.shelftap.app.ui.common.UiError
import vn.shelftap.app.ui.common.formatDate

data class HistoryRow(
    val key: String,
    val titleId: String,
    val title: String,
    val borrowedText: String,
    /** Null while the book is still out. */
    val returnedText: String?,
)

data class HistoryUiState(
    val isLoading: Boolean = true,
    val historyEnabled: Boolean = true,
    val rows: List<HistoryRow> = emptyList(),
    val showingSavedData: Boolean = false,
    val loadError: UiError? = null,
    val actionError: UiError? = null,
    /** True while a privacy change is being sent. */
    val saving: Boolean = false,
    val confirmTurnOff: Boolean = false,
)

class HistoryViewModel(
    private val repository: LibraryRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            try {
                val loaded = repository.history()
                publish(loaded.value, savedData = loaded.fromCache)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.update { it.copy(isLoading = false, loadError = UiError.NO_SAVED_DATA) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, loadError = UiError.GENERIC) }
            }
        }
    }

    /** Turning the setting on applies at once; turning it off first asks for confirmation. */
    fun onHistoryToggled(enabled: Boolean) {
        if (enabled) {
            saveHistoryEnabled(true)
        } else {
            _state.update { it.copy(confirmTurnOff = true) }
        }
    }

    fun dismissTurnOffConfirmation() {
        _state.update { it.copy(confirmTurnOff = false) }
    }

    fun confirmTurnOff() {
        _state.update { it.copy(confirmTurnOff = false) }
        saveHistoryEnabled(false)
    }

    fun actionErrorShown() {
        _state.update { it.copy(actionError = null) }
    }

    private fun saveHistoryEnabled(enabled: Boolean) {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                val snapshot = repository.setHistoryEnabled(enabled)
                publish(snapshot, savedData = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.update { it.copy(actionError = UiError.OFFLINE) }
            } catch (e: Exception) {
                _state.update { it.copy(actionError = UiError.GENERIC) }
            } finally {
                _state.update { it.copy(saving = false) }
            }
        }
    }

    private fun publish(snapshot: HistorySnapshot, savedData: Boolean) {
        val zone = clock.zone
        _state.update {
            it.copy(
                isLoading = false,
                historyEnabled = snapshot.historyEnabled,
                rows = snapshot.items
                    .sortedByDescending { item -> item.outAt }
                    .map { item -> item.toRow(zone) },
                showingSavedData = savedData,
                loadError = null,
            )
        }
    }

    private fun HistoryItem.toRow(zone: ZoneId): HistoryRow = HistoryRow(
        key = "$titleId@$outAt",
        titleId = titleId,
        title = title,
        borrowedText = formatDate(outAt, zone),
        returnedText = returnedAt?.let { formatDate(it, zone) },
    )
}
