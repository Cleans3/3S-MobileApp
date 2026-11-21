package vn.shelftap.app.data

import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response as OkHttpResponse
import okhttp3.Route
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val BASE_URL = "https://api.shelftap.example/"

private class BearerInterceptor(private val session: SessionStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): OkHttpResponse {
        val token = session.accessToken()
        val request = if (token == null) chain.request() else chain.request().newBuilder()
            .header("Authorization", "Bearer $token").build()
        return chain.proceed(request)
    }
}

private class TokenAuthenticator(private val session: SessionStore) : Authenticator {
    private val lock = Any()
    override fun authenticate(route: Route?, response: OkHttpResponse): Request? {
        if (responseCount(response) >= 2) return null
        val code = runCatching {
            response.peekBody(16 * 1024).string().let { JSONObject(it).optString("code") }
        }.getOrNull()
        if (code != "TOKEN_INVALID") return null

        synchronized(lock) {
            val usedToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            val current = session.accessToken()
            if (current != null && current != usedToken) {
                return response.request.newBuilder().header("Authorization", "Bearer $current").build()
            }
            val refresh = session.refreshToken() ?: return null
            val client = OkHttpClient()
            val body = JSONObject().put("refreshToken", refresh).put("deviceId", session.deviceId)
                .toString().toRequestBody("application/json".toMediaType())
            val refreshRequest = Request.Builder().url(BASE_URL + "api/auth/refresh").post(body).build()
            val refreshResponse = runCatching { client.newCall(refreshRequest).execute() }.getOrNull() ?: return null
            refreshResponse.use {
                if (!it.isSuccessful) { session.clear(); return null }
                val json = JSONObject(it.body?.string().orEmpty())
                session.saveAuth(AuthResponse(json.getString("accessToken"), json.optLong("accessExpiresIn"),
                    json.getString("refreshToken"), json.optBoolean("mustChangePassword")))
            }
            return response.request.newBuilder().header("Authorization", "Bearer ${session.accessToken()}").build()
        }
    }

    private fun responseCount(response: OkHttpResponse): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) { result++; prior = prior.priorResponse }
        return result
    }
}

object ApiClient {
    fun create(session: SessionStore): ShelfTapApi {
        val client = OkHttpClient.Builder().addInterceptor(BearerInterceptor(session))
            .authenticator(TokenAuthenticator(session)).build()
        return Retrofit.Builder().baseUrl(BASE_URL).client(client)
            .addConverterFactory(GsonConverterFactory.create()).build().create(ShelfTapApi::class.java)
    }
}
