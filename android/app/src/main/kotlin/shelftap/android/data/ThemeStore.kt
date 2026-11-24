package shelftap.android.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** The member's choice of light or dark, kept on this phone only. A display setting: no personal data, and not sent anywhere. */
class ThemeStore(private val store: DataStore<Preferences>) {
    val mode: Flow<ThemeMode> = store.data.map { prefs ->
        prefs[KEY]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: ThemeMode.SYSTEM
    }

    suspend fun set(mode: ThemeMode) {
        store.edit { it[KEY] = mode.name }
    }

    private companion object {
        val KEY = stringPreferencesKey("theme_mode")
    }
}
