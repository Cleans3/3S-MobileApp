package vn.shelftap.app

import android.app.Application
import vn.shelftap.app.sync.CatalogueSyncScheduler

class ShelfTapApp : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        CatalogueSyncScheduler.schedule(this)
    }
}
