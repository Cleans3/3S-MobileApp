package vn.shelftap.app

import android.content.Context
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import vn.shelftap.app.auth.AuthRepository
import vn.shelftap.app.data.EncryptedCache
import vn.shelftap.app.data.LibraryRepository
import vn.shelftap.app.data.ShelfTapApi

/** Manually wired dependencies, created once per process. */
class AppContainer(context: Context, authRepository: AuthRepository) {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val api = ShelfTapApi(
        client = client,
        authRepository = authRepository,
        ioDispatcher = ioDispatcher,
    )

    private val cache = EncryptedCache(File(context.filesDir, "secure-cache"))

    val libraryRepository = LibraryRepository(api, cache, ioDispatcher)
}
