package com.example.libcatalog

import android.app.Application
import com.example.libcatalog.data.BookRepository
import com.example.libcatalog.data.createBookApi

/** Application class: owns the single repository instance shared by the app. */
class LibraryApp : Application() {

    val bookRepository: BookRepository by lazy {
        BookRepository(createBookApi(BuildConfig.API_BASE_URL, logging = BuildConfig.DEBUG))
    }
}
