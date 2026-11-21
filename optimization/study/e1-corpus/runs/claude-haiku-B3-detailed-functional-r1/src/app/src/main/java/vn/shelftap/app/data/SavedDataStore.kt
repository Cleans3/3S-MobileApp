package vn.shelftap.app.data

import android.content.Context
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/** Keeps the last successfully loaded API data on the device, as JSON in SharedPreferences. */
class SavedDataStore(context: Context, private val json: Json) {
    private val prefs = context.getSharedPreferences("saved_data", Context.MODE_PRIVATE)

    fun <T> save(key: String, serializer: KSerializer<T>, value: T) {
        prefs.edit().putString(key, json.encodeToString(serializer, value)).apply()
    }

    fun <T> load(key: String, serializer: KSerializer<T>): T? {
        val raw = prefs.getString(key, null) ?: return null
        return runCatching { json.decodeFromString(serializer, raw) }.getOrNull()
    }
}
