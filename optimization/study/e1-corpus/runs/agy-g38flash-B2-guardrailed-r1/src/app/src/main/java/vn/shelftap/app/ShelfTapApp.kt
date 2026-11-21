package vn.shelftap.app

import android.app.Application
import vn.shelftap.app.data.api.NetworkModule
import vn.shelftap.app.data.crypto.KeystoreEncryptionManager
import vn.shelftap.app.data.local.EncryptedSessionStorage
import vn.shelftap.app.data.repository.AuthRepository

class ShelfTapApp : Application() {

    lateinit var encryptionManager: KeystoreEncryptionManager
        private set

    lateinit var sessionStorage: EncryptedSessionStorage
        private set

    lateinit var networkModule: NetworkModule
        private set

    lateinit var authRepository: AuthRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        encryptionManager = KeystoreEncryptionManager()
        sessionStorage = EncryptedSessionStorage(
            context = applicationContext,
            encryptionManager = encryptionManager
        )

        networkModule = NetworkModule(
            sessionStorage = sessionStorage,
            onSessionExpired = {
                if (::authRepository.isInitialized) {
                    authRepository.handleSessionExpired()
                }
            }
        )

        authRepository = AuthRepository(
            apiService = networkModule.apiService,
            sessionStorage = sessionStorage,
            json = networkModule.json
        )
    }

    companion object {
        lateinit var instance: ShelfTapApp
            private set
    }
}
