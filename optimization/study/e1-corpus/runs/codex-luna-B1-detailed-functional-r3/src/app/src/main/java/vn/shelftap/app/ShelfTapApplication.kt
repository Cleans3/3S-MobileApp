package vn.shelftap.app

import android.app.Application
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import vn.shelftap.app.data.CatalogueApi
import vn.shelftap.app.data.CatalogueRepository
import vn.shelftap.app.data.ShelfTapDatabase

class ShelfTapApplication : Application() {
    val repository: CatalogueRepository by lazy {
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val api = Retrofit.Builder().baseUrl("https://api.shelftap.example/")
            .addConverterFactory(MoshiConverterFactory.create(moshi)).build().create(CatalogueApi::class.java)
        CatalogueRepository(ShelfTapDatabase.create(this).titleDao(), api)
    }
}
