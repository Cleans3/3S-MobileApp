package vn.shelftap.app

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import vn.shelftap.app.data.ApiClient
import vn.shelftap.app.data.AuthRepository
import vn.shelftap.app.data.EncryptedSessionStore
import vn.shelftap.app.security.KeystoreAesCipher
import java.util.concurrent.TimeUnit

/** Manual dependency container. Creating it does no disk or network work. */
class AppContainer(context: Context) {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val apiClient = ApiClient(BuildConfig.API_BASE_URL, httpClient, ioDispatcher)

    private val sessionStore = EncryptedSessionStore(
        context = context.applicationContext,
        cipher = KeystoreAesCipher(),
        io = ioDispatcher,
    )

    val authRepository: AuthRepository = AuthRepository(apiClient, sessionStore)
}
