package vn.shelftap.app

import android.app.Application
import android.content.Context
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import vn.shelftap.app.data.ApiClient
import vn.shelftap.app.data.AuthRepository
import vn.shelftap.app.data.SessionStore
import java.util.concurrent.TimeUnit

class ShelfTapApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Hand-rolled dependency container: small enough that a DI framework is not worth it. */
class AppContainer(context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val sessionStore = SessionStore(context)

    private val api = ApiClient(
        baseUrl = BuildConfig.API_BASE_URL,
        http = http,
        json = json,
    )

    val authRepository = AuthRepository(
        api = api,
        session = sessionStore,
        json = json,
    )
}
