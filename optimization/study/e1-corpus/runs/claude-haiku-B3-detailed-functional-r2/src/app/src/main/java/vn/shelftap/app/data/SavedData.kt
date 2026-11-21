package vn.shelftap.app.data

import java.io.File
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Keeps the last successfully loaded responses on disk so Loans and History
 * can still be shown when the network is unavailable.
 */
class SavedData(directory: File, private val json: Json) {

    private val dir = File(directory, "saved-data").apply { mkdirs() }

    fun <T> save(key: String, serializer: KSerializer<T>, value: T) {
        runCatching { fileFor(key).writeText(json.encodeToString(serializer, value)) }
    }

    fun <T> load(key: String, serializer: KSerializer<T>): T? =
        runCatching {
            fileFor(key)
                .takeIf { it.exists() }
                ?.readText()
                ?.let { json.decodeFromString(serializer, it) }
        }.getOrNull()

    private fun fileFor(key: String): File = File(dir, "$key.json")
}
