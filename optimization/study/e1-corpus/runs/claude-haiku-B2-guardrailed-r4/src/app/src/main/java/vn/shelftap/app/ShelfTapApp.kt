package vn.shelftap.app

import android.app.Application
import android.content.Context
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import vn.shelftap.app.data.ApiClient
import vn.shelftap.app.data.AuthRepository
import vn.shelftap.app.data.KeystoreCipher
import vn.shelftap.app.data.SessionStore
import vn.shelftap.app.data.sessionDataStore
import java.util.concurrent.TimeUnit

class ShelfTapApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

/** Manual dependency wiring: one HTTP client, one API client, one session store, one auth repository. */
class AppContainer(context: Context) {
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()

    private val api = ApiClient(http)

    private val sessionStore = SessionStore(
        dataStore = context.applicationContext.sessionDataStore,
        cipher = KeystoreCipher(),
        io = Dispatchers.IO,
    )

    val authRepository = AuthRepository(api, sessionStore)
}
