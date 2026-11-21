package vn.shelftap.app

import android.app.Application
import vn.shelftap.app.di.AppContainer

class ShelfTapApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
