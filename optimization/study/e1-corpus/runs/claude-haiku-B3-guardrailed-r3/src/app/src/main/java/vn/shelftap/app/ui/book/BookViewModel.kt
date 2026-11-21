package vn.shelftap.app.ui.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.shelftap.app.data.LibraryRepository
import vn.shelftap.app.data.Title
import vn.shelftap.app.ui.Problem
import vn.shelftap.app.ui.toProblem

data class BookDetail(
    val title: String,
    /** Comma-separated; empty when the catalogue lists no authors. */
    val authors: String,
    /** Comma-separated; empty when the catalogue lists no subjects. */
    val subjects: String,
    val coverUrl: String?,
    val copiesAvailable: Int,
    val copiesTotal: Int,
)

data class BookUiState(
    val loading: Boolean = true,
    val detail: BookDetail? = null,
    val problem: Problem? = null,
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
        viewModelScope.launch {
            _state.value = BookUiState(loading = true)
            try {
                val title = repository.title(titleId)
                _state.value = BookUiState(loading = false, detail = title.toDetail())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = BookUiState(loading = false, problem = e.toProblem())
            }
        }
    }

    private fun Title.toDetail(): BookDetail = BookDetail(
        title = title,
        authors = authors.joinToString(", "),
        subjects = subjects.joinToString(", "),
        coverUrl = coverUrl,
        copiesAvailable = copiesAvailable,
        copiesTotal = copiesTotal,
    )
}
