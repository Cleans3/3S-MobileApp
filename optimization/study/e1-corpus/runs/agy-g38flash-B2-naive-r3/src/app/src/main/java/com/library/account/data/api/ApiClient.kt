package com.library.account.data.api

import com.google.gson.GsonBuilder
import com.library.account.data.session.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Factory and provider for configured Retrofit REST API service instances.
 */
object ApiClient {

    private const val DEFAULT_BASE_URL = "https://api.library.example.org/"

    @Volatile
    private var apiService: LibraryApiService? = null

    fun getApiService(sessionManager: SessionManager, baseUrl: String = DEFAULT_BASE_URL): LibraryApiService {
        return apiService ?: synchronized(this) {
            apiService ?: createApiService(sessionManager, baseUrl).also { apiService = it }
        }
    }

    private fun createApiService(sessionManager: SessionManager, baseUrl: String): LibraryApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .addInterceptor(MockLibraryApiInterceptor(sessionManager))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        val gson = GsonBuilder()
            .setLenient()
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        return retrofit.create(LibraryApiService::class.java)
    }
}
