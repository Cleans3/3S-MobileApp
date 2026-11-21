package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import vn.shelftap.app.data.local.EncryptedShelfTapCache
import vn.shelftap.app.data.remote.NetworkClient
import vn.shelftap.app.data.repository.AuthRepository
import vn.shelftap.app.data.repository.DefaultAuthRepository
import vn.shelftap.app.data.repository.ShelfTapRepository
import vn.shelftap.app.data.repository.ShelfTapRepositoryImpl
import vn.shelftap.app.ui.ShelfTapApp
import vn.shelftap.app.ui.theme.ShelfTapTheme

class MainActivity : ComponentActivity() {

    private lateinit var authRepository: AuthRepository
    private lateinit var repository: ShelfTapRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize dependencies
        authRepository = DefaultAuthRepository()
        val okHttpClient = NetworkClient.createOkHttpClient(authRepository)
        val apiService = NetworkClient.createApiService(okHttpClient)
        val encryptedCache = EncryptedShelfTapCache(applicationContext)
        repository = ShelfTapRepositoryImpl(apiService, encryptedCache)

        setContent {
            ShelfTapTheme {
                ShelfTapApp(
                    repository = repository,
                    authRepository = authRepository
                )
            }
        }
    }
}
