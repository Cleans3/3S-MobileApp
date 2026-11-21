package vn.shelftap.app.net

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import vn.shelftap.app.auth.AuthRepository
import vn.shelftap.app.data.HistoryItem
import vn.shelftap.app.data.Loan
import vn.shelftap.app.data.Member
import vn.shelftap.app.data.PrivacyUpdate
import vn.shelftap.app.data.ShelfTapJson
import vn.shelftap.app.data.Title
import java.util.concurrent.TimeUnit

const val API_BASE_URL = "https://api.shelftap.example"
const val RENEWAL_NOT_ALLOWED_CODE = "RENEWAL_NOT_ALLOWED"

/** An HTTP error from the API. [errorCode] is the API's error code, if the body had one. */
class ApiException(val httpStatus: Int, val errorCode: String?) :
    Exception("Request failed with HTTP $httpStatus")

/**
 * Typed client for the ShelfTap REST API. Every call runs on [ioDispatcher] and sends the
 * member's bearer token. Nothing from requests or responses is logged.
 */
class ApiClient(
    private val auth: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher,
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun loans(): List<Loan> =
        send("GET", apiUrl("api", "me", "loans"), null, ListSerializer(Loan.serializer()))

    suspend fun history(): List<HistoryItem> =
        send("GET", apiUrl("api", "me", "history"), null, ListSerializer(HistoryItem.serializer()))

    suspend fun renewLoan(loanId: String): Loan =
        send("POST", apiUrl("api", "loans", loanId, "renew"), null, Loan.serializer())

    suspend fun me(): Member =
        send("GET", apiUrl("api", "me"), null, Member.serializer())

    suspend fun setHistoryEnabled(enabled: Boolean): Member = send(
        "PUT",
        apiUrl("api", "me", "privacy"),
        ShelfTapJson.encodeToString(PrivacyUpdate.serializer(), PrivacyUpdate(enabled)),
        Member.serializer(),
    )

    suspend fun title(titleId: String): Title =
        send("GET", apiUrl("api", "titles", titleId), null, Title.serializer())

    private suspend fun <T> send(
        method: String,
        target: HttpUrl,
        jsonBody: String?,
        serializer: KSerializer<T>,
    ): T = withContext(ioDispatcher) {
        val token = auth.accessToken()
        val body: RequestBody? = when {
            jsonBody != null -> jsonBody.toRequestBody(JSON_MEDIA_TYPE)
            method == "GET" -> null
            else -> "".toRequestBody(null)
        }
        val request = Request.Builder()
            .url(target)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .method(method, body)
            .build()

        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw ApiException(response.code, errorCodeOf(text))
            }
            ShelfTapJson.decodeFromString(serializer, text)
        }
    }

    /** Each argument is one path segment, so ids are percent-encoded rather than interpreted. */
    private fun apiUrl(vararg segments: String): HttpUrl =
        API_BASE_URL.toHttpUrl().newBuilder().apply {
            segments.forEach { addPathSegment(it) }
        }.build()

    private fun errorCodeOf(body: String): String? = runCatching {
        ShelfTapJson.parseToJsonElement(body).jsonObject["code"]?.jsonPrimitive?.content
    }.getOrNull()

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
