package shelftap.android.api

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.ConnectionSpec
import shelftap.android.data.Repository
import shelftap.android.session.FakeVault
import shelftap.android.session.SessionManager

/** The real client stack (Retrofit, OkHttp, authenticator, session) against a MockWebServer over plain HTTP. */
class ApiHarness(val vault: FakeVault = FakeVault()) : AutoCloseable {
    val server = MockWebServer().apply { start() }
    private val baseUrl = server.url("/")
    private val base = Net.baseClient(baseUrl, emptyList(), listOf(ConnectionSpec.CLEARTEXT))
    val session = SessionManager(Net.retrofit(base, baseUrl).create(AuthApi::class.java), vault, { "device-1" }, { "ok" })
    val repo = Repository(Net.retrofit(Net.authedClient(base, session), baseUrl).create(ShelfTapApi::class.java), session)

    fun enqueue(status: Int, body: String = "", vararg headers: Pair<String, String>) {
        server.enqueue(
            MockResponse.Builder().code(status).apply {
                headers.forEach { (k, v) -> addHeader(k, v) }
                if (body.isNotEmpty()) addHeader("Content-Type", "application/json").body(body)
            }.build(),
        )
    }

    fun enqueueTokens(access: String, refresh: String, mustChangePassword: Boolean = false) =
        enqueue(200, tokenPair(access, refresh, mustChangePassword))

    fun enqueueProblem(status: Int, code: String, vararg headers: Pair<String, String>) =
        enqueue(
            status,
            """{"type":"about:blank","title":"t","status":$status,"detail":"secret detail 123","code":"$code"}""",
            *headers,
        )

    override fun close() = server.close()

    companion object {
        fun tokenPair(access: String, refresh: String, mustChangePassword: Boolean = false) =
            """{"accessToken":"$access","accessExpiresIn":900,"refreshToken":"$refresh","mustChangePassword":$mustChangePassword}"""

        const val ME_JSON = """{"memberId":"m1","fullName":"An Nguyen","email":"an@x.vn","memberType":"STUDENT","status":"ACTIVE","historyEnabled":true,"mustChangePassword":false}"""
    }
}
