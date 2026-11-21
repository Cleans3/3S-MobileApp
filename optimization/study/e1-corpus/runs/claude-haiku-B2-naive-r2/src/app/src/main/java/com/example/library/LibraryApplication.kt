package com.example.library

import android.app.Application
import com.example.library.data.AccountRepository
import com.example.library.data.AuthInterceptor
import com.example.library.data.LibraryApi
import com.example.library.data.SessionStore
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class LibraryApplication : Application() {

    lateinit var accountRepository: AccountRepository
        private set

    override fun onCreate() {
        super.onCreate()

        val session = SessionStore(this)

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(session))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.LIBRARY_API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        accountRepository = AccountRepository(
            api = retrofit.create(LibraryApi::class.java),
            session = session
        )
    }
}
