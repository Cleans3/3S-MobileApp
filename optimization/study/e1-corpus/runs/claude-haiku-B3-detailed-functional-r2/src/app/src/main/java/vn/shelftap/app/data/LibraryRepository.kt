package vn.shelftap.app.data

import java.io.IOException
import java.time.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import vn.shelftap.app.data.ApiClient

/** Data plus a flag telling the UI whether it came from the on-device copy. */
class Snapshot<T>(
    val data: T,
    val fromSavedData: Boolean,
)

class LibraryRepository(
    private val api: ApiClient,
    private val saved: SavedData,
) {
    private val json: Json = api.json
    private val loansSerializer = ListSerializer(Loan.serializer())
    private val historySerializer = ListSerializer(HistoryItem.serializer())

    /** Current loans. Falls back to the last saved list when the network is unavailable. */
    suspend fun loans(): Snapshot<List<Loan>> =
        withSavedData(LOANS_KEY, loansSerializer) {
            api.getJson<List<Loan>>("/api/me/loans")
        }

    /** Past loans, newest first. Falls back to the last saved list when offline. */
    suspend fun history(): Snapshot<List<HistoryItem>> =
        withSavedData(HISTORY_KEY, historySerializer) {
            api.getJson<List<HistoryItem>>("/api/me/history")
                .sortedWith(compareByDescending<HistoryItem> { parseInstant(it.returnedAt ?: it.outAt) ?: Instant.MIN })
        }

    /** Renews a loan. Throws ApiException (code RENEWAL_NOT_ALLOWED) when not allowed. */
    suspend fun renew(loanId: String): Loan {
        val updated = api.postJson<Loan>("/api/loans/$loanId/renew")
        saved.load(LOANS_KEY, loansSerializer)?.let { cached ->
            saved.save(
                LOANS_KEY,
                loansSerializer,
                cached.map { if (it.loanId == updated.loanId) updated else it },
            )
        }
        return updated
    }

    suspend fun me(): Snapshot<Member> =
        withSavedData(ME_KEY, Member.serializer()) {
            api.getJson<Member>("/api/me")
        }

    suspend fun setHistoryEnabled(enabled: Boolean): Member {
        val updated = api.putJson<Member>(
            "/api/me/privacy",
            json.encodeToString(PrivacyUpdate.serializer(), PrivacyUpdate(enabled)),
        )
        saved.save(ME_KEY, Member.serializer(), updated)
        return updated
    }

    /** Book details. Not cached. */
    suspend fun title(titleId: String): TitleDetail =
        api.getJson<TitleDetail>("/api/titles/$titleId")

    private suspend fun <T> withSavedData(
        key: String,
        serializer: KSerializer<T>,
        fetch: suspend () -> T,
    ): Snapshot<T> {
        val fresh = try {
            fetch()
        } catch (e: IOException) {
            val cached = saved.load(key, serializer) ?: throw e
            return Snapshot(cached, fromSavedData = true)
        }
        saved.save(key, serializer, fresh)
        return Snapshot(fresh, fromSavedData = false)
    }

    private companion object {
        const val LOANS_KEY = "loans"
        const val HISTORY_KEY = "history"
        const val ME_KEY = "me"
    }
}
