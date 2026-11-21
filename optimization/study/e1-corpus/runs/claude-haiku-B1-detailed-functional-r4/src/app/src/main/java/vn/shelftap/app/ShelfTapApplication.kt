package vn.shelftap.app

import android.app.Application
import androidx.room.Room
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.data.ShelfTapApi
import vn.shelftap.app.data.ShelfTapDatabase
import vn.shelftap.app.data.ShelfTapJson
import java.util.concurrent.TimeUnit

class ShelfTapApplication : Application() {

    lateinit var repository: CatalogueRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        val database = Room.databaseBuilder(this, ShelfTapDatabase::class.java, "shelftap.db")
            .build()

        val httpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val api = Retrofit.Builder()
            .baseUrl(ShelfTapApi.BASE_URL)
            .client(httpClient)
            .addConverterFactory(ShelfTapJson.instance.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ShelfTapApi::class.java)

        repository = CatalogueRepository(
            api = api,
            dao = database.titleDao(),
            prefs = getSharedPreferences("catalogue", MODE_PRIVATE),
            scope = appScope,
        )
        repository.startSyncIfNeeded()
    }
}
