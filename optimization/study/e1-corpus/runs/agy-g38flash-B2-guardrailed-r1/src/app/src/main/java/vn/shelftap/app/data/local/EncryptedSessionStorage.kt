package vn.shelftap.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import vn.shelftap.app.data.crypto.KeystoreEncryptionManager
import vn.shelftap.app.data.models.MemberProfileResponse
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "shelftap_secure_storage")

class EncryptedSessionStorage(
    private val context: Context,
    private val encryptionManager: KeystoreEncryptionManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val cachedAccessToken = AtomicReference<String?>(null)
    private val cachedRefreshToken = AtomicReference<String?>(null)
    private val cachedDeviceId = AtomicReference<String?>(null)

    companion object {
        private val KEY_ENC_DEVICE_ID = stringPreferencesKey("enc_device_id")
        private val KEY_ENC_ACCESS_TOKEN = stringPreferencesKey("enc_access_token")
        private val KEY_ENC_REFRESH_TOKEN = stringPreferencesKey("enc_refresh_token")
        private val KEY_ENC_MUST_CHANGE_PASSWORD = stringPreferencesKey("enc_must_change_pwd")

        // Personal profile data stored encrypted
        private val KEY_ENC_MEMBER_ID = stringPreferencesKey("enc_member_id")
        private val KEY_ENC_FULL_NAME = stringPreferencesKey("enc_full_name")
        private val KEY_ENC_EMAIL = stringPreferencesKey("enc_email")
        private val KEY_ENC_MEMBER_TYPE = stringPreferencesKey("enc_member_type")
        private val KEY_ENC_STATUS = stringPreferencesKey("enc_status")
        private val KEY_ENC_HISTORY_ENABLED = stringPreferencesKey("enc_history_enabled")
    }

    suspend fun getOrCreateDeviceId(): String = withContext(ioDispatcher) {
        val cached = cachedDeviceId.get()
        if (!cached.isNullOrBlank()) return@withContext cached

        val existing = getDecryptedValue(KEY_ENC_DEVICE_ID)
        if (!existing.isNullOrBlank()) {
            cachedDeviceId.set(existing)
            existing
        } else {
            val newDeviceId = UUID.randomUUID().toString()
            saveEncryptedValue(KEY_ENC_DEVICE_ID, newDeviceId)
            cachedDeviceId.set(newDeviceId)
            newDeviceId
        }
    }

    suspend fun saveAuthTokens(
        accessToken: String,
        refreshToken: String,
        mustChangePassword: Boolean
    ) = withContext(ioDispatcher) {
        cachedAccessToken.set(accessToken)
        cachedRefreshToken.set(refreshToken)
        saveEncryptedValue(KEY_ENC_ACCESS_TOKEN, accessToken)
        saveEncryptedValue(KEY_ENC_REFRESH_TOKEN, refreshToken)
        saveEncryptedValue(KEY_ENC_MUST_CHANGE_PASSWORD, mustChangePassword.toString())
    }

    suspend fun getAccessToken(): String? = withContext(ioDispatcher) {
        cachedAccessToken.get() ?: run {
            val token = getDecryptedValue(KEY_ENC_ACCESS_TOKEN)
            cachedAccessToken.set(token)
            token
        }
    }

    fun getAccessTokenSync(): String? {
        return cachedAccessToken.get()
    }

    suspend fun getRefreshToken(): String? = withContext(ioDispatcher) {
        cachedRefreshToken.get() ?: run {
            val token = getDecryptedValue(KEY_ENC_REFRESH_TOKEN)
            cachedRefreshToken.set(token)
            token
        }
    }

    fun getRefreshTokenSync(): String? {
        return cachedRefreshToken.get()
    }

    suspend fun mustChangePassword(): Boolean = withContext(ioDispatcher) {
        getDecryptedValue(KEY_ENC_MUST_CHANGE_PASSWORD)?.toBooleanStrictOrNull() ?: false
    }

    suspend fun setMustChangePassword(mustChange: Boolean) = withContext(ioDispatcher) {
        saveEncryptedValue(KEY_ENC_MUST_CHANGE_PASSWORD, mustChange.toString())
    }

    suspend fun saveProfile(profile: MemberProfileResponse) = withContext(ioDispatcher) {
        saveEncryptedValue(KEY_ENC_MEMBER_ID, profile.memberId)
        saveEncryptedValue(KEY_ENC_FULL_NAME, profile.fullName)
        saveEncryptedValue(KEY_ENC_EMAIL, profile.email)
        saveEncryptedValue(KEY_ENC_MEMBER_TYPE, profile.memberType)
        saveEncryptedValue(KEY_ENC_STATUS, profile.status)
        saveEncryptedValue(KEY_ENC_HISTORY_ENABLED, profile.historyEnabled.toString())
        saveEncryptedValue(KEY_ENC_MUST_CHANGE_PASSWORD, profile.mustChangePassword.toString())
    }

    suspend fun getCachedProfile(): MemberProfileResponse? = withContext(ioDispatcher) {
        val memberId = getDecryptedValue(KEY_ENC_MEMBER_ID) ?: return@withContext null
        val fullName = getDecryptedValue(KEY_ENC_FULL_NAME) ?: ""
        val email = getDecryptedValue(KEY_ENC_EMAIL) ?: ""
        val memberType = getDecryptedValue(KEY_ENC_MEMBER_TYPE) ?: ""
        val status = getDecryptedValue(KEY_ENC_STATUS) ?: ""
        val historyEnabled = getDecryptedValue(KEY_ENC_HISTORY_ENABLED)?.toBooleanStrictOrNull() ?: false
        val mustChangePassword = getDecryptedValue(KEY_ENC_MUST_CHANGE_PASSWORD)?.toBooleanStrictOrNull() ?: false

        MemberProfileResponse(
            memberId = memberId,
            fullName = fullName,
            email = email,
            memberType = memberType,
            status = status,
            historyEnabled = historyEnabled,
            mustChangePassword = mustChangePassword
        )
    }

    suspend fun clearSession() = withContext(ioDispatcher) {
        cachedAccessToken.set(null)
        cachedRefreshToken.set(null)
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_ENC_ACCESS_TOKEN)
            preferences.remove(KEY_ENC_REFRESH_TOKEN)
            preferences.remove(KEY_ENC_MUST_CHANGE_PASSWORD)
            preferences.remove(KEY_ENC_MEMBER_ID)
            preferences.remove(KEY_ENC_FULL_NAME)
            preferences.remove(KEY_ENC_EMAIL)
            preferences.remove(KEY_ENC_MEMBER_TYPE)
            preferences.remove(KEY_ENC_STATUS)
            preferences.remove(KEY_ENC_HISTORY_ENABLED)
        }
    }

    private suspend fun saveEncryptedValue(key: Preferences.Key<String>, plainText: String) {
        val encrypted = encryptionManager.encrypt(plainText)
        context.dataStore.edit { preferences ->
            preferences[key] = encrypted
        }
    }

    private suspend fun getDecryptedValue(key: Preferences.Key<String>): String? {
        val encrypted = context.dataStore.data.map { it[key] }.firstOrNull() ?: return null
        return try {
            encryptionManager.decrypt(encrypted)
        } catch (_: Exception) {
            null
        }
    }
}
