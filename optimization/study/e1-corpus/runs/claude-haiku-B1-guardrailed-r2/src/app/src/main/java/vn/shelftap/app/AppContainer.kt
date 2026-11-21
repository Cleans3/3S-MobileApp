package vn.shelftap.app

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import vn.shelftap.app.data.CatalogueDatabase
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.data.TitleApi
import java.util.concurrent.TimeUnit

/** Hand-written dependency container. Holds the one repository the whole app shares. */
class AppContainer(context: Context) {

    /** Outlives any screen, so the catalogue download keeps going when a screen closes. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val database: CatalogueDatabase = Room.databaseBuilder(
        context,
        CatalogueDatabase::class.java,
        "shelftap-catalogue.db"
    ).build()

    val repository: CatalogueRepository = CatalogueRepository(
        api = TitleApi(
            client = httpClient,
            json = Json { ignoreUnknownKeys = true },
            baseUrl = BASE_URL,
            ioDispatcher = Dispatchers.IO
        ),
        dao = database.titleDao(),
        scope = appScope,
        ioDispatcher = Dispatchers.IO
    )

    private companion object {
        const val BASE_URL = "https://api.shelftap.example"
    }
}
