package vn.shelftap.app

import android.app.Application
import android.content.Context
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import vn.shelftap.app.data.AuthRepository
import vn.shelftap.app.data.SessionStore
import vn.shelftap.app.data.ShelfTapApi

class ShelfTapApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

/** Hand-wired dependencies; the app is small enough that no DI framework is needed. */
class AppContainer(context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        // Refresh tokens are single use: never silently replay a request on connection failure.
        .retryOnConnectionFailure(false)
        .build()

    private val store = SessionStore(context)

    val auth = AuthRepository(ShelfTapApi(http, json), store)
}
