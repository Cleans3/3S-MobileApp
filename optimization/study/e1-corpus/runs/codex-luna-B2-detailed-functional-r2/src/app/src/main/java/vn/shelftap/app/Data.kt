package vn.shelftap.app

import android.content.Context
import android.provider.Settings
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.util.UUID

private val Context.store by preferencesDataStore("shelftap_session")
data class Session(val accessToken: String, val refreshToken: String, val mustChangePassword: Boolean)
data class Member(val memberId: String, val fullName: String, val email: String, val memberType: String, val status: String, val historyEnabled: Boolean, val mustChangePassword: Boolean)

class SessionStore(private val context: Context) {
    private val deviceKey = stringPreferencesKey("device_id")
    private val accessKey = stringPreferencesKey("access_token")
    private val refreshKey = stringPreferencesKey("refresh_token")
    private val changeKey = booleanPreferencesKey("must_change_password")
    suspend fun deviceId(): String { val p = context.store.data.first(); val found = p[deviceKey]; if (found != null) return found; val id = UUID.randomUUID().toString(); context.store.edit { it[deviceKey] = id }; return id }
    suspend fun session(): Session? = context.store.data.first().let { p -> val a=p[accessKey]; val r=p[refreshKey]; if (a != null && r != null) Session(a,r,p[changeKey] ?: false) else null }
    suspend fun save(s: Session) { context.store.edit { it[accessKey]=s.accessToken; it[refreshKey]=s.refreshToken; it[changeKey]=s.mustChangePassword } }
    suspend fun clear() { context.store.edit { it.remove(accessKey); it.remove(refreshKey); it.remove(changeKey) } }
    suspend fun setMustChange(value: Boolean) { context.store.edit { it[changeKey]=value } }
}
