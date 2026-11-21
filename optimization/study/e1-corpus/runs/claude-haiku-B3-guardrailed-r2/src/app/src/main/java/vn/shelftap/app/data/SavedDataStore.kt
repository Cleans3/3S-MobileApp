package vn.shelftap.app.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import vn.shelftap.app.model.HistoryItem
import vn.shelftap.app.model.Loan
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.GeneralSecurityException

/** Last data loaded from the server. Null fields have never been loaded. */
data class SavedData(
    val loans: List<Loan>? = null,
    val history: List<HistoryItem>? = null,
    val historyEnabled: Boolean? = null,
)

/**
 * Keeps the last loaded loans and history in app-private storage. The file holds
 * only ciphertext produced by [KeystoreCipher]; plaintext never touches disk.
 */
class SavedDataStore(
    private val file: File,
    private val cipher: KeystoreCipher,
    private val io: CoroutineDispatcher,
) {
    private val mutex = Mutex()

    suspend fun read(): SavedData = withContext(io) {
        mutex.withLock { readLocked() }
    }

    suspend fun update(change: (SavedData) -> SavedData) {
        withContext(io) {
            mutex.withLock {
                writeLocked(change(readLocked()))
            }
        }
    }

    private fun readLocked(): SavedData {
        if (!file.exists()) return SavedData()
        return try {
            val json = JSONObject(String(cipher.decrypt(file.readBytes()), Charsets.UTF_8))
            SavedData(
                loans = json.optJSONArray("loans")?.let { array ->
                    List(array.length()) { parseLoan(array.getJSONObject(it)) }
                },
                history = json.optJSONArray("history")?.let { array ->
                    List(array.length()) { parseHistoryItem(array.getJSONObject(it)) }
                },
                historyEnabled = if (json.isNull("historyEnabled")) null else json.getBoolean("historyEnabled"),
            )
        } catch (e: GeneralSecurityException) {
            SavedData()
        } catch (e: JSONException) {
            SavedData()
        } catch (e: IOException) {
            SavedData()
        } catch (e: IllegalArgumentException) {
            SavedData()
        }
    }

    private fun writeLocked(data: SavedData) {
        val json = JSONObject()
        data.loans?.let { loans -> json.put("loans", JSONArray(loans.map { loanToJson(it) })) }
        data.history?.let { items -> json.put("history", JSONArray(items.map { historyToJson(it) })) }
        data.historyEnabled?.let { json.put("historyEnabled", it) }

        val encrypted = cipher.encrypt(json.toString().toByteArray(Charsets.UTF_8))
        val temp = File(file.parentFile, file.name + ".tmp")
        temp.writeBytes(encrypted)
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE)
    }
}
