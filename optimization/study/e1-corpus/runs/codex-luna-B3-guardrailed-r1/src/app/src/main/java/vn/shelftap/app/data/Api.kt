package vn.shelftap.app.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

interface AuthRepository { suspend fun accessToken(): String }

internal fun JSONObject.optionalString(key: String): String? = if (isNull(key)) null else optString(key)
internal fun JSONObject.str(key: String) = optString(key)

class ShelfTapApi(private val auth: AuthRepository, private val io: CoroutineDispatcher = Dispatchers.IO) {
    private val baseUrl = "https://api.shelftap.example"

    suspend fun loans(): List<Loan> = getArray("/api/me/loans").map { it.loan() }
    suspend fun history(): List<HistoryItem> = getArray("/api/me/history").map { HistoryItem(it.str("titleId"), it.str("title"), it.str("outAt"), it.optionalString("returnedAt")) }
    suspend fun member(): Member = getObject("/api/me").member()
    suspend fun detail(id: String): BookDetail = getObject("/api/titles/${java.net.URLEncoder.encode(id, "UTF-8")}").detail()
    suspend fun renew(id: String): Loan = request("POST", "/api/loans/${java.net.URLEncoder.encode(id, "UTF-8")}/renew").let { it.loan() }
    suspend fun updateHistory(enabled: Boolean): Member = request("PUT", "/api/me/privacy", JSONObject().put("historyEnabled", enabled)).member()

    private suspend fun getArray(path: String): List<JSONObject> = withContext(io) { requestBlocking("GET", path, null).let { array -> (0 until array.length()).map { array.getJSONObject(it) } } }
    private suspend fun getObject(path: String): JSONObject = withContext(io) { requestBlocking("GET", path, null).getJSONObject(0) }
    private suspend fun request(method: String, path: String, body: JSONObject? = null): JSONObject = withContext(io) { requestBlocking(method, path, body).getJSONObject(0) }

    private suspend fun requestBlocking(method: String, path: String, body: JSONObject?): JSONArray {
        val accessToken = auth.accessToken()
        val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method; connectTimeout = 15_000; readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $accessToken")
            if (body != null) { doOutput = true; setRequestProperty("Content-Type", "application/json") }
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw ApiException(status, runCatching { JSONObject(text).optString("code") }.getOrDefault(""))
            return if (text.trimStart().startsWith("[")) JSONArray(text) else JSONArray().put(JSONObject(text))
        } finally { connection.disconnect() }
    }
}

class ApiException(val status: Int, val code: String) : Exception()
internal fun JSONObject.loan() = Loan(str("loanId"), str("copyId"), str("titleId"), str("title"), str("barcode"), str("outAt"), str("dueAt"), optionalString("returnedAt"), optInt("renewals"), optBoolean("renewable"))
private fun JSONObject.member() = Member(str("memberId"), str("fullName"), str("email"), str("memberType"), str("status"), optBoolean("historyEnabled"), optBoolean("mustChangePassword"))
private fun JSONObject.detail() = BookDetail(str("titleId"), str("isbn"), str("title"), stringList("authors"), stringList("subjects"), optionalString("coverUrl"), optInt("copiesAvailable"), optInt("copiesTotal"))
private fun JSONObject.stringList(key: String): List<String> = optJSONArray(key)?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList()
