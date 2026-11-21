package vn.shelftap.app

import android.app.Application
import vn.shelftap.app.data.network.NetworkClient
import vn.shelftap.app.data.repository.AuthRepository
import vn.shelftap.app.data.repository.AuthRepositoryImpl
import vn.shelftap.app.data.repository.ShelfTapRepository
import vn.shelftap.app.data.repository.ShelfTapRepositoryImpl
import vn.shelftap.app.data.security.EncryptedStorage

class ShelfTapApplication : Application() {

    lateinit var encryptedStorage: EncryptedStorage
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var shelfTapRepository: ShelfTapRepository
        private set

    override fun onCreate() {
        super.onCreate()
        encryptedStorage = EncryptedStorage(this)
        authRepository = AuthRepositoryImpl(encryptedStorage)

        val okHttpClient = NetworkClient.createOkHttpClient(authRepository)
        val apiService = NetworkClient.createApiService(okHttpClient)
        shelfTapRepository = ShelfTapRepositoryImpl(
            apiService = apiService,
            encryptedStorage = encryptedStorage
        )
    }
}
