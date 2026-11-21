package com.library.app

import android.app.Application
import com.library.app.data.api.ApiClient
import com.library.app.data.local.SessionManager
import com.library.app.data.repository.LibraryRepository

class LibraryApplication : Application() {

    lateinit var sessionManager: SessionManager
        private set

    lateinit var repository: LibraryRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        sessionManager = SessionManager(applicationContext)
        val apiService = ApiClient.create(sessionManager)
        repository = LibraryRepository(apiService, sessionManager)
    }

    companion object {
        lateinit var instance: LibraryApplication
            private set
    }
}
