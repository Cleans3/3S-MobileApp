package com.library.catalogue

import android.app.Application
import com.library.catalogue.data.generator.BookDataSeeder
import com.library.catalogue.data.local.LibraryDatabase
import com.library.catalogue.data.repository.BookRepository
import com.library.catalogue.data.repository.BookRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LibraryApplication : Application() {

    lateinit var database: LibraryDatabase
        private set

    lateinit var bookRepository: BookRepository
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        database = LibraryDatabase.getInstance(this)
        val seeder = BookDataSeeder(database)
        bookRepository = BookRepositoryImpl(database, seeder)

        // Asynchronously ensure our 100,000 catalogue is seeded
        applicationScope.launch {
            bookRepository.ensureDataSeeded()
        }
    }
}
