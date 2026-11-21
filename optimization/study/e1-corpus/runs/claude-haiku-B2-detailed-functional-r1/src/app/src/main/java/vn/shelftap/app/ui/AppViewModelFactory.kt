package vn.shelftap.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import vn.shelftap.app.ShelfTapApp
import vn.shelftap.app.data.SessionRepository

/** Builds view models that need the app's [SessionRepository]. */
class AppViewModelFactory(
    private val build: (SessionRepository) -> ViewModel,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ShelfTapApp
        @Suppress("UNCHECKED_CAST")
        return build(app.container.sessionRepository) as T
    }
}
