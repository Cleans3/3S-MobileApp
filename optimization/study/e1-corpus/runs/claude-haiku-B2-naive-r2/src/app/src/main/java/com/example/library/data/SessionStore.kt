package com.example.library.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Keeps the signed-in session token in encrypted storage so the member
 * stays signed in across app launches until they sign out.
 */
class SessionStore(context: Context) {

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) {
            prefs.edit {
                if (value == null) remove(KEY_TOKEN) else putString(KEY_TOKEN, value)
            }
        }

    fun clear() {
        prefs.edit { remove(KEY_TOKEN) }
    }

    private companion object {
        const val FILE_NAME = "library_session"
        const val KEY_TOKEN = "access_token"
    }
}
