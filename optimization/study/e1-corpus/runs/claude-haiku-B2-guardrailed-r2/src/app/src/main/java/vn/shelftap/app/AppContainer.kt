package vn.shelftap.app

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import vn.shelftap.app.data.AccountRepository
import vn.shelftap.app.data.SessionManager
import vn.shelftap.app.data.SessionStore
import vn.shelftap.app.data.ShelfTapApi
import vn.shelftap.app.data.TokenCipher
import java.util.concurrent.TimeUnit

/** Hand-written dependency container. Holds the only long-lived instances of the data layer. */
class AppContainer(context: Context) {
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val api = ShelfTapApi(httpClient, ioDispatcher)
    private val store = SessionStore(context.applicationContext, TokenCipher(), ioDispatcher)

    val sessions = SessionManager(store)
    val accounts = AccountRepository(api, store, sessions)
}
