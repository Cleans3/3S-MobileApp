package vn.shelftap.app.ui.detail

import vn.shelftap.app.data.model.TitleDetail

data class BookDetailUiState(
    val isLoading: Boolean = false,
    val titleDetail: TitleDetail? = null,
    val isSavedData: Boolean = false,
    val errorMessage: String? = null
)
