package com.example.library

import android.app.Application
import com.example.library.data.AuthRepository
import com.example.library.data.TokenStore
import com.example.library.data.api.AuthInterceptor
import com.example.library.data.api.LibraryApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Application class that wires the networking and sign-in dependencies together. */
class LibraryApplication : Application() {

    lateinit var authRepository: AuthRepository
        private set

    override fun onCreate() {
        super.onCreate()

        val tokenStore = TokenStore(this)

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.LIBRARY_API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        authRepository = AuthRepository(
            api = retrofit.create(LibraryApi::class.java),
            tokenStore = tokenStore
        )
    }
}
