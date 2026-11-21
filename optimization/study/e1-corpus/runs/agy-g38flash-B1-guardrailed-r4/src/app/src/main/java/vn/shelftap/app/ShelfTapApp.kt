package vn.shelftap.app

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.Dispatchers
import vn.shelftap.app.data.api.ApiClient
import vn.shelftap.app.data.local.AppDatabase
import vn.shelftap.app.data.repository.CatalogueRepository
import vn.shelftap.app.ui.catalogue.CatalogueViewModel
import vn.shelftap.app.ui.detail.TitleDetailViewModel

class ShelfTapApp : Application() {

    lateinit var catalogueRepository: CatalogueRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val isDebug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val database = AppDatabase.getInstance(this)
        val apiService = ApiClient.create(isDebug = isDebug)

        catalogueRepository = CatalogueRepository(
            titleDao = database.titleDao(),
            apiService = apiService,
            ioDispatcher = Dispatchers.IO
        )
    }
}

class ShelfTapViewModelFactory(
    private val app: ShelfTapApp
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(CatalogueViewModel::class.java) -> {
                CatalogueViewModel(app.catalogueRepository, Dispatchers.IO) as T
            }
            modelClass.isAssignableFrom(TitleDetailViewModel::class.java) -> {
                TitleDetailViewModel(app.catalogueRepository, Dispatchers.IO) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
