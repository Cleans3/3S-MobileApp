package vn.shelftap.app

import android.app.Application
import vn.shelftap.app.di.AppContainer
import vn.shelftap.app.di.DefaultAppContainer

class ShelfTapApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
