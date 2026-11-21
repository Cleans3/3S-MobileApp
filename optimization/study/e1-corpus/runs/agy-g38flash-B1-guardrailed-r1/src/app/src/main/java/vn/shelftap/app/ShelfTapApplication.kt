package vn.shelftap.app

import android.app.Application
import vn.shelftap.app.di.ServiceLocator

class ShelfTapApplication : Application() {

    lateinit var serviceLocator: ServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        serviceLocator = ServiceLocator(this)
    }
}
