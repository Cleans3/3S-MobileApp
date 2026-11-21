package vn.shelftap.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class ShelfTapApi(private val auth: AuthRepository, private val client: OkHttpClient = OkHttpClient()) {
    private val base = "https://api.shelftap.example"
    private suspend fun request(path: String, method: String = "GET", body: String? = null): String = withContext(Dispatchers.IO) {
        val token = auth.accessToken()
        val builder = Request.Builder().url(base + path).header("Authorization", "Bearer $token").header("Accept", "application/json")
        if (body != null) builder.method(method, body.toRequestBody("application/json".toMediaType())) else builder.method(method, null)
        client.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw ApiException(response.code, runCatching { JSONObject(text).optString("code") }.getOrDefault(""))
            text
        }
    }
    suspend fun loans() = JSONArray(request("/api/me/loans")).toLoans()
    suspend fun history() = JSONArray(request("/api/me/history")).toHistory()
    suspend fun member() = JSONObject(request("/api/me")).toMember()
    suspend fun updateHistory(enabled: Boolean) = JSONObject(request("/api/me/privacy", "PUT", JSONObject().put("historyEnabled", enabled).toString())).toMember()
    suspend fun renew(id: String) = JSONObject(request("/api/loans/$id/renew", "POST", "{}" )).toLoan()
    suspend fun title(id: String) = JSONObject(request("/api/titles/$id")).toBook()
}
class ApiException(val status: Int, val code: String) : Exception()

fun JSONObject.toLoan() = Loan(optString("loanId"), optString("copyId"), optString("titleId"), optString("title"), optString("barcode"), optString("outAt"), optString("dueAt"), optString("returnedAt").ifEmpty { null }, optInt("renewals"), optBoolean("renewable"))
fun JSONArray.toLoans() = (0 until length()).map { getJSONObject(it).toLoan() }
fun JSONObject.toHistory() = HistoryItem(optString("titleId"), optString("title"), optString("outAt"), optString("returnedAt").ifEmpty { null })
fun JSONArray.toHistory() = (0 until length()).map { getJSONObject(it).toHistory() }
private fun JSONObject.toMember() = Member(optString("memberId"), optString("fullName"), optString("email"), optString("memberType"), optString("status"), optBoolean("historyEnabled"), optBoolean("mustChangePassword"))
private fun JSONObject.toBook() = BookDetail(optString("titleId"), optString("isbn"), optString("title"), optJSONArray("authors").toStrings(), optJSONArray("subjects").toStrings(), optString("coverUrl").ifEmpty { null }, optInt("copiesAvailable"), optInt("copiesTotal"))
private fun JSONArray?.toStrings() = if (this == null) emptyList() else (0 until length()).map { optString(it) }
