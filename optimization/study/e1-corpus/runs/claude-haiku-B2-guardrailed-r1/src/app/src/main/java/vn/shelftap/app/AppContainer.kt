package vn.shelftap.app

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import vn.shelftap.app.data.api.ApiConfig
import vn.shelftap.app.data.api.ShelfTapApi
import vn.shelftap.app.data.security.TokenCipher
import vn.shelftap.app.data.session.SecureStateStore
import vn.shelftap.app.data.session.SessionRepository
import java.util.concurrent.TimeUnit

/** Manual dependency container. Created once in [ShelfTapApp.onCreate]. */
internal class AppContainer(context: Context) {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val json = Json { ignoreUnknownKeys = true }

    // No logging interceptor: request and response bodies contain tokens and personal data.
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val api = ShelfTapApi(httpClient, json, ApiConfig.BASE_URL, ioDispatcher)
    private val store = SecureStateStore(context, TokenCipher(), json, ioDispatcher)

    val session = SessionRepository(store, api, appScope)

    init {
        // The device ID is created on first launch and kept after that.
        appScope.launch { session.ensureDeviceId() }
    }
}
