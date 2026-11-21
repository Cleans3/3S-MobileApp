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
        val builder = Request.Builder().url(base + path).header("Authorization", "Bearer ${auth.accessToken()}")
        if (method == "PUT" || method == "POST") builder.method(method, body.orEmpty().toRequestBody("application/json".toMediaType()))
        client.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty(); if (!response.isSuccessful) throw ApiException(response.code, JSONObject(text).optString("code")); text
        }
    }
    suspend fun loans() = JSONArray(request("/api/me/loans")).toLoans()
    suspend fun history() = JSONArray(request("/api/me/history")).toHistory()
    suspend fun renew(id: String) = JSONObject(request("/api/loans/$id/renew", "POST")).toLoan()
    suspend fun member() = JSONObject(request("/api/me")).toMember()
    suspend fun setHistory(enabled: Boolean) = JSONObject(request("/api/me/privacy", "PUT", JSONObject().put("historyEnabled", enabled).toString())).toMember()
    suspend fun title(id: String) = JSONObject(request("/api/titles/$id")).toTitle()
}
class ApiException(val status: Int, val code: String) : Exception()
private fun JSONObject.s(key: String) = optString(key)
private fun JSONObject.toLoan() = Loan(s("loanId"), s("copyId"), s("titleId"), s("title"), s("barcode"), s("outAt"), s("dueAt"), if (has("returnedAt") && !isNull("returnedAt")) s("returnedAt") else null, optInt("renewals"), optBoolean("renewable"))
internal fun JSONArray.toLoans() = (0 until length()).map { getJSONObject(it).toLoan() }
internal fun JSONArray.toHistory() = (0 until length()).map { val o=getJSONObject(it); HistoryItem(o.s("titleId"), o.s("title"), o.s("outAt"), if (o.has("returnedAt")&&!o.isNull("returnedAt")) o.s("returnedAt") else null) }
private fun JSONObject.toMember() = Member(s("memberId"), s("fullName"), s("email"), s("memberType"), s("status"), optBoolean("historyEnabled"), optBoolean("mustChangePassword"))
private fun JSONObject.toTitle() = TitleDetail(s("titleId"), s("isbn"), s("title"), optJSONArray("authors").strings(), optJSONArray("subjects").strings(), if (has("coverUrl")&&!isNull("coverUrl")) s("coverUrl") else null, optInt("copiesAvailable"), optInt("copiesTotal"))
private fun JSONArray?.strings() = if (this == null) emptyList() else (0 until length()).map { optString(it) }
