package vn.shelftap.app

import android.content.Context
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import vn.shelftap.app.api.ShelfTapApi
import vn.shelftap.app.auth.AuthRepository
import vn.shelftap.app.auth.UnconfiguredAuthRepository
import vn.shelftap.app.data.LibraryRepository
import vn.shelftap.app.data.SecureCache

/**
 * Manual dependency container. The [AuthRepository] here is the seam where the
 * app's real signed-in AuthRepository must be supplied.
 */
class AppContainer(context: Context) {

    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val authRepository: AuthRepository = UnconfiguredAuthRepository()

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    val libraryRepository: LibraryRepository = LibraryRepository(
        api = ShelfTapApi(authRepository, httpClient, json, ioDispatcher),
        cache = SecureCache(File(context.applicationContext.filesDir, "cache"), json),
        io = ioDispatcher,
    )
}
