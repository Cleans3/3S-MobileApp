package vn.shelftap.app.ui.detail

import vn.shelftap.app.domain.model.Title

sealed interface TitleDetailUiState {
    data object Loading : TitleDetailUiState
    data class Success(val title: Title) : TitleDetailUiState
    data object Error : TitleDetailUiState
}
