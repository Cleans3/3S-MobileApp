package vn.shelftap.app

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import vn.shelftap.app.data.local.ShelfTapDatabase
import vn.shelftap.app.data.remote.NetworkClient
import vn.shelftap.app.data.remote.ShelfTapApiService
import vn.shelftap.app.data.repository.CatalogueRepository
import vn.shelftap.app.data.sync.CatalogueSyncWorker
import java.util.concurrent.TimeUnit

class ShelfTapApplication : Application() {

    lateinit var database: ShelfTapDatabase
        private set

    lateinit var apiService: ShelfTapApiService
        private set

    lateinit var repository: CatalogueRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = ShelfTapDatabase.getInstance(this)
        apiService = NetworkClient.createApiService(this)
        repository = CatalogueRepository(database, apiService)

        scheduleBackgroundSync()
    }

    private fun scheduleBackgroundSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = PeriodicWorkRequestBuilder<CatalogueSyncWorker>(
            12, TimeUnit.HOURS,
            1, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "CataloguePeriodicSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )
    }

    companion object {
        lateinit var instance: ShelfTapApplication
            private set
    }
}
