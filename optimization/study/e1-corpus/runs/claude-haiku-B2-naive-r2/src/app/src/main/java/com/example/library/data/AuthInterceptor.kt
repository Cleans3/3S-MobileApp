package com.example.library.data

import okhttp3.Interceptor
import okhttp3.Response

/** Adds "Authorization: Bearer <token>" to every request while a session exists. */
class AuthInterceptor(private val session: SessionStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = session.token
        val request = if (token != null && original.header(HEADER) == null) {
            original.newBuilder().header(HEADER, "Bearer $token").build()
        } else {
            original
        }
        return chain.proceed(request)
    }

    private companion object {
        const val HEADER = "Authorization"
    }
}
