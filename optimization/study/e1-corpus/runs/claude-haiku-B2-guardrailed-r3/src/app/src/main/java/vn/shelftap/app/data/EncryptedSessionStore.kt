package vn.shelftap.app.data

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import vn.shelftap.app.security.KeystoreAesCipher
import java.io.File
import java.security.GeneralSecurityException

/** Session state kept on the phone. [deviceId] outlives sign-outs. */
data class StoredSession(
    val deviceId: String,
    val accessToken: String? = null,
    val refreshToken: String? = null,
)

/**
 * Stores the session as one AES-256-GCM blob in app-private storage.
 * Only ciphertext touches the disk; the key stays in the Android Keystore.
 */
class EncryptedSessionStore(
    context: Context,
    private val cipher: KeystoreAesCipher,
    private val io: CoroutineDispatcher,
) {
    private val file = File(context.filesDir, FILE_NAME)

    /** Returns null when nothing is stored or the stored blob cannot be decrypted. */
    suspend fun read(): StoredSession? = withContext(io) {
        if (!file.exists()) return@withContext null
        try {
            val json = JSONObject(String(cipher.decrypt(file.readBytes()), Charsets.UTF_8))
            StoredSession(
                deviceId = json.getString("deviceId"),
                accessToken = json.optString("accessToken").ifEmpty { null },
                refreshToken = json.optString("refreshToken").ifEmpty { null },
            )
        } catch (e: GeneralSecurityException) {
            // The key was lost or the blob is corrupt. Discard it.
            file.delete()
            null
        } catch (e: JSONException) {
            file.delete()
            null
        } catch (e: IllegalArgumentException) {
            file.delete()
            null
        }
    }

    suspend fun write(session: StoredSession): Unit = withContext(io) {
        val json = JSONObject().put("deviceId", session.deviceId)
        session.accessToken?.let { json.put("accessToken", it) }
        session.refreshToken?.let { json.put("refreshToken", it) }
        val blob = cipher.encrypt(json.toString().toByteArray(Charsets.UTF_8))

        // Write to a temp file, then rename, so a crash never leaves a half-written session.
        val tmp = File(file.parentFile, "$FILE_NAME.tmp")
        tmp.writeBytes(blob)
        check(tmp.renameTo(file)) { "Could not save session" }
    }

    private companion object {
        const val FILE_NAME = "session.enc"
    }
}
