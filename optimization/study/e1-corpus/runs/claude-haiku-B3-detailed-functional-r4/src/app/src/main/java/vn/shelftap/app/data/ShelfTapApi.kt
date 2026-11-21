package vn.shelftap.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import vn.shelftap.app.auth.AuthRepository
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

const val RENEWAL_NOT_ALLOWED = "RENEWAL_NOT_ALLOWED"

val ShelfJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

class ApiException(
    val httpStatus: Int,
    val code: String?,
) : Exception("HTTP $httpStatus${code?.let { " $it" }.orEmpty()}")

/** Message shown to the member for any failure. */
fun Throwable.userMessage(): String = when (this) {
    is IOException -> "No connection. Check your network and try again."
    is ApiException -> "Request failed (HTTP $httpStatus)."
    else -> "Something went wrong."
}

class ShelfTapApi(
    private val auth: AuthRepository,
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun loans(): List<Loan> =
        call("/api/me/loans", "GET", null, ListSerializer(Loan.serializer()))

    suspend fun history(): List<HistoryItem> =
        call("/api/me/history", "GET", null, ListSerializer(HistoryItem.serializer()))

    suspend fun renew(loanId: String): Loan =
        call("/api/loans/${segment(loanId)}/renew", "POST", null, Loan.serializer())

    suspend fun me(): Me =
        call("/api/me", "GET", null, Me.serializer())

    suspend fun setHistoryEnabled(enabled: Boolean): Me =
        call(
            "/api/me/privacy",
            "PUT",
            ShelfJson.encodeToString(PrivacyUpdate.serializer(), PrivacyUpdate(enabled)),
            Me.serializer(),
        )

    suspend fun title(titleId: String): Title =
        call("/api/titles/${segment(titleId)}", "GET", null, Title.serializer())

    private suspend fun <T> call(
        path: String,
        method: String,
        body: String?,
        serializer: KSerializer<T>,
    ): T = withContext(Dispatchers.IO) {
        val token = auth.accessToken()
        val requestBody = when {
            body != null -> body.toRequestBody(JSON_TYPE)
            method == "POST" || method == "PUT" -> "".toRequestBody()
            else -> null
        }
        val request = Request.Builder()
            .url(BASE_URL + path)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .method(method, requestBody)
            .build()

        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw parseError(response.code, text)
            }
            ShelfJson.decodeFromString(serializer, text)
        }
    }

    private fun parseError(status: Int, text: String): ApiException {
        if (text.contains(RENEWAL_NOT_ALLOWED)) {
            return ApiException(status, RENEWAL_NOT_ALLOWED)
        }
        val code = runCatching {
            ShelfJson.parseToJsonElement(text).jsonObject["code"]?.jsonPrimitive?.content
        }.getOrNull()
        return ApiException(status, code)
    }

    private fun segment(id: String): String =
        URLEncoder.encode(id, "UTF-8").replace("+", "%20")

    private companion object {
        const val BASE_URL = "https://api.shelftap.example"
        val JSON_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
