package vn.shelftap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.room.Room
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import vn.shelftap.app.data.CatalogueApi
import vn.shelftap.app.data.CatalogueDatabase
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.ui.ShelfTapApp
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val database = Room.databaseBuilder(applicationContext, CatalogueDatabase::class.java, "catalogue.db").build()
        val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
        val api = Retrofit.Builder().baseUrl("https://api.shelftap.example/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build().create(CatalogueApi::class.java)
        setContent { ShelfTapApp(CatalogueRepository(database, api, Dispatchers.IO)) }
    }
}
