package com.example.librarycatalogue

import android.app.Application
import com.example.librarycatalogue.data.local.LibraryDatabase
import com.example.librarycatalogue.data.repository.BookRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LibraryApp : Application() {

    lateinit var database: LibraryDatabase
        private set

    lateinit var bookRepository: BookRepository
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = LibraryDatabase.getInstance(this)
        bookRepository = BookRepository(database)

        // Asynchronously ensure the 100,000 catalogue is seeded on first launch
        applicationScope.launch {
            bookRepository.initializeCatalogue()
        }
    }

    companion object {
        lateinit var instance: LibraryApp
            private set
    }
}
