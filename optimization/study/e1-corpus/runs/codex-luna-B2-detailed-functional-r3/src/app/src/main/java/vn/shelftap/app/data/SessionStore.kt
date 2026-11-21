package vn.shelftap.app.data

import android.content.Context
import android.provider.Settings
import java.util.UUID

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("shelftap_session", Context.MODE_PRIVATE)
    val deviceId: String
        get() = prefs.getString("deviceId", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("deviceId", it).apply() }
    fun session(): Session? {
        val access = prefs.getString("accessToken", null) ?: return null
        val refresh = prefs.getString("refreshToken", null) ?: return null
        return Session(access, refresh, prefs.getBoolean("mustChangePassword", false))
    }
    fun save(session: Session) = prefs.edit().putString("accessToken", session.accessToken).putString("refreshToken", session.refreshToken).putBoolean("mustChangePassword", session.mustChangePassword).apply()
    fun clear() = prefs.edit().remove("accessToken").remove("refreshToken").remove("mustChangePassword").apply()
}
