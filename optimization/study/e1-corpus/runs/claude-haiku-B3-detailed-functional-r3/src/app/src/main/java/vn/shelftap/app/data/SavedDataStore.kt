package vn.shelftap.app.data

import android.content.Context
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/** Keeps the last successfully loaded data so Loans and History work offline. */
class SavedDataStore(context: Context, private val json: Json) {
    private val prefs = context.getSharedPreferences("shelftap_saved_data", Context.MODE_PRIVATE)

    fun <T> write(key: String, serializer: KSerializer<T>, value: T) {
        prefs.edit().putString(key, json.encodeToString(serializer, value)).apply()
    }

    fun <T> read(key: String, serializer: KSerializer<T>): T? {
        val text = prefs.getString(key, null) ?: return null
        return runCatching { json.decodeFromString(serializer, text) }.getOrNull()
    }
}
