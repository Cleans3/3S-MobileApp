package vn.shelftap.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.data.local.AppDatabase
import vn.shelftap.app.data.remote.TitleApi

/**
 * Application class. Holds the process-wide repository (manual dependency wiring, no DI framework).
 * [appScope] outlives any Activity so a catalogue download keeps going while the app is open.
 */
class ShelfTapApp : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val repository: CatalogueRepository by lazy {
        CatalogueRepository(
            database = AppDatabase.create(this),
            api = TitleApi(),
            prefs = getSharedPreferences("catalogue_sync", MODE_PRIVATE),
            appScope = appScope,
        )
    }
}
