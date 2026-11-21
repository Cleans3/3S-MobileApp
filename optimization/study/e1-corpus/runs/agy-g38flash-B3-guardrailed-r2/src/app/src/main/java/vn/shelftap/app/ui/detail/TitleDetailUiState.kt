package vn.shelftap.app.ui.detail

import vn.shelftap.app.data.model.TitleDetails

data class TitleDetailUiState(
    val isLoading: Boolean = false,
    val details: TitleDetails? = null,
    val error: String? = null
)
