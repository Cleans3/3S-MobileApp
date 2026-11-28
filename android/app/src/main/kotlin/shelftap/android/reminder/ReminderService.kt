package shelftap.android.reminder

import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import shelftap.android.api.ApiException
import shelftap.android.api.Loan

/** The alarms of the phone, so the service can be tested with a fake. An id is the epoch day a reminder is for. */
interface ReminderAlarms {
    fun set(id: Int, reminder: DueReminder)
    fun cancel(id: Int)
}

/** The settings the service reads and the record it keeps of what it set. */
interface ReminderSettings {
    /** Whether the member asked for due-date reminders. */
    val enabled: Flow<Boolean>
    suspend fun scheduledIds(): Set<Int>
    suspend fun setScheduledIds(ids: Set<Int>)
}

/**
 * Sets and cancels the member's due-date reminders. They are worked out on the phone from the loans ([DueReminderPlanner]) and kept as
 * alarms of the phone: no server sends them, and nothing is stored except which days have an alarm. Alarms are dropped when the member
 * turns the setting off, when Android no longer allows notifications and when the session ends. After a restart of the phone they come
 * back the next time the app is opened and the loans load (the app has no copy of the loans to set them from sooner).
 */
class ReminderService(
    private val settings: ReminderSettings,
    private val alarms: ReminderAlarms,
    private val loans: suspend () -> List<Loan>,
    /** Whether Android lets the app post notifications right now (the permission, and the switch in the phone's settings). */
    private val canNotify: () -> Boolean,
    private val now: () -> Instant = Instant::now,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) {
    private val lock = Mutex()

    /** Plans from [current] loans and sets the alarms; cancels all when reminders are off or not allowed. */
    suspend fun update(current: List<Loan>) = lock.withLock {
        val old = settings.scheduledIds()
        if (!settings.enabled.first() || !canNotify()) {
            cancelLocked(old)
            return@withLock
        }
        val planned = DueReminderPlanner.plan(current, now(), zone()).associateBy { it.dueDay.toEpochDay().toInt() }
        (old - planned.keys).forEach(alarms::cancel)
        planned.forEach { (id, reminder) -> alarms.set(id, reminder) }
        settings.setScheduledIds(planned.keys)
    }

    /** Reads the loans from the server and sets the alarms from them; when they cannot be read, the alarms already set stay. */
    suspend fun refresh() {
        val current = try { loans() } catch (_: ApiException) { return }
        update(current)
    }

    suspend fun cancelAll() = lock.withLock { cancelLocked(settings.scheduledIds()) }

    private suspend fun cancelLocked(ids: Set<Int>) {
        ids.forEach(alarms::cancel)
        if (ids.isNotEmpty()) settings.setScheduledIds(emptySet())
    }

    /**
     * Refreshes while the session is [SessionCue.ACTIVE] and the setting is on; cancels every alarm when the setting goes off or the session
     * [SessionCue.ENDED]; does nothing while the session is only [SessionCue.PAUSED] (starting up, offline, locked, timed out): the alarms set
     * before stay, since it is still the same member's phone.
     */
    fun bind(scope: CoroutineScope, session: Flow<SessionCue>) {
        combine(session, settings.enabled) { cue, on -> if (!on) Action.CANCEL else when (cue) {
            SessionCue.ACTIVE -> Action.REFRESH
            SessionCue.ENDED -> Action.CANCEL
            SessionCue.PAUSED -> Action.NONE
        } }
            .distinctUntilChanged()
            .onEach { action -> when (action) { Action.REFRESH -> refresh(); Action.CANCEL -> cancelAll(); Action.NONE -> Unit } }
            .launchIn(scope)
    }

    private enum class Action { REFRESH, CANCEL, NONE }
}

/** What the session means for the reminders. */
enum class SessionCue { ACTIVE, ENDED, PAUSED }
