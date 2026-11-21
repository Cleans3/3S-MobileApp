package vn.shelftap.app

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import vn.shelftap.app.data.CatalogueSync
import vn.shelftap.app.data.TitleRepository
import vn.shelftap.app.data.local.AppDatabase
import vn.shelftap.app.data.remote.CatalogueApi
import java.util.concurrent.TimeUnit

/** Manual dependency container. The dispatcher is injected so the work can be moved in tests. */
class AppContainer(
    context: Context,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val appScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "shelftap.db",
    ).build()

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api = CatalogueApi(httpClient, ioDispatcher)

    val repository = TitleRepository(database, api, ioDispatcher)

    val sync = CatalogueSync(database, api, appScope)
}
