package vn.shelftap.app.ui.detail

import vn.shelftap.app.data.model.Title

/**
 * UI State for Title Detail screen.
 */
sealed interface TitleDetailUiState {
    object Loading : TitleDetailUiState
    data class Success(val title: Title) : TitleDetailUiState
    data class Error(val message: String) : TitleDetailUiState
}
