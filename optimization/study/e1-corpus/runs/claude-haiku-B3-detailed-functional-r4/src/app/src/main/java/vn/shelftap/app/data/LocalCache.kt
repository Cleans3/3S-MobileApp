package vn.shelftap.app.data

import android.content.SharedPreferences
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer

/** Last successfully loaded data, kept so Loans and History work offline. */
class LocalCache(private val prefs: SharedPreferences) {

    fun loans(): List<Loan>? = read(KEY_LOANS, ListSerializer(Loan.serializer()))

    fun saveLoans(loans: List<Loan>) = write(KEY_LOANS, ListSerializer(Loan.serializer()), loans)

    fun history(): List<HistoryItem>? = read(KEY_HISTORY, ListSerializer(HistoryItem.serializer()))

    fun saveHistory(items: List<HistoryItem>) =
        write(KEY_HISTORY, ListSerializer(HistoryItem.serializer()), items)

    fun me(): Me? = read(KEY_ME, Me.serializer())

    fun saveMe(me: Me) = write(KEY_ME, Me.serializer(), me)

    private fun <T> read(key: String, serializer: KSerializer<T>): T? {
        val raw = prefs.getString(key, null) ?: return null
        return runCatching { ShelfJson.decodeFromString(serializer, raw) }.getOrNull()
    }

    private fun <T> write(key: String, serializer: KSerializer<T>, value: T) {
        prefs.edit().putString(key, ShelfJson.encodeToString(serializer, value)).apply()
    }

    private companion object {
        const val KEY_LOANS = "loans"
        const val KEY_HISTORY = "history"
        const val KEY_ME = "me"
    }
}
