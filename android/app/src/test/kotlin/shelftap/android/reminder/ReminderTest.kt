package shelftap.android.reminder

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import shelftap.android.api.ApiException
import shelftap.android.api.ErrorCode
import shelftap.android.api.Loan

/** When a reminder is planned, what it says, and when the service sets and cancels the phone's alarms. */
class ReminderTest {
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")

    /** 2026-12-05 10:00 local: a Saturday, after the 07:30 of that day. */
    private val now = Instant.parse("2026-12-05T03:00:00Z")

    private fun loan(id: String, dueAt: String, returnedAt: String? = null, title: String = "Book $id") =
        Loan("l$id", "c$id", id, title, "bc$id", "2026-11-21T00:00:00Z", dueAt, returnedAt, 0, true)

    /** Local midnight-plus: the due moment of the server is an instant; 10:00 local on the day is `03:00Z`. */
    private fun due(day: String) = "${day}T03:00:00Z"

    // ---- planner

    @Test fun `a loan due the day after tomorrow gets a reminder tomorrow at 7 30`() {
        val plan = DueReminderPlanner.plan(listOf(loan("1", due("2026-12-07"))), now, zone)
        assertEquals(1, plan.size)
        // 2026-12-06 07:30 local (UTC+7) is 2026-12-06T00:30:00Z
        assertEquals(Instant.parse("2026-12-06T00:30:00Z"), plan.single().triggerAt)
        assertEquals(LocalDate.of(2026, 12, 7), plan.single().dueDay)
    }

    @Test fun `a loan due tomorrow whose reminder time has passed gets none`() {
        // Due 2026-12-06: the reminder was for today 07:30, already gone.
        assertTrue(DueReminderPlanner.plan(listOf(loan("1", due("2026-12-06"))), now, zone).isEmpty())
    }

    @Test fun `an overdue loan gets none`() {
        assertTrue(DueReminderPlanner.plan(listOf(loan("1", due("2026-12-01"))), now, zone).isEmpty())
    }

    @Test fun `a returned loan gets none`() {
        assertTrue(DueReminderPlanner.plan(listOf(loan("1", due("2026-12-10"), returnedAt = "2026-12-04T00:00:00Z")), now, zone).isEmpty())
    }

    @Test fun `loans due the same day share one reminder and it names no book`() {
        val plan = DueReminderPlanner.plan(listOf(loan("1", due("2026-12-10")), loan("2", due("2026-12-10"))), now, zone)
        assertEquals(1, plan.size)
        assertEquals(2, plan.single().count)
        assertNull(plan.single().title)
    }

    @Test fun `a single loan carries its title`() {
        assertEquals("Số đỏ", DueReminderPlanner.plan(listOf(loan("1", due("2026-12-10"), title = "Số đỏ")), now, zone).single().title)
    }

    @Test fun `reminders come in time order and the day follows the phone's zone`() {
        // 2026-12-09T20:00Z is already the 10th in Vietnam (UTC+7).
        val plan = DueReminderPlanner.plan(listOf(loan("1", "2026-12-12T03:00:00Z"), loan("2", "2026-12-09T20:00:00Z")), now, zone)
        assertEquals(listOf(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12)), plan.map { it.dueDay })
    }

    @Test fun `a timestamp that cannot be read is skipped`() {
        assertTrue(DueReminderPlanner.plan(listOf(loan("1", "tomorrow")), now, zone).isEmpty())
    }

    @Test fun `no more than the guard number of reminders`() {
        val many = (1..40).map { loan("$it", due("2026-12-${(it % 20) + 10}"), title = "t$it") } +
            (1..40).map { loan("x$it", Instant.parse("2027-01-01T03:00:00Z").plusSeconds(86_400L * it).toString()) }
        assertTrue(DueReminderPlanner.plan(many, now, zone).size <= DueReminderPlanner.MAX_REMINDERS)
    }

    // ---- service

    private class FakeAlarms : ReminderAlarms {
        val set = LinkedHashMap<Int, DueReminder>()
        val cancelled = ArrayList<Int>()
        override fun set(id: Int, reminder: DueReminder) { set[id] = reminder }
        override fun cancel(id: Int) { set.remove(id); cancelled += id }
    }

    private class FakeSettings(on: Boolean = true) : ReminderSettings {
        val on = MutableStateFlow(on)
        var ids: Set<Int> = emptySet()
        override val enabled get() = on
        override suspend fun scheduledIds() = ids
        override suspend fun setScheduledIds(ids: Set<Int>) { this.ids = ids }
    }

    private fun service(settings: FakeSettings, alarms: FakeAlarms, loans: suspend () -> List<Loan> = { emptyList() }, canNotify: () -> Boolean = { true }) =
        ReminderService(settings, alarms, loans, canNotify, now = { now }, zone = { zone })

    @Test fun `update sets one alarm per due day and remembers which`() = runBlocking {
        val settings = FakeSettings(); val alarms = FakeAlarms()
        service(settings, alarms).update(listOf(loan("1", due("2026-12-10")), loan("2", due("2026-12-12"))))
        assertEquals(setOf(LocalDate.of(2026, 12, 10).toEpochDay().toInt(), LocalDate.of(2026, 12, 12).toEpochDay().toInt()), alarms.set.keys)
        assertEquals(alarms.set.keys, settings.ids)
    }

    @Test fun `a later update cancels the alarm of a loan that went away`() = runBlocking {
        val settings = FakeSettings(); val alarms = FakeAlarms(); val s = service(settings, alarms)
        s.update(listOf(loan("1", due("2026-12-10")), loan("2", due("2026-12-12"))))
        s.update(listOf(loan("2", due("2026-12-12"))))
        assertEquals(listOf(LocalDate.of(2026, 12, 12).toEpochDay().toInt()), alarms.set.keys.toList())
        assertEquals(listOf(LocalDate.of(2026, 12, 10).toEpochDay().toInt()), alarms.cancelled)
    }

    @Test fun `nothing is set while the setting is off, and what was set is cancelled`() = runBlocking {
        val settings = FakeSettings(); val alarms = FakeAlarms(); val s = service(settings, alarms)
        s.update(listOf(loan("1", due("2026-12-10"))))
        settings.on.value = false
        s.update(listOf(loan("1", due("2026-12-10"))))
        assertTrue(alarms.set.isEmpty())
        assertTrue(settings.ids.isEmpty())
    }

    @Test fun `nothing is set when Android does not allow notifications`() = runBlocking {
        val settings = FakeSettings(); val alarms = FakeAlarms()
        service(settings, alarms, canNotify = { false }).update(listOf(loan("1", due("2026-12-10"))))
        assertTrue(alarms.set.isEmpty())
    }

    @Test fun `refresh keeps the alarms when the loans cannot be read`() = runBlocking {
        val settings = FakeSettings(); val alarms = FakeAlarms()
        service(settings, alarms).update(listOf(loan("1", due("2026-12-10"))))
        service(settings, alarms, loans = { throw ApiException.Http(503, ErrorCode.NOT_FOUND, null) }).refresh()
        assertEquals(1, alarms.set.size)
    }

    @Test fun `cancelAll cancels every alarm that was set`() = runBlocking {
        val settings = FakeSettings(); val alarms = FakeAlarms(); val s = service(settings, alarms)
        s.update(listOf(loan("1", due("2026-12-10")), loan("2", due("2026-12-12"))))
        s.cancelAll()
        assertTrue(alarms.set.isEmpty())
        assertTrue(settings.ids.isEmpty())
    }

    @Test fun `bind sets alarms while active and cancels when the session ends but not when it is only paused`() = runBlocking {
        val settings = FakeSettings(); val alarms = FakeAlarms()
        val s = service(settings, alarms, loans = { listOf(loan("1", due("2026-12-10"))) })
        val cue = MutableStateFlow(SessionCue.PAUSED)
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        s.bind(scope, cue)
        assertTrue(alarms.set.isEmpty())             // starting up: nothing yet
        cue.value = SessionCue.ACTIVE
        assertEquals(1, alarms.set.size)
        cue.value = SessionCue.PAUSED                // timed out or offline: the alarm stays
        assertEquals(1, alarms.set.size)
        cue.value = SessionCue.ENDED                 // signed out: it goes
        assertTrue(alarms.set.isEmpty())
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
        Unit
    }

    @Test fun `turning the setting off cancels at once even while the session is active`() = runBlocking {
        val settings = FakeSettings(); val alarms = FakeAlarms()
        val s = service(settings, alarms, loans = { listOf(loan("1", due("2026-12-10"))) })
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        s.bind(scope, flowOf(SessionCue.ACTIVE))
        assertEquals(1, alarms.set.size)
        settings.on.value = false
        assertTrue(alarms.set.isEmpty())
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
        Unit
    }
}
