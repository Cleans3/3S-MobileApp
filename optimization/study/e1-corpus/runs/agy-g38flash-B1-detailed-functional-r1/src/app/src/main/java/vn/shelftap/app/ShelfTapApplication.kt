package vn.shelftap.app

import android.app.Application
import vn.shelftap.app.data.local.AppDatabase
import vn.shelftap.app.data.remote.ApiClient
import vn.shelftap.app.data.repository.CatalogueRepository
import vn.shelftap.app.data.sync.CatalogueSyncManager

/**
 * Application class initializing database, API service, sync manager, and repository.
 */
class ShelfTapApplication : Application() {

    lateinit var repository: CatalogueRepository
        private set

    lateinit var syncManager: CatalogueSyncManager
        private set

    override fun onCreate() {
        super.onCreate()

        val database = AppDatabase.getInstance(this)
        val apiService = ApiClient.createService(this)

        syncManager = CatalogueSyncManager(
            apiService = apiService,
            titleDao = database.titleDao()
        )

        repository = CatalogueRepository(
            titleDao = database.titleDao(),
            apiService = apiService,
            syncManager = syncManager
        )
    }
}
