package com.example.libraryapp.data.api

import okhttp3.Interceptor
import okhttp3.Response

/** Holds the bearer token that is attached to every outgoing request while a member is signed in. */
class AuthTokenHolder {
    @Volatile
    var token: String? = null
}

class AuthInterceptor(private val tokenHolder: AuthTokenHolder) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokenHolder.token ?: return chain.proceed(request)
        val authenticated = request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(authenticated)
    }
}
