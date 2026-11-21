package com.example.libraryapp.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.libraryapp.data.local.SessionManager
import com.example.libraryapp.data.remote.LibraryApiService
import com.example.libraryapp.data.remote.NetworkClient
import com.example.libraryapp.data.repository.LibraryRepository
import com.example.libraryapp.data.repository.LibraryRepositoryImpl
import com.example.libraryapp.ui.screens.login.LoginViewModel
import com.example.libraryapp.ui.screens.profile.ProfileViewModel

object ServiceLocator {

    private var sessionManager: SessionManager? = null
    private var apiService: LibraryApiService? = null
    private var libraryRepository: LibraryRepository? = null

    fun initialize(context: Context) {
        val appCtx = context.applicationContext
        val sessionMgr = SessionManager(appCtx).also { sessionManager = it }
        val api = NetworkClient.createApiService(sessionMgr).also { apiService = it }
        libraryRepository = LibraryRepositoryImpl(api, sessionMgr)
    }

    fun provideLibraryRepository(): LibraryRepository {
        return libraryRepository ?: throw IllegalStateException("ServiceLocator has not been initialized")
    }

    fun provideViewModelFactory(): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repo = provideLibraryRepository()
                return when {
                    modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                        LoginViewModel(repo) as T
                    }
                    modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                        ProfileViewModel(repo) as T
                    }
                    else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }

    fun setTestRepository(repository: LibraryRepository) {
        libraryRepository = repository
    }
}
