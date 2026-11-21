package vn.shelftap.app

import android.app.Application
import vn.shelftap.app.data.api.NetworkModule
import vn.shelftap.app.data.local.AppDatabase
import vn.shelftap.app.data.repository.CatalogueRepository
import vn.shelftap.app.data.repository.CatalogueRepositoryImpl
import vn.shelftap.app.util.DefaultDispatcherProvider
import vn.shelftap.app.util.DispatcherProvider

class ShelfTapApplication : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val dispatchers: DispatcherProvider by lazy {
        DefaultDispatcherProvider()
    }

    val repository: CatalogueRepository by lazy {
        CatalogueRepositoryImpl(
            api = NetworkModule.api,
            database = database,
            dispatchers = dispatchers
        )
    }
}
