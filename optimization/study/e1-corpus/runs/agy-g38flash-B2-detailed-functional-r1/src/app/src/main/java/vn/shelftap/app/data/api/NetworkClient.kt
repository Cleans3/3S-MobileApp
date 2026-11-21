package vn.shelftap.app.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import vn.shelftap.app.data.local.DeviceIdProvider
import vn.shelftap.app.data.local.SessionManager
import java.util.concurrent.TimeUnit

object NetworkClient {

    private const val BASE_URL = "https://api.shelftap.example/"

    fun createApiService(
        sessionManager: SessionManager,
        deviceIdProvider: DeviceIdProvider
    ): ShelfTapApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = AuthInterceptor(
            sessionManager = sessionManager,
            deviceIdProvider = deviceIdProvider,
            baseUrl = BASE_URL
        )

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ShelfTapApiService::class.java)
    }
}
