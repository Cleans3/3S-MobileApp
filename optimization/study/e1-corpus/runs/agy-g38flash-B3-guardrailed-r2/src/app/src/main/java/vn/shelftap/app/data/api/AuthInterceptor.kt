package vn.shelftap.app.data.api

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import vn.shelftap.app.data.repository.AuthRepository

class AuthInterceptor(
    private val authRepository: AuthRepository
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        // AuthRepository.accessToken() is called to retrieve the current bearer token
        val token = runBlocking { authRepository.accessToken() }
        val authenticatedRequest = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .build()
        return chain.proceed(authenticatedRequest)
    }
}
