package vn.shelftap.app

import android.app.Application
import vn.shelftap.app.data.local.AppDatabase
import vn.shelftap.app.data.remote.NetworkClient
import vn.shelftap.app.data.repository.AuthRepository
import vn.shelftap.app.data.repository.DefaultAuthRepository
import vn.shelftap.app.data.repository.ShelfTapRepository

class ShelfTapApplication : Application() {

    lateinit var authRepository: AuthRepository
        private set

    lateinit var repository: ShelfTapRepository
        private set

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getInstance(this)
        authRepository = DefaultAuthRepository()

        val apiService = NetworkClient.createApiService(authRepository)
        repository = ShelfTapRepository(
            apiService = apiService,
            database = database
        )
    }
}
