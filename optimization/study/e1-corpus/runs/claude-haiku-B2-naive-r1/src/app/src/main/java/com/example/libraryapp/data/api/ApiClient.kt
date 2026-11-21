package com.example.libraryapp.data.api

import com.example.libraryapp.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Bearer token of the current session. Written by the repository on sign-in and
 * sign-out, read by the interceptor on every request.
 */
object AuthToken {
    @Volatile
    var value: String? = null
}

object ApiClient {

    fun create(): LibraryApi {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = AuthToken.value
                val request = if (token.isNullOrBlank()) {
                    chain.request()
                } else {
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                }
                chain.proceed(request)
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.LIBRARY_API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LibraryApi::class.java)
    }
}
