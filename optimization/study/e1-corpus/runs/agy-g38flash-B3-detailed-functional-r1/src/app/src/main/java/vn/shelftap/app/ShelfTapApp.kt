package vn.shelftap.app

import android.app.Application
import vn.shelftap.app.data.api.RetrofitClient
import vn.shelftap.app.data.api.ShelfTapApiService
import vn.shelftap.app.data.auth.AuthRepository
import vn.shelftap.app.data.auth.DefaultAuthRepository
import vn.shelftap.app.data.local.ShelfTapDatabase
import vn.shelftap.app.data.repository.LibraryRepository

class ShelfTapApp : Application() {

    lateinit var authRepository: AuthRepository
        private set

    lateinit var libraryRepository: LibraryRepository
        private set

    override fun onCreate() {
        super.onCreate()

        authRepository = DefaultAuthRepository()
        val apiService: ShelfTapApiService = RetrofitClient.create(authRepository)
        val database = ShelfTapDatabase.getInstance(this)

        libraryRepository = LibraryRepository(
            apiService = apiService,
            loanDao = database.loanDao(),
            historyDao = database.historyDao()
        )
    }
}
