package com.example.libraryapp.data.remote

import com.example.libraryapp.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = sessionManager.getAuthToken()

        val requestBuilder = originalRequest.newBuilder()
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }
        requestBuilder.header("Accept", "application/json")

        val response = chain.proceed(requestBuilder.build())

        // If server responds with 401 Unauthorized, automatically clear the expired token
        if (response.code == 401) {
            sessionManager.clearSession()
        }

        return response
    }
}
