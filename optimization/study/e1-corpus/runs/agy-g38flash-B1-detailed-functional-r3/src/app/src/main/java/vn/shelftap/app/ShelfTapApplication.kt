package vn.shelftap.app

import android.app.Application
import vn.shelftap.app.data.local.AppDatabase
import vn.shelftap.app.data.remote.NetworkClient
import vn.shelftap.app.data.repository.CatalogueRepository
import vn.shelftap.app.data.sync.CatalogueSyncManager

class ShelfTapApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var syncManager: CatalogueSyncManager
        private set

    lateinit var catalogueRepository: CatalogueRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        val apiService = NetworkClient.apiService

        syncManager = CatalogueSyncManager(
            apiService = apiService,
            titleDao = database.titleDao()
        )

        catalogueRepository = CatalogueRepository(
            titleDao = database.titleDao(),
            apiService = apiService,
            syncManager = syncManager
        )

        // Kick off sync on app launch to download or update offline catalogue
        syncManager.startSync(force = false)
    }

    companion object {
        lateinit var instance: ShelfTapApplication
            private set
    }
}
