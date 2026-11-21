package vn.shelftap.slop

import android.content.Context
import android.util.Log

/** Keeps the member session token between launches. */
class TokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    fun save(token: String) {
        prefs.edit().putString("token", token).apply()
    }

    fun load(): String? = prefs.getString("token", null)

    fun clear() {
        if (BuildConfig.DEBUG) Log.d("TokenStore", "session cleared")
        prefs.edit().remove("token").apply()
    }
}
