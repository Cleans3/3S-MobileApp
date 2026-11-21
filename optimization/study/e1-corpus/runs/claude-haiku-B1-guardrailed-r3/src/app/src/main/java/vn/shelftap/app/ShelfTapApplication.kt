package vn.shelftap.app

import android.app.Application

class ShelfTapApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Resumes the catalogue download if it was interrupted; does nothing once it is complete.
        container.sync.start()
    }
}
