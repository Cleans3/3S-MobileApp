package vn.shelftap.app.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import java.io.IOException

/** A loaded value. [Saved] means the network was unavailable and the last saved copy was used. */
sealed interface Loaded<out T> {
    val data: T

    data class Fresh<out T>(override val data: T) : Loaded<T>
    data class Saved<out T>(override val data: T) : Loaded<T>
}

class ShelfTapRepository(
    private val api: ShelfTapApi,
    private val store: SavedDataStore,
    private val accessToken: suspend () -> String,
) {
    suspend fun loadLoans(): Loaded<List<Loan>> =
        loadWithSaved(KEY_LOANS, ListSerializer(Loan.serializer())) { api.getLoans(it) }

    suspend fun loadHistory(): Loaded<List<HistoryItem>> =
        loadWithSaved(KEY_HISTORY, ListSerializer(HistoryItem.serializer())) { api.getHistory(it) }

    suspend fun loadMember(): Loaded<Member> =
        loadWithSaved(KEY_MEMBER, Member.serializer()) { api.getMember(it) }

    /** Renews a loan and patches the saved loan list so the change survives offline reloads. */
    suspend fun renewLoan(loanId: String): Loan {
        val updated = api.renewLoan(accessToken(), loanId)
        val loansSerializer = ListSerializer(Loan.serializer())
        store.read(KEY_LOANS, loansSerializer)?.let { saved ->
            store.write(
                KEY_LOANS,
                loansSerializer,
                saved.map { if (it.loanId == loanId) updated else it },
            )
        }
        return updated
    }

    suspend fun setHistoryEnabled(enabled: Boolean): Member {
        val member = api.setHistoryEnabled(accessToken(), enabled)
        store.write(KEY_MEMBER, Member.serializer(), member)
        return member
    }

    suspend fun loadTitle(titleId: String): TitleDetail = api.getTitle(accessToken(), titleId)

    private suspend fun <T> loadWithSaved(
        key: String,
        serializer: KSerializer<T>,
        fetch: suspend (token: String) -> T,
    ): Loaded<T> = try {
        val fresh = fetch(accessToken())
        store.write(key, serializer, fresh)
        Loaded.Fresh(fresh)
    } catch (e: IOException) {
        val saved = store.read(key, serializer) ?: throw e
        Loaded.Saved(saved)
    }

    private companion object {
        const val KEY_LOANS = "loans"
        const val KEY_HISTORY = "history"
        const val KEY_MEMBER = "member"
    }
}
