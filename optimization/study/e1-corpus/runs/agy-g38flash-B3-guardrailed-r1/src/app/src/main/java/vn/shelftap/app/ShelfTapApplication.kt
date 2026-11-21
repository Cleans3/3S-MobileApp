package vn.shelftap.app

import android.app.Application
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import vn.shelftap.app.data.api.AuthInterceptor
import vn.shelftap.app.data.api.ShelfTapApiService
import vn.shelftap.app.data.repository.AuthRepository
import vn.shelftap.app.data.repository.EncryptedAuthRepository
import vn.shelftap.app.data.repository.LibraryRepository
import vn.shelftap.app.data.security.EncryptedStorage
import vn.shelftap.app.data.security.KeystoreCrypto
import java.util.concurrent.TimeUnit

class ShelfTapApplication : Application() {

    lateinit var keystoreCrypto: KeystoreCrypto
        private set
    lateinit var encryptedStorage: EncryptedStorage
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var libraryRepository: LibraryRepository
        private set

    override fun onCreate() {
        super.onCreate()

        keystoreCrypto = KeystoreCrypto()
        encryptedStorage = EncryptedStorage(this, keystoreCrypto)
        authRepository = EncryptedAuthRepository(encryptedStorage)

        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            isLenient = true
        }

        // Rule 7: Logging interceptor level is NONE in release
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }

        // Rule 1 & 2: HTTPS only, standard OkHttpClient without bypassing certificates
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(authRepository))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.shelftap.example/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        val apiService = retrofit.create(ShelfTapApiService::class.java)
        libraryRepository = LibraryRepository(apiService, encryptedStorage, json)
    }
}
