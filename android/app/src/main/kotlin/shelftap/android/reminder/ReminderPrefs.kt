package shelftap.android.reminder

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * The member's notification settings, kept on this phone only (settings, not personal data; nothing here is sent anywhere).
 * [dueReminder] is off until the member turns it on (and Android has allowed notifications); [hideTitles] is on by default, because what
 * a member borrows is sensitive (Decree 13/2023) and a lock screen can be read by anyone nearby.
 */
class ReminderPrefs(private val store: DataStore<Preferences>) : ReminderSettings {
    val dueReminder: Flow<Boolean> = store.data.map { it[DUE_REMINDER] ?: false }
    override val enabled: Flow<Boolean> get() = dueReminder
    val hideTitles: Flow<Boolean> = store.data.map { it[HIDE_TITLES] ?: true }

    suspend fun setDueReminder(on: Boolean) {
        store.edit { it[DUE_REMINDER] = on }
    }

    suspend fun setHideTitles(on: Boolean) {
        store.edit { it[HIDE_TITLES] = on }
    }

    /** The ids of the alarms set last (the epoch day each one is for), so the next refresh can cancel exactly those. */
    override suspend fun scheduledIds(): Set<Int> = store.data.first()[SCHEDULED]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()

    override suspend fun setScheduledIds(ids: Set<Int>) {
        store.edit { it[SCHEDULED] = ids.map { id -> id.toString() }.toSet() }
    }

    private companion object {
        val DUE_REMINDER = booleanPreferencesKey("notify_due_reminder")
        val HIDE_TITLES = booleanPreferencesKey("notify_hide_titles")
        val SCHEDULED = stringSetPreferencesKey("notify_scheduled_days")
    }
}
