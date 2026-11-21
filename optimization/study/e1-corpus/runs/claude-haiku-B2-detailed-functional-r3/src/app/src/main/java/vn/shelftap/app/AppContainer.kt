package vn.shelftap.app

import android.content.Context
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import vn.shelftap.app.data.SessionRepository
import vn.shelftap.app.data.SessionStore
import vn.shelftap.app.data.ShelfTapApi
import java.util.concurrent.TimeUnit

/** Hand-written dependency container; the app has only one repository. */
class AppContainer(context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val api = ShelfTapApi(
        client = httpClient,
        json = json,
        baseUrl = BuildConfig.API_BASE_URL,
    )

    val repository = SessionRepository(
        api = api,
        store = SessionStore(context.applicationContext),
    )
}
