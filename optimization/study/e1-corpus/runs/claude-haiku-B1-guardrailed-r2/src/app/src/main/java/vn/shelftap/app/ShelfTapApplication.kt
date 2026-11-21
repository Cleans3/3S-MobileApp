package vn.shelftap.app

import android.app.Application

class ShelfTapApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // Downloads the catalogue if it is not complete yet. Does nothing otherwise.
        container.repository.startSync()
    }
}
