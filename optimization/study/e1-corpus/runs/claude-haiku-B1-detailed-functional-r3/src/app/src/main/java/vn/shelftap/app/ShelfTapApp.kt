package vn.shelftap.app

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import vn.shelftap.app.data.CatalogueDatabase
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.data.createShelfTapApi

class ShelfTapApp : Application() {

    val repository: CatalogueRepository by lazy {
        CatalogueRepository(
            api = createShelfTapApi(),
            dao = CatalogueDatabase.create(this).titleDao(),
            prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
        )
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Downloads the catalogue when it has never been downloaded or is older than a day.
        // Failures are silent; the saved catalogue keeps working offline.
        appScope.launch { repository.syncIfStale() }
    }

    companion object {
        private const val PREFS_NAME = "catalogue_sync"
    }
}

@Composable
fun appRepository(): CatalogueRepository =
    (LocalContext.current.applicationContext as ShelfTapApp).repository
