package shelftap.android.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.util.UUID
import kotlinx.coroutines.flow.first
import shelftap.android.session.DeviceIdProvider

/** One random UUID per install, generated on first use. Survives logout (it identifies the install, not the member). */
class DeviceIdStore(private val store: DataStore<Preferences>) : DeviceIdProvider {
    override suspend fun get(): String {
        store.data.first()[KEY]?.let { return it }
        var id = ""
        store.edit { prefs -> id = prefs[KEY] ?: UUID.randomUUID().toString().also { prefs[KEY] = it } }
        return id
    }

    private companion object {
        val KEY = stringPreferencesKey("device_id")
    }
}
