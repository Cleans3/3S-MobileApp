package shelftap.android.api

import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.CertificatePinner
import okhttp3.ConnectionSpec
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** What the HTTP layer needs from the session: the live access token and a way to renew it after a 401. */
interface TokenSource {
    fun accessToken(): String?

    /** Blocking. Returns a usable access token after [failedToken] was rejected, or null to give up. */
    fun refreshAfterUnauthorized(failedToken: String): String?
}

object Net {
    const val INTEGRITY_HEADER = "X-Device-Integrity"

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    /**
     * TLS 1.2/1.3 only, no redirects, optional SPKI pins for the API host. [connectionSpecs] is a seam for
     * tests that talk to a plain-HTTP MockWebServer; the app always uses the default.
     */
    fun baseClient(
        baseUrl: HttpUrl,
        pins: List<String>,
        connectionSpecs: List<ConnectionSpec> = listOf(ConnectionSpec.RESTRICTED_TLS),
    ): OkHttpClient = OkHttpClient.Builder()
        .connectionSpecs(connectionSpecs)
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .apply {
            if (pins.isNotEmpty()) {
                certificatePinner(CertificatePinner.Builder().apply { pins.forEach { add(baseUrl.host, it) } }.build())
            }
        }
        .build()

    fun authedClient(base: OkHttpClient, tokens: TokenSource): OkHttpClient = base.newBuilder()
        .addInterceptor(BearerInterceptor(tokens))
        .authenticator(TokenAuthenticator(tokens))
        .build()

    fun retrofit(client: OkHttpClient, baseUrl: HttpUrl): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}

private class BearerInterceptor(private val tokens: TokenSource) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokens.accessToken() ?: return chain.proceed(chain.request())
        return chain.proceed(chain.request().newBuilder().header("Authorization", "Bearer $token").build())
    }
}

/** On 401 TOKEN_INVALID: one refresh, one retry. A second 401 (priorResponse set) is final. */
private class TokenAuthenticator(private val tokens: TokenSource) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse != null) return null
        val used = response.request.header("Authorization")?.removePrefix("Bearer ") ?: return null
        if (problemCode(response.peekBody(PEEK_BYTES).string()) != ErrorCode.TOKEN_INVALID) return null
        val fresh = tokens.refreshAfterUnauthorized(used) ?: return null
        return response.request.newBuilder().header("Authorization", "Bearer $fresh").build()
    }

    private companion object {
        const val PEEK_BYTES = 8192L
    }
}
