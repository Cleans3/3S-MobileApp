package vn.shelftap.app

import android.app.Application
import vn.shelftap.app.data.CatalogueDatabase
import vn.shelftap.app.data.CatalogueRepository

class ShelfTapApplication : Application() {
    val repository: CatalogueRepository by lazy {
        CatalogueRepository(CatalogueDatabase(this))
    }
}
