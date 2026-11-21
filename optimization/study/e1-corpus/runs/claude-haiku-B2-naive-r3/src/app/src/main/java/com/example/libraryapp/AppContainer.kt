package com.example.libraryapp

import android.content.Context
import com.example.libraryapp.data.AccountRepository
import com.example.libraryapp.data.api.AuthInterceptor
import com.example.libraryapp.data.api.AuthTokenHolder
import com.example.libraryapp.data.api.LibraryApi
import com.example.libraryapp.data.session.TokenStorage
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/** Manual dependency container; creates the network stack and the account repository once per app. */
class AppContainer(context: Context) {

    private val tokenHolder = AuthTokenHolder()
    private val tokenStorage = TokenStorage(context)

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(tokenHolder))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val api: LibraryApi = retrofit.create(LibraryApi::class.java)

    val accountRepository: AccountRepository = AccountRepository(api, tokenStorage, tokenHolder)
}
