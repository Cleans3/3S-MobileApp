package vn.shelftap.app.di

import android.content.Context
import kotlinx.coroutines.Dispatchers
import vn.shelftap.app.data.local.AppDatabase
import vn.shelftap.app.data.remote.NetworkClient
import vn.shelftap.app.data.repository.CatalogueRepository
import vn.shelftap.app.data.repository.CatalogueRepositoryImpl
import vn.shelftap.app.data.security.KeyStoreCryptoManager

interface AppContainer {
    val catalogueRepository: CatalogueRepository
    val keyStoreCryptoManager: KeyStoreCryptoManager
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    private val apiService by lazy {
        NetworkClient.createApiService()
    }

    override val keyStoreCryptoManager: KeyStoreCryptoManager by lazy {
        KeyStoreCryptoManager()
    }

    override val catalogueRepository: CatalogueRepository by lazy {
        CatalogueRepositoryImpl(
            database = database,
            apiService = apiService,
            ioDispatcher = Dispatchers.IO
        )
    }
}
