package vn.shelftap.app.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import vn.shelftap.app.model.*
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

interface AuthRepository { suspend fun accessToken(): String }

class PreferenceAuthRepository(context: Context) : AuthRepository {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)
    override suspend fun accessToken(): String = prefs.getString("accessToken", "")?.takeIf { it.isNotBlank() }
        ?: throw IllegalStateException("No active session")
}

class CacheStore(context: Context) {
    private val prefs = context.getSharedPreferences("shelftap_cache", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().build()
    private val loansAdapter = moshi.adapter<List<Loan>>(Types.newParameterizedType(List::class.java, Loan::class.java))
    private val historyAdapter = moshi.adapter<List<HistoryItem>>(Types.newParameterizedType(List::class.java, HistoryItem::class.java))
    fun saveLoans(value: List<Loan>) = prefs.edit().putString("loans", loansAdapter.toJson(value)).apply()
    fun loadLoans(): List<Loan>? = prefs.getString("loans", null)?.let { loansAdapter.fromJson(it) }
    fun saveHistory(value: List<HistoryItem>) = prefs.edit().putString("history", historyAdapter.toJson(value)).apply()
    fun loadHistory(): List<HistoryItem>? = prefs.getString("history", null)?.let { historyAdapter.fromJson(it) }
}

class ShelfTapRepository(private val auth: AuthRepository, private val cache: CacheStore, private val api: ShelfTapApi) {
    private suspend fun bearer() = "Bearer ${auth.accessToken()}"
    suspend fun loans(): Pair<List<Loan>, Boolean> = withContext(Dispatchers.IO) { try { api.loans(bearer()).also(cache::saveLoans) to false } catch (_: Exception) { (cache.loadLoans() ?: emptyList()) to true } }
    suspend fun history(): Pair<List<HistoryItem>, Boolean> = withContext(Dispatchers.IO) { try { api.history(bearer()).also(cache::saveHistory) to false } catch (_: Exception) { (cache.loadHistory() ?: emptyList()) to true } }
    suspend fun renew(id: String) = withContext(Dispatchers.IO) { api.renew(bearer(), id).also { cache.loadLoans()?.map { loan -> if (loan.loanId == id) it else loan }?.let(cache::saveLoans) } }
    suspend fun member() = withContext(Dispatchers.IO) { api.member(bearer()) }
    suspend fun updatePrivacy(enabled: Boolean) = withContext(Dispatchers.IO) { api.updatePrivacy(bearer(), PrivacyRequest(enabled)) }
    suspend fun title(id: String) = withContext(Dispatchers.IO) { api.title(bearer(), id) }
}

object AppGraph {
    fun repository(context: Context): ShelfTapRepository {
        val moshi = Moshi.Builder().build()
        val api = Retrofit.Builder().baseUrl("https://api.shelftap.example/").addConverterFactory(MoshiConverterFactory.create(moshi)).build().create(ShelfTapApi::class.java)
        return ShelfTapRepository(PreferenceAuthRepository(context), CacheStore(context), api)
    }
}
