package vn.shelftap.app.di

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import vn.shelftap.app.data.api.AuthInterceptor
import vn.shelftap.app.data.api.ShelfTapApiService
import vn.shelftap.app.data.api.TokenProvider
import vn.shelftap.app.data.repository.SessionRepository
import vn.shelftap.app.data.security.DeviceIdProvider
import vn.shelftap.app.data.security.KeystoreCrypto
import java.util.concurrent.TimeUnit

class AppContainer(private val context: Context) {

    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    val mainDispatcher: CoroutineDispatcher = Dispatchers.Main

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    val keystoreCrypto: KeystoreCrypto by lazy {
        KeystoreCrypto()
    }

    val deviceIdProvider: DeviceIdProvider by lazy {
        DeviceIdProvider(context, keystoreCrypto, ioDispatcher)
    }

    // Raw OkHttpClient without AuthInterceptor for unauthenticated calls and token refresh
    val rawOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // TokenProvider delegate pointing to sessionRepository
    private val tokenProviderDelegate = object : TokenProvider {
        override fun getAccessToken(): String? = sessionRepository.getAccessToken()
        override fun refreshTokenSynchronized(failedToken: String?): String? =
            sessionRepository.refreshTokenSynchronized(failedToken)
        override fun notifySessionExpired() = sessionRepository.notifySessionExpired()
    }

    // Authenticated OkHttpClient with AuthInterceptor for automatic Bearer token injection and refresh retry
    val authenticatedOkHttpClient: OkHttpClient by lazy {
        val authInterceptor = AuthInterceptor(tokenProviderDelegate, json)
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .build()
    }

    val retrofit: Retrofit by lazy {
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl("https://api.shelftap.example/")
            .client(authenticatedOkHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    val apiService: ShelfTapApiService by lazy {
        retrofit.create(ShelfTapApiService::class.java)
    }

    val sessionRepository: SessionRepository by lazy {
        SessionRepository(
            context = context,
            crypto = keystoreCrypto,
            deviceIdProvider = deviceIdProvider,
            apiService = apiService,
            rawOkHttpClient = rawOkHttpClient,
            json = json,
            ioDispatcher = ioDispatcher
        )
    }
}
