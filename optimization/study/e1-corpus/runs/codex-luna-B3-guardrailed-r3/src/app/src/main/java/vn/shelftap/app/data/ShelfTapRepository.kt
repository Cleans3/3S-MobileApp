package vn.shelftap.app.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Cached<T>(val value: T, val offline: Boolean)
class ShelfTapRepository(context: Context, auth: AuthRepository, private val io: CoroutineDispatcher = Dispatchers.IO) {
    private val api = ShelfTapApi(auth, io); private val store = EncryptedStore(context); private val json = Json { ignoreUnknownKeys = true }
    private fun cached(): Map<String, String> = store.read()?.split("\n", limit = 3)?.associate { it.substringBefore("\u0000") to it.substringAfter("\u0000") } ?: emptyMap()
    private fun save(key: String, value: String) { val all = cached().toMutableMap(); all[key] = value; store.write(all.entries.joinToString("\n") { "${it.key}\u0000${it.value}" }) }
    suspend fun loans(): Cached<List<Loan>> = try { api.loans().also { withContext(io) { save("loans", json.encodeToString(it)) } }.let { Cached(it, false) } } catch (_: Exception) { Cached(json.decodeFromString(cached()["loans"] ?: "[]"), true) }
    suspend fun history(): Cached<List<HistoryItem>> = try { api.history().sortedByDescending { it.outAt }.also { withContext(io) { save("history", json.encodeToString(it)) } }.let { Cached(it, false) } } catch (_: Exception) { Cached(json.decodeFromString<List<HistoryItem>>(cached()["history"] ?: "[]").sortedByDescending { it.outAt }, true) }
    suspend fun member() = api.member()
    suspend fun renew(id: String) = api.renew(id)
    suspend fun setHistory(enabled: Boolean) = api.setHistory(enabled)
    suspend fun title(id: String) = api.title(id)
}
