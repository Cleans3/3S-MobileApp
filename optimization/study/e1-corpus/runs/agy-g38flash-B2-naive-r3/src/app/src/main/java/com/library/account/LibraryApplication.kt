package com.library.account

import android.app.Application
import com.library.account.data.api.ApiClient
import com.library.account.data.repository.AuthRepository
import com.library.account.data.session.SessionManager

/**
 * Application class providing centralized application state and dependencies.
 */
class LibraryApplication : Application() {

    lateinit var sessionManager: SessionManager
        private set

    lateinit var authRepository: AuthRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        sessionManager = SessionManager(applicationContext)
        val apiService = ApiClient.getApiService(sessionManager)
        authRepository = AuthRepository(apiService, sessionManager)
    }

    companion object {
        lateinit var instance: LibraryApplication
            private set
    }
}
