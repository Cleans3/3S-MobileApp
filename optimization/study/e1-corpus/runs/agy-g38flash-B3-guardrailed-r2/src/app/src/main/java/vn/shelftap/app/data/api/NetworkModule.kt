package vn.shelftap.app.data.api

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import vn.shelftap.app.data.repository.AuthRepository
import java.util.concurrent.TimeUnit

object NetworkModule {

    // HTTPS used strictly as per network security rule
    private const val BASE_URL = "https://api.shelftap.example"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    fun createShelfTapApi(authRepository: AuthRepository): ShelfTapApi {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(authRepository))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        val contentType = "application/json".toMediaType()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        return retrofit.create(ShelfTapApi::class.java)
    }
}
