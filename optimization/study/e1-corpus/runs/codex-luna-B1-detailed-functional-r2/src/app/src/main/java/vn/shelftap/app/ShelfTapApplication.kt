package vn.shelftap.app

import android.app.Application
import vn.shelftap.app.data.AppDatabase
import vn.shelftap.app.data.CatalogueRepository

class ShelfTapApplication : Application() {
    val database by lazy { AppDatabase.create(this) }
    val repository by lazy { CatalogueRepository(database.titleDao()) }
}
