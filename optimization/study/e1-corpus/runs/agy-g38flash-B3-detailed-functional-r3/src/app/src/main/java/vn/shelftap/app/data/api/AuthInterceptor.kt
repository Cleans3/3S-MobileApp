package vn.shelftap.app.data.api

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import vn.shelftap.app.data.auth.AuthRepository

class AuthInterceptor(
    private val authRepository: AuthRepository
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking {
            try {
                authRepository.accessToken()
            } catch (e: Exception) {
                ""
            }
        }

        val requestBuilder = chain.request().newBuilder()
            .header("Accept", "application/json")

        if (token.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
