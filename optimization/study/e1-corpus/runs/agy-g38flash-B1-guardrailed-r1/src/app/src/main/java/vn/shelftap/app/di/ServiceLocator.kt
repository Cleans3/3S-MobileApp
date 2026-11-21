package vn.shelftap.app.di

import android.content.Context
import androidx.room.Room
import vn.shelftap.app.data.local.AppDatabase
import vn.shelftap.app.data.remote.NetworkModule
import vn.shelftap.app.data.remote.ShelfTapApiService
import vn.shelftap.app.data.repository.CatalogueRepository
import vn.shelftap.app.data.repository.CatalogueRepositoryImpl
import vn.shelftap.app.data.security.CryptoManager

class ServiceLocator(private val context: Context) {

    val dispatcherProvider: DispatcherProvider by lazy {
        DefaultDispatcherProvider()
    }

    val cryptoManager: CryptoManager by lazy {
        CryptoManager(context.applicationContext, dispatcherProvider)
    }

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "shelftap_catalogue.db"
        ).fallbackToDestructiveMigration().build()
    }

    val apiService: ShelfTapApiService by lazy {
        val okHttpClient = NetworkModule.createOkHttpClient()
        NetworkModule.createApiService(okHttpClient)
    }

    val catalogueRepository: CatalogueRepository by lazy {
        CatalogueRepositoryImpl(
            apiService = apiService,
            database = database,
            dispatcherProvider = dispatcherProvider
        )
    }
}
