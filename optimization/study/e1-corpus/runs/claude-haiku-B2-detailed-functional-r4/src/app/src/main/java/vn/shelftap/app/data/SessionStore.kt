package vn.shelftap.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

/**
 * Keeps the device ID (plain prefs, created once per installation) and the
 * session tokens (encrypted prefs, cleared on sign-out or session end).
 */
class SessionStore(context: Context) {

    private val installPrefs: SharedPreferences =
        context.getSharedPreferences(INSTALL_FILE, Context.MODE_PRIVATE)

    private val sessionPrefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            SESSION_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    /** Generated on first launch and kept for the life of the installation. */
    val deviceId: String = installPrefs.getString(KEY_DEVICE_ID, null)
        ?: UUID.randomUUID().toString().also {
            installPrefs.edit().putString(KEY_DEVICE_ID, it).commit()
        }

    val accessToken: String?
        get() = sessionPrefs.getString(KEY_ACCESS_TOKEN, null)

    val refreshToken: String?
        get() = sessionPrefs.getString(KEY_REFRESH_TOKEN, null)

    /**
     * Commits synchronously. Refresh tokens are single use, so the rotated token
     * must be on disk before anything else tries to use it.
     */
    fun save(accessToken: String, refreshToken: String) {
        sessionPrefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .commit()
    }

    fun clear() {
        sessionPrefs.edit().clear().commit()
    }

    private companion object {
        const val INSTALL_FILE = "install"
        const val SESSION_FILE = "session"
        const val KEY_DEVICE_ID = "deviceId"
        const val KEY_ACCESS_TOKEN = "accessToken"
        const val KEY_REFRESH_TOKEN = "refreshToken"
    }
}
