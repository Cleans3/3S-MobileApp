package vn.shelftap.app

import android.app.Application
import android.content.Context
import androidx.room.Room
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.data.local.ShelfTapDatabase
import vn.shelftap.app.data.remote.CatalogueApi

class ShelfTapApp : Application() {

    lateinit var repository: CatalogueRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val db = Room.databaseBuilder(this, ShelfTapDatabase::class.java, "shelftap.db").build()
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        repository = CatalogueRepository(db, CatalogueApi(), prefs)
        // Downloads the whole catalogue once; later launches use the local copy only.
        repository.startSyncIfNeeded()
    }

    private companion object {
        const val PREFS_NAME = "shelftap"
    }
}
