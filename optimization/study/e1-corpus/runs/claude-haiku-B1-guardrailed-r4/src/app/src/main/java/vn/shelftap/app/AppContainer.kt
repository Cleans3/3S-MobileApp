package vn.shelftap.app

import android.content.Context
import androidx.room.Room
import okhttp3.OkHttpClient
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.data.CatalogueSync
import vn.shelftap.app.data.local.ShelfTapDatabase
import vn.shelftap.app.data.remote.CatalogueApi
import java.util.concurrent.TimeUnit

/** Manual dependency container. Created once per process by [ShelfTapApp]. */
class AppContainer(context: Context) {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val database: ShelfTapDatabase = Room.databaseBuilder(
        context.applicationContext,
        ShelfTapDatabase::class.java,
        "shelftap.db",
    ).build()

    private val api = CatalogueApi(httpClient)

    val repository = CatalogueRepository(database.catalogueDao(), api)

    val sync = CatalogueSync(database.catalogueDao(), api)
}
