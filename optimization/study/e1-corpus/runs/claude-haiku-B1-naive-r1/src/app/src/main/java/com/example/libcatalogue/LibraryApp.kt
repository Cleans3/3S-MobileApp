package com.example.libcatalogue

import android.app.Application
import com.example.libcatalogue.data.AppContainer

class LibraryApp : Application() {

    /** Manual dependency container; created lazily on first use. */
    val container: AppContainer by lazy { AppContainer(this) }
}
