package vn.shelftap.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.SerializationException
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.data.TitleEntity
import java.io.IOException

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Loaded(val title: TitleEntity) : DetailUiState
    data object Error : DetailUiState
}

class DetailViewModel(
    private val repository: CatalogueRepository,
    private val titleId: String
) : ViewModel() {

    val state: StateFlow<DetailUiState> = flow {
        emit(DetailUiState.Loading)
        val result: DetailUiState = try {
            DetailUiState.Loaded(repository.getTitle(titleId))
        } catch (e: IOException) {
            DetailUiState.Error
        } catch (e: SerializationException) {
            DetailUiState.Error
        }
        emit(result)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetailUiState.Loading
    )
}
