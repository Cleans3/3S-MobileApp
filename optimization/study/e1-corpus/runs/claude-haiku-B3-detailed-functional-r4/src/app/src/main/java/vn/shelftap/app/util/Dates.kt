package vn.shelftap.app.util

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

object Dates {
    private val zone: ZoneId = ZoneId.systemDefault()
    private val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(zone)

    /** Parses an ISO-8601 timestamp (the API sends UTC, e.g. 2026-11-21T08:00:00Z). */
    fun parse(iso: String): Instant = OffsetDateTime.parse(iso).toInstant()

    fun formatDate(instant: Instant): String = dateFormat.format(instant)

    /** Calendar days from today to the given instant, in the device time zone. Negative when past. */
    fun daysUntil(instant: Instant, now: Instant = Instant.now()): Long =
        ChronoUnit.DAYS.between(now.atZone(zone).toLocalDate(), instant.atZone(zone).toLocalDate())
}
