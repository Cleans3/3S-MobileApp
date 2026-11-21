package vn.shelftap.app

import android.app.Application
import vn.shelftap.app.data.api.NetworkModule
import vn.shelftap.app.data.crypto.EncryptedStorageManager
import vn.shelftap.app.data.repository.AuthRepository
import vn.shelftap.app.data.repository.DefaultAuthRepository
import vn.shelftap.app.data.repository.HistoryRepository
import vn.shelftap.app.data.repository.LoanRepository
import vn.shelftap.app.data.repository.TitleRepository
import vn.shelftap.app.data.repository.UserRepository

class ShelfTapApp : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }

    class AppContainer(application: Application) {
        val authRepository: AuthRepository = DefaultAuthRepository()
        val encryptedStorageManager = EncryptedStorageManager(application)
        val api = NetworkModule.createShelfTapApi(authRepository)

        val loanRepository = LoanRepository(api, encryptedStorageManager)
        val historyRepository = HistoryRepository(api, encryptedStorageManager)
        val userRepository = UserRepository(api, encryptedStorageManager)
        val titleRepository = TitleRepository(api)
    }
}
