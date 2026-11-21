package com.example.libraryapp

import android.app.Application
import com.example.libraryapp.di.ServiceLocator

class LibraryApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.initialize(this)
    }
}
