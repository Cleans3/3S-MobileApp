package vn.shelftap.app.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import vn.shelftap.app.data.security.TokenCipher
import java.security.GeneralSecurityException
import java.util.Base64

private val Context.secureStateDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "secure_state",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

@Serializable
internal data class StoredSession(
    val accessToken: String,
    val refreshToken: String,
    val mustChangePassword: Boolean,
)

@Serializable
internal data class StoredState(
    val deviceId: String,
    val session: StoredSession? = null,
)

/**
 * Keeps [StoredState] in app-private DataStore. The state is serialized to JSON and encrypted
 * with the Keystore key before it is written, so only ciphertext reaches the disk.
 */
internal class SecureStateStore(
    context: Context,
    private val cipher: TokenCipher,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val dataStore: DataStore<Preferences> = context.applicationContext.secureStateDataStore
    private val stateKey = stringPreferencesKey("state")

    /** Decrypted state, or null when nothing readable is stored. */
    val states: Flow<StoredState?> = dataStore.data
        .map { prefs -> prefs[stateKey]?.let { decode(it) } }
        .flowOn(ioDispatcher)

    suspend fun read(): StoredState? = states.first()

    suspend fun write(state: StoredState) {
        withContext(ioDispatcher) {
            val plain = json.encodeToString(state).toByteArray(Charsets.UTF_8)
            val encoded = Base64.getEncoder().encodeToString(cipher.encrypt(plain))
            dataStore.edit { prefs -> prefs[stateKey] = encoded }
        }
    }

    private fun decode(encoded: String): StoredState? = try {
        val plain = cipher.decrypt(Base64.getDecoder().decode(encoded))
        json.decodeFromString<StoredState>(String(plain, Charsets.UTF_8))
    } catch (e: GeneralSecurityException) {
        // Key lost or data damaged: treat as signed out. A new device ID is created on demand.
        null
    } catch (e: IllegalArgumentException) {
        // Bad Base64 or JSON (SerializationException is a subclass).
        null
    }
}
