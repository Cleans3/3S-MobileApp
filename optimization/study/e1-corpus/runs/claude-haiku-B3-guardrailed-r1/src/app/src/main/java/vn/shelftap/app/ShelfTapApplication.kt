package vn.shelftap.app

import android.app.Application
import android.content.Context

class ShelfTapApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

fun Context.appContainer(): AppContainer =
    (applicationContext as ShelfTapApplication).container
