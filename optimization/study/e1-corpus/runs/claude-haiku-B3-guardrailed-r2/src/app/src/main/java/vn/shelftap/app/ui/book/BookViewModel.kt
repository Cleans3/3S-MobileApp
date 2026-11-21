package vn.shelftap.app.ui.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.shelftap.app.data.LibraryRepository
import vn.shelftap.app.ui.common.UiMessage
import vn.shelftap.app.ui.common.toUiMessage

/** Book details, with lists already joined for display. */
data class BookUi(
    val title: String,
    val coverUrl: String?,
    val authors: String,
    val subjects: String,
    val copiesAvailable: Int,
    val copiesTotal: Int,
)

data class BookUiState(
    val loading: Boolean = true,
    val book: BookUi? = null,
    val message: UiMessage? = null,
)

class BookViewModel(
    private val library: LibraryRepository,
    private val titleId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(BookUiState())
    val state: StateFlow<BookUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null) }
            try {
                val title = library.title(titleId)
                _state.value = BookUiState(
                    loading = false,
                    book = BookUi(
                        title = title.title,
                        coverUrl = title.coverUrl,
                        authors = title.authors.joinToString(", "),
                        subjects = title.subjects.joinToString(", "),
                        copiesAvailable = title.copiesAvailable,
                        copiesTotal = title.copiesTotal,
                    ),
                )
            } catch (e: Exception) {
                _state.value = BookUiState(
                    loading = false,
                    message = e.toUiMessage(UiMessage.LOAD_FAILED),
                )
            }
        }
    }
}
