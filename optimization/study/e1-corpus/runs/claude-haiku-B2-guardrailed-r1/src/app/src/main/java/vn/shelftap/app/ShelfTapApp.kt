package vn.shelftap.app

import android.app.Application

class ShelfTapApp : Application() {

    internal lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
