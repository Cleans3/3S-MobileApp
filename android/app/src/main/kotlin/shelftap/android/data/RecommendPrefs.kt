package shelftap.android.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The member's switch "Suggestions from my reading history", kept on this phone only (a setting, not personal data, and not sent anywhere).
 * On by default; the server-side switch that saves the history at all is separate and stays in [Repository.setHistoryEnabled].
 */
class RecommendPrefs(private val store: DataStore<Preferences>) {
    val useHistory: Flow<Boolean> = store.data.map { it[USE_HISTORY] ?: true }

    suspend fun setUseHistory(on: Boolean) {
        store.edit { it[USE_HISTORY] = on }
    }

    private companion object {
        val USE_HISTORY = booleanPreferencesKey("recommend_use_history")
    }
}
