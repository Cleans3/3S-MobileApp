package vn.shelftap.app

import android.app.Application

class ShelfTapApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
    }
}
