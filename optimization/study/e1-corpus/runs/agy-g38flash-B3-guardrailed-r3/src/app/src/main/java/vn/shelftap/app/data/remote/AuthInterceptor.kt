package vn.shelftap.app.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import vn.shelftap.app.data.repository.AuthRepository

/**
 * Interceptor that injects Bearer authorization token into requests.
 * Uses HTTPS exclusively. Never logs tokens or sensitive headers.
 */
class AuthInterceptor(
    private val authRepository: AuthRepository
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // Interceptor runs on OkHttp background thread pool, safe for blocking retrieval
        val token = runBlocking {
            authRepository.accessToken()
        }

        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(request)
    }
}
