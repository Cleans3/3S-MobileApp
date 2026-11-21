package vn.shelftap.app

import android.app.Application

class ShelfTapApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
