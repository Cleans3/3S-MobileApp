package vn.shelftap.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ShelfRepository(private val context: Context, private val auth: AuthRepository) {
    private val prefs = context.getSharedPreferences("shelf_cache", Context.MODE_PRIVATE)
    private val base = "https://api.shelftap.example"

    suspend fun loans(): Pair<List<Loan>, Boolean> = cachedList("loans") { get("/api/me/loans").let(::parseLoans) }
    suspend fun history(): Pair<List<HistoryItem>, Boolean> = cachedList("history") { get("/api/me/history").let(::parseHistory) }
    suspend fun member(): Member = parseMember(get("/api/me"))
    suspend fun title(id: String): TitleDetail = parseTitle(get("/api/titles/$id"))
    suspend fun renew(id: String): Loan = parseLoan(post("/api/loans/$id/renew"))
    suspend fun setHistory(enabled: Boolean): Member = parseMember(put("/api/me/privacy", JSONObject().put("historyEnabled", enabled).toString()))

    private suspend fun <T> cachedList(key: String, request: suspend () -> List<T>): Pair<List<T>, Boolean> = withContext(Dispatchers.IO) {
        try { val value = request(); prefs.edit().putString(key, JSONArray(value.map { it.toJson() }).toString()).apply(); value to false }
        catch (_: Exception) { val raw = prefs.getString(key, "[]") ?: "[]"; JSONArray(raw).let { arr -> (0 until arr.length()).map { fromJson(key, arr.getJSONObject(it)) } as List<T> } to true }
    }
    private fun Any.toJson(): JSONObject = when (this) { is Loan -> JSONObject().apply { put("loanId", loanId); put("copyId", copyId); put("titleId", titleId); put("title", title); put("barcode", barcode); put("outAt", outAt); put("dueAt", dueAt); put("returnedAt", returnedAt); put("renewals", renewals); put("renewable", renewable) }; is HistoryItem -> JSONObject().apply { put("titleId", titleId); put("title", title); put("outAt", outAt); put("returnedAt", returnedAt) }; else -> JSONObject() }
    @Suppress("UNCHECKED_CAST") private fun <T> fromJson(key: String, o: JSONObject): T = if (key == "loans") parseLoan(o) as T else parseHistoryItem(o) as T
    private suspend fun get(path: String) = request("GET", path, null)
    private suspend fun post(path: String) = request("POST", path, null)
    private suspend fun put(path: String, body: String) = request("PUT", path, body)
    private suspend fun request(method: String, path: String, body: String?): String = withContext(Dispatchers.IO) {
        val c = URL(base + path).openConnection() as HttpURLConnection
        try { c.requestMethod = method; c.setRequestProperty("Authorization", "Bearer ${auth.accessToken()}"); c.setRequestProperty("Accept", "application/json"); c.setRequestProperty("Content-Type", "application/json"); if (body != null) { c.doOutput = true; c.outputStream.use { it.write(body.toByteArray()) } }; val code = c.responseCode; val stream = if (code in 200..299) c.inputStream else c.errorStream; val text = stream?.bufferedReader()?.use { it.readText() } ?: "{}"; if (code !in 200..299) throw ApiException(code, runCatching { JSONObject(text).optString("code") }.getOrNull()); text } finally { c.disconnect() }
    }
    class ApiException(val status: Int, val code: String?) : Exception()

    private fun parseLoans(s: String) = JSONArray(s).let { a -> (0 until a.length()).map { parseLoan(a.getJSONObject(it)) } }
    private fun parseHistory(s: String) = JSONArray(s).let { a -> (0 until a.length()).map { parseHistoryItem(a.getJSONObject(it)) } }
    private fun parseLoan(o: JSONObject) = Loan(o.optString("loanId"), o.optString("copyId"), o.optString("titleId"), o.optString("title"), o.optString("barcode"), o.optString("outAt"), o.optString("dueAt"), nullable(o, "returnedAt"), o.optInt("renewals"), o.optBoolean("renewable"))
    private fun parseHistoryItem(o: JSONObject) = HistoryItem(o.optString("titleId"), o.optString("title"), o.optString("outAt"), nullable(o, "returnedAt"))
    private fun parseMember(s: String) = JSONObject(s).let { Member(it.optString("memberId"), it.optString("fullName"), it.optString("email"), it.optString("memberType"), it.optString("status"), it.optBoolean("historyEnabled"), it.optBoolean("mustChangePassword")) }
    private fun parseTitle(s: String) = JSONObject(s).let { parseTitle(it) }
    private fun parseTitle(o: JSONObject) = TitleDetail(o.optString("titleId"), o.optString("isbn"), o.optString("title"), strings(o.optJSONArray("authors")), strings(o.optJSONArray("subjects")), o.optString("coverUrl").takeIf { it.isNotBlank() }, o.optInt("copiesAvailable"), o.optInt("copiesTotal"))
    private fun strings(a: JSONArray?) = a?.let { (0 until it.length()).map { i -> it.optString(i) } } ?: emptyList()
    private fun nullable(o: JSONObject, key: String): String? = if (o.isNull(key)) null else o.optString(key).takeIf { it.isNotBlank() && it != "null" }
}
