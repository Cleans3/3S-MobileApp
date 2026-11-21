package com.example.libraryapp

import android.app.Application
import com.example.libraryapp.data.local.SessionManager
import com.example.libraryapp.data.remote.LibraryApiService
import com.example.libraryapp.data.remote.NetworkModule
import com.example.libraryapp.data.repository.AuthRepository
import com.example.libraryapp.data.repository.AuthRepositoryImpl

class LibraryApplication : Application() {

    lateinit var sessionManager: SessionManager
        private set

    lateinit var apiService: LibraryApiService
        private set

    lateinit var authRepository: AuthRepository
        private set

    override fun onCreate() {
        super.onCreate()

        sessionManager = SessionManager(this)
        apiService = NetworkModule.provideLibraryApiService(
            sessionManager = sessionManager,
            enableMockFallback = true
        )
        authRepository = AuthRepositoryImpl(
            apiService = apiService,
            sessionManager = sessionManager
        )
    }
}
