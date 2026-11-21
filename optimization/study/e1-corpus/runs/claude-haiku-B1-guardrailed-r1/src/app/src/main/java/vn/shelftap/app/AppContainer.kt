package vn.shelftap.app

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.data.local.ShelfTapDatabase
import vn.shelftap.app.data.remote.CatalogueApi
import java.util.concurrent.TimeUnit

/** Hand-written dependency container, created once by [ShelfTapApplication]. */
class AppContainer(context: Context) {

    private val ioDispatcher = Dispatchers.IO
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val database = ShelfTapDatabase.create(context)
    private val api = CatalogueApi(httpClient, json, ioDispatcher)

    val repository = CatalogueRepository(
        database = database,
        api = api,
        ioDispatcher = ioDispatcher,
        appScope = appScope,
    )
}
