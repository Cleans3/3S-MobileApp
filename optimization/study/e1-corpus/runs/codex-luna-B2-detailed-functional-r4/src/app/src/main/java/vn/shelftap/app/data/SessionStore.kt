package vn.shelftap.app.data

import android.content.Context
import java.util.UUID

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("shelftap_session", Context.MODE_PRIVATE)

    val deviceId: String
        get() = prefs.getString("deviceId", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("deviceId", it).apply()
        }

    @Synchronized fun accessToken(): String? = prefs.getString("accessToken", null)
    @Synchronized fun refreshToken(): String? = prefs.getString("refreshToken", null)
    @Synchronized fun mustChangePassword(): Boolean = prefs.getBoolean("mustChangePassword", false)

    @Synchronized fun saveAuth(auth: AuthResponse) {
        prefs.edit().putString("accessToken", auth.accessToken)
            .putString("refreshToken", auth.refreshToken)
            .putBoolean("mustChangePassword", auth.mustChangePassword).apply()
    }

    @Synchronized fun setMustChangePassword(value: Boolean) = prefs.edit().putBoolean("mustChangePassword", value).apply()
    @Synchronized fun clear() = prefs.edit().remove("accessToken").remove("refreshToken")
        .remove("mustChangePassword").apply()
}
