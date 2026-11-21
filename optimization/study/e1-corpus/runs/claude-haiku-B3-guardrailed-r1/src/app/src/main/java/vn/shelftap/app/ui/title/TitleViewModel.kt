package vn.shelftap.app.ui.title

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.api.TitleDetail
import vn.shelftap.app.data.LibraryRepository

data class TitleUiState(
    val loading: Boolean = true,
    val failed: Boolean = false,
    val title: TitleDetail? = null,
)

class TitleViewModel(
    private val repository: LibraryRepository,
    private val titleId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(TitleUiState())
    val state: StateFlow<TitleUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, failed = false) }
            try {
                val detail = repository.title(titleId)
                _state.value = TitleUiState(loading = false, title = detail)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, failed = true) }
            }
        }
    }
}
