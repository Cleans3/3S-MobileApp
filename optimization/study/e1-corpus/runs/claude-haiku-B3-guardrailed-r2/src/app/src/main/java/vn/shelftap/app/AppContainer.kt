package vn.shelftap.app

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import vn.shelftap.app.auth.AuthRepository
import vn.shelftap.app.data.KeystoreCipher
import vn.shelftap.app.data.LibraryRepository
import vn.shelftap.app.data.SavedDataStore
import vn.shelftap.app.data.ShelfTapApi
import java.io.File
import java.util.concurrent.TimeUnit

/** Hand-written dependency graph. Dispatchers are injected so tests can replace them. */
class AppContainer(
    context: Context,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val auth = AuthRepository()

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    private val api = ShelfTapApi(
        client = httpClient,
        accessToken = auth::accessToken,
        io = ioDispatcher,
    )

    private val savedData = SavedDataStore(
        file = File(context.filesDir, "saved_data.bin"),
        cipher = KeystoreCipher(),
        io = ioDispatcher,
    )

    val library: LibraryRepository = LibraryRepository(api, savedData)
}
