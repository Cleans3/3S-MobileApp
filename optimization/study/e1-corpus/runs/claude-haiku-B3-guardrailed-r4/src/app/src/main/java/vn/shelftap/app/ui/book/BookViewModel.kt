package vn.shelftap.app.ui.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.CatalogTitle
import vn.shelftap.app.data.LibraryRepository
import vn.shelftap.app.ui.common.UiError

/** Display-ready book details; lists are joined here so the composable only lays them out. */
data class BookDetails(
    val title: String,
    val authors: String,
    val subjects: String,
    val coverUrl: String?,
    val copiesAvailable: Int,
    val copiesTotal: Int,
)

data class BookUiState(
    val book: BookDetails? = null,
    val error: UiError? = null,
)

class BookViewModel(
    private val repository: LibraryRepository,
    private val titleId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(BookUiState())
    val state: StateFlow<BookUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(error = null) }
        viewModelScope.launch {
            try {
                val title = repository.title(titleId)
                _state.update { it.copy(book = title.toDetails(), error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.update { it.copy(error = UiError.OFFLINE) }
            } catch (e: Exception) {
                _state.update { it.copy(error = UiError.GENERIC) }
            }
        }
    }

    private fun CatalogTitle.toDetails(): BookDetails = BookDetails(
        title = title,
        authors = authors.joinToString(", "),
        subjects = subjects.joinToString(", "),
        coverUrl = coverUrl,
        copiesAvailable = copiesAvailable,
        copiesTotal = copiesTotal,
    )
}
