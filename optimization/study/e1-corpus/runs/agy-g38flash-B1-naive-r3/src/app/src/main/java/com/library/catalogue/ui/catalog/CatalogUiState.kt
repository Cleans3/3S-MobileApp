package com.library.catalogue.ui.catalog

import com.library.catalogue.data.model.Book

data class CatalogUiState(
    val searchQuery: String = "",
    val selectedGenre: String = "All",
    val onlyAvailable: Boolean = false,
    val totalCount: Int = 0,
    val genres: List<String> = emptyList(),
    val selectedBook: Book? = null,
    val seedingProgress: Float = 1.0f
) {
    val isSeeding: Boolean
        get() = seedingProgress < 1.0f
}
