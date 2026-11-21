package vn.shelftap.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

const val API_BASE_URL = "https://api.shelftap.example"

/** Thrown when the server cannot be reached (no connectivity, timeouts, DNS failures). */
class NetworkUnavailableException(cause: IOException) : IOException("Network unavailable", cause)

/** Thrown for a non-2xx response. [code] is the server's error code, e.g. RENEWAL_NOT_ALLOWED. */
class ApiException(val status: Int, val code: String?, message: String) : Exception(message)

class ShelfTapApi(
    private val json: Json,
    private val baseUrl: String = API_BASE_URL,
) {
    suspend fun getLoans(token: String): List<Loan> =
        json.decodeFromString(ListSerializer(Loan.serializer()), request("GET", "/api/me/loans", token))

    suspend fun getHistory(token: String): List<HistoryItem> =
        json.decodeFromString(ListSerializer(HistoryItem.serializer()), request("GET", "/api/me/history", token))

    suspend fun renewLoan(token: String, loanId: String): Loan =
        json.decodeFromString(
            Loan.serializer(),
            request("POST", "/api/loans/${encodeSegment(loanId)}/renew", token, body = ""),
        )

    suspend fun getMember(token: String): Member =
        json.decodeFromString(Member.serializer(), request("GET", "/api/me", token))

    suspend fun setHistoryEnabled(token: String, enabled: Boolean): Member {
        val body = json.encodeToString(PrivacyUpdate.serializer(), PrivacyUpdate(enabled))
        return json.decodeFromString(Member.serializer(), request("PUT", "/api/me/privacy", token, body))
    }

    suspend fun getTitle(token: String, titleId: String): TitleDetail =
        json.decodeFromString(
            TitleDetail.serializer(),
            request("GET", "/api/titles/${encodeSegment(titleId)}", token),
        )

    private suspend fun request(
        method: String,
        path: String,
        token: String,
        body: String? = null,
    ): String = withContext(Dispatchers.IO) {
        val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            if (body != null) {
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val error = runCatching { json.decodeFromString(ApiErrorBody.serializer(), text) }.getOrNull()
                throw ApiException(
                    status = status,
                    code = error?.code,
                    message = error?.message ?: "Request failed ($status)",
                )
            }
            text
        } catch (e: IOException) {
            throw NetworkUnavailableException(e)
        } finally {
            connection.disconnect()
        }
    }

    private fun encodeSegment(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
