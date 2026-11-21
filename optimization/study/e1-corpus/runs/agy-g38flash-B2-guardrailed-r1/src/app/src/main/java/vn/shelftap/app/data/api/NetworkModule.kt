package vn.shelftap.app.data.api

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import vn.shelftap.app.data.local.EncryptedSessionStorage
import java.util.concurrent.TimeUnit

class NetworkModule(
    private val sessionStorage: EncryptedSessionStorage,
    private val onSessionExpired: () -> Unit
) {
    companion object {
        const val BASE_URL = "https://api.shelftap.example/"
    }

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val authInterceptor = AuthInterceptor(
        sessionStorage = sessionStorage,
        json = json,
        onSessionExpired = onSessionExpired
    )

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val apiService: ShelfTapApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ShelfTapApiService::class.java)
}
