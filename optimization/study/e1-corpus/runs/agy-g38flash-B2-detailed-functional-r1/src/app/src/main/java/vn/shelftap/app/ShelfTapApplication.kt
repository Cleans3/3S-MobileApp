package vn.shelftap.app

import android.app.Application
import vn.shelftap.app.data.api.NetworkClient
import vn.shelftap.app.data.api.ShelfTapApiService
import vn.shelftap.app.data.local.DeviceIdProvider
import vn.shelftap.app.data.local.SessionManager
import vn.shelftap.app.data.repository.AuthRepository

class ShelfTapApplication : Application() {

    lateinit var deviceIdProvider: DeviceIdProvider
        private set

    lateinit var sessionManager: SessionManager
        private set

    lateinit var apiService: ShelfTapApiService
        private set

    lateinit var authRepository: AuthRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        deviceIdProvider = DeviceIdProvider(this)
        sessionManager = SessionManager(this)
        apiService = NetworkClient.createApiService(sessionManager, deviceIdProvider)
        authRepository = AuthRepository(apiService, sessionManager, deviceIdProvider)
    }

    companion object {
        lateinit var instance: ShelfTapApplication
            private set
    }
}
