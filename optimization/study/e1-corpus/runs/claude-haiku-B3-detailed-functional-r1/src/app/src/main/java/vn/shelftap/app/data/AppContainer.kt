package vn.shelftap.app.data

import android.content.Context
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import vn.shelftap.app.auth.AuthRepository
import vn.shelftap.app.auth.PlaceholderAuthRepository
import java.util.concurrent.TimeUnit

/** Manual dependency container. Swap [authRepository] for the app's real implementation. */
class AppContainer(context: Context) {
    val authRepository: AuthRepository = PlaceholderAuthRepository()

    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    val library = LibraryRepository(
        api = ShelfTapApi(httpClient, json, authRepository),
        store = SavedDataStore(context.applicationContext, json),
    )
}
