package vn.shelftap.app.ui.detail

import vn.shelftap.app.data.model.TitleDetail

data class BookDetailUiState(
    val titleDetail: TitleDetail? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
