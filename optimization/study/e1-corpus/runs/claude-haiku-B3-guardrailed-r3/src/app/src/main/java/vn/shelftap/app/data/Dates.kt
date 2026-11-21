package vn.shelftap.app.data

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle

private val displayDate: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())

/** Parses the API's ISO-8601 timestamps (UTC with "Z", or with an explicit offset). */
fun parseInstant(iso: String): Instant = try {
    Instant.parse(iso)
} catch (e: DateTimeParseException) {
    OffsetDateTime.parse(iso).toInstant()
}

fun formatDate(instant: Instant): String = displayDate.format(instant)
