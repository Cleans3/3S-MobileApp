package vn.shelftap.app

import android.app.Application
import vn.shelftap.app.data.AppContainer

class ShelfTapApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
