package com.example.librarycatalogue

import android.app.Application
import com.example.librarycatalogue.data.BookRepository
import com.example.librarycatalogue.data.LibraryApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

class LibraryApplication : Application() {

    /** Shared repository; created lazily so app start-up stays cheap. */
    val repository: BookRepository by lazy { BookRepository(createApi()) }

    private fun createApi(): LibraryApi {
        val json = Json { ignoreUnknownKeys = true }
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.LIBRARY_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(LibraryApi::class.java)
    }
}
