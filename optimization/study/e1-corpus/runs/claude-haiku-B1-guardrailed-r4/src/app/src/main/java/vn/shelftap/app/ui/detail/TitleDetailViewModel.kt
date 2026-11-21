package vn.shelftap.app.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONException
import vn.shelftap.app.ShelfTapApp
import vn.shelftap.app.model.Title

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Loaded(val title: Title) : DetailUiState
    data object NotFound : DetailUiState
    /** Not stored on the phone and the network request failed. */
    data object Unavailable : DetailUiState
}

class TitleDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val repository = (application as ShelfTapApp).container.repository
    private val titleId: String = checkNotNull(savedStateHandle.get<String>(ARG_TITLE_ID))

    private val _state = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = DetailUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                repository.title(titleId)?.let { DetailUiState.Loaded(it) } ?: DetailUiState.NotFound
            } catch (e: IOException) {
                DetailUiState.Unavailable
            } catch (e: JSONException) {
                DetailUiState.Unavailable
            }
        }
    }

    companion object {
        const val ARG_TITLE_ID = "titleId"
    }
}
