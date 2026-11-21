package vn.shelftap.app.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import vn.shelftap.app.model.HistoryItem
import vn.shelftap.app.model.Loan
import vn.shelftap.app.model.Member
import vn.shelftap.app.model.TitleDetail
import java.net.URLEncoder

/** The server answered with a non-2xx status. [errorCode] is the body's "code" field, if any. */
class ApiException(val status: Int, val errorCode: String?) : Exception("HTTP $status")

class RenewalNotAllowedException : Exception("Loan cannot be renewed")

/**
 * REST client for https://api.shelftap.example. Every call runs on [io] and
 * sends the bearer token from [accessToken]. Nothing here logs requests,
 * responses or headers.
 */
class ShelfTapApi(
    private val client: OkHttpClient,
    private val accessToken: suspend () -> String,
    private val io: CoroutineDispatcher,
    private val baseUrl: String = "https://api.shelftap.example",
) {

    suspend fun myLoans(): List<Loan> = get("/api/me/loans") { parseArray(it, ::parseLoanOrThrow) }

    suspend fun history(): List<HistoryItem> = get("/api/me/history") { parseArray(it, ::parseHistoryOrThrow) }

    suspend fun renewLoan(loanId: String): Loan {
        val request = Request.Builder()
            .url("$baseUrl/api/loans/${encodeSegment(loanId)}/renew")
            .post("".toRequestBody(null))
        return try {
            send(request) { parseLoanOrThrow(JSONObject(it)) }
        } catch (e: ApiException) {
            if (e.status == 409 || e.errorCode == "RENEWAL_NOT_ALLOWED") {
                throw RenewalNotAllowedException()
            }
            throw e
        }
    }

    suspend fun me(): Member = get("/api/me") { parseMemberOrThrow(it) }

    suspend fun setHistoryEnabled(enabled: Boolean): Member {
        val body = JSONObject().put("historyEnabled", enabled).toString()
        val request = Request.Builder()
            .url("$baseUrl/api/me/privacy")
            .put(body.toRequestBody(JSON_MEDIA_TYPE))
        return send(request) { parseMemberOrThrow(it) }
    }

    suspend fun title(titleId: String): TitleDetail =
        get("/api/titles/${encodeSegment(titleId)}") { parseTitleOrThrow(it) }

    private suspend fun <T> get(path: String, parse: (String) -> T): T =
        send(Request.Builder().url(baseUrl + path).get(), parse)

    private suspend fun <T> send(request: Request.Builder, parse: (String) -> T): T =
        withContext(io) {
            val token = accessToken()
            val call = request
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .build()
            client.newCall(call).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw ApiException(response.code, errorCodeOf(body))
                }
                parse(body)
            }
        }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun encodeSegment(value: String): String =
            URLEncoder.encode(value, "UTF-8").replace("+", "%20")

        fun errorCodeOf(body: String): String? = try {
            JSONObject(body).optString("code").ifEmpty { null }
        } catch (e: JSONException) {
            null
        }

        fun parseLoanOrThrow(o: JSONObject): Loan = parseLoan(o)
        fun parseHistoryOrThrow(o: JSONObject): HistoryItem = parseHistoryItem(o)
        fun parseMemberOrThrow(text: String): Member = parseMember(JSONObject(text))
        fun parseTitleOrThrow(text: String): TitleDetail = parseTitle(JSONObject(text))
    }
}
