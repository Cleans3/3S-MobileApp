package vn.shelftap.app.data

import java.net.URLEncoder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import vn.shelftap.app.auth.AuthRepository

/**
 * An HTTP error answered by the server. The response body is never logged or shown to the user.
 */
class ApiException(val httpStatus: Int, val code: String?) :
    Exception("Request failed with HTTP $httpStatus")

/**
 * Thin client for the ShelfTap REST API. Every call runs on [ioDispatcher], so it is safe
 * to call from the main thread.
 */
class ShelfTapApi(
    private val client: OkHttpClient,
    private val authRepository: AuthRepository,
    private val ioDispatcher: CoroutineDispatcher,
    private val baseUrl: String = BASE_URL,
) {
    suspend fun getLoans(): List<Loan> = withContext(ioDispatcher) {
        JSONArray(send("GET", "/api/me/loans")).mapObjects(::loanFromJson)
    }

    suspend fun renewLoan(loanId: String): Loan = withContext(ioDispatcher) {
        loanFromJson(JSONObject(send("POST", "/api/loans/${encodeSegment(loanId)}/renew", "{}")))
    }

    suspend fun getHistory(): List<HistoryItem> = withContext(ioDispatcher) {
        JSONArray(send("GET", "/api/me/history")).mapObjects(::historyItemFromJson)
    }

    suspend fun getMember(): Member = withContext(ioDispatcher) {
        memberFromJson(JSONObject(send("GET", "/api/me")))
    }

    suspend fun updatePrivacy(historyEnabled: Boolean): Member = withContext(ioDispatcher) {
        val body = JSONObject().put("historyEnabled", historyEnabled).toString()
        memberFromJson(JSONObject(send("PUT", "/api/me/privacy", body)))
    }

    suspend fun getTitle(titleId: String): CatalogTitle = withContext(ioDispatcher) {
        catalogTitleFromJson(JSONObject(send("GET", "/api/titles/${encodeSegment(titleId)}")))
    }

    private suspend fun send(method: String, path: String, jsonBody: String? = null): String =
        withContext(ioDispatcher) {
            val token = authRepository.accessToken()
            val body = if (method == "GET") null else (jsonBody ?: "").toRequestBody(JSON_TYPE)
            val request = Request.Builder()
                .url(baseUrl + path)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .method(method, body)
                .build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw ApiException(response.code, errorCodeOf(text))
                }
                text
            }
        }

    private fun errorCodeOf(text: String): String? = try {
        JSONObject(text).optString("code").ifEmpty { null }
    } catch (e: JSONException) {
        null
    }

    private fun encodeSegment(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    companion object {
        const val BASE_URL = "https://api.shelftap.example"
        private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
