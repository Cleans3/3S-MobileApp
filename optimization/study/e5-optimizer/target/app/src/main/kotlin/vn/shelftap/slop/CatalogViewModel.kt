package vn.shelftap.slop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.runBlocking

class CatalogViewModel(private val repo: CatalogRepository = CatalogRepository()) : ViewModel() {
    var books by mutableStateOf<List<Book>>(emptyList())
        private set
    var query by mutableStateOf("")
        private set

    init {
        books = runBlocking { repo.load() }
    }

    fun onQueryChange(value: String) {
        query = value
    }
}
