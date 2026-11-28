package shelftap.android.reminder

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import shelftap.android.api.Loan

/**
 * One reminder: [count] loans fall due on [dueDay]; the phone shows it at [triggerAt]. [title] is the book's title when exactly one loan
 * falls due that day (the notification names it after the phone is unlocked), else null.
 */
data class DueReminder(val triggerAt: Instant, val dueDay: LocalDate, val count: Int, val title: String?)

/**
 * Works out the reminders for the member's loans on the phone, from `dueAt`: one the day before a loan falls due, at 07:30 local time.
 * Loans due the same day share one reminder. A reminder whose time has already passed is not planned (the member is looking at the app,
 * and a notification for a past moment would only be noise). Pure: no clock, no Android.
 */
object DueReminderPlanner {
    const val LEAD_DAYS = 1L
    val AT: LocalTime = LocalTime.of(7, 30)

    /** AlarmManager keeps at most 500 alarms per app; a member has a handful of loans, this is only a guard. */
    const val MAX_REMINDERS = 20

    fun plan(loans: List<Loan>, now: Instant, zone: ZoneId): List<DueReminder> =
        loans.filter { it.returnedAt == null }
            .mapNotNull { loan -> dueDay(loan.dueAt, zone)?.let { it to loan.title } }
            .groupBy({ it.first }, { it.second })
            .map { (day, titles) ->
                DueReminder(day.minusDays(LEAD_DAYS).atTime(AT).atZone(zone).toInstant(), day, titles.size, titles.singleOrNull())
            }
            .filter { it.triggerAt.isAfter(now) }
            .sortedBy { it.triggerAt }
            .take(MAX_REMINDERS)

    /** The local day a loan falls due, or null when the server's timestamp cannot be read. */
    private fun dueDay(dueAt: String, zone: ZoneId): LocalDate? =
        try { Instant.parse(dueAt).atZone(zone).toLocalDate() } catch (_: DateTimeParseException) { null }
}
