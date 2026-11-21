package vn.shelftap.app

import android.app.Application
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.data.db.AppDatabase
import vn.shelftap.app.data.remote.CatalogueApi

class ShelfTapApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Single place where the database, network client and repository are wired together. */
    val repository: CatalogueRepository by lazy {
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "shelftap.db")
            .build()
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        CatalogueRepository(
            db = db,
            api = CatalogueApi(client),
            prefs = getSharedPreferences("catalogue", MODE_PRIVATE),
            appScope = appScope,
        )
    }

    override fun onCreate() {
        super.onCreate()
        // Download the full catalogue in the background until it has been stored once.
        repository.startSyncIfIncomplete()
    }
}
