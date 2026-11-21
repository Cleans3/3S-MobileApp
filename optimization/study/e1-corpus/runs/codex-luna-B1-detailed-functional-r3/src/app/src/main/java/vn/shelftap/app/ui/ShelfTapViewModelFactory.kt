package vn.shelftap.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import vn.shelftap.app.data.CatalogueRepository

class ShelfTapViewModelFactory(private val repository: CatalogueRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ShelfTapViewModel(repository) as T
}
