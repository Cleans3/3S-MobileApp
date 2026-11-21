package vn.shelftap.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object DateTimeUtils {
    private val displayDateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
    private val zoneId = ZoneId.systemDefault()

    fun parseInstant(isoString: String?): Instant? {
        if (isoString.isNullOrBlank()) return null
        return try {
            Instant.parse(isoString)
        } catch (e: Exception) {
            null
        }
    }

    fun formatDate(isoString: String?): String {
        val instant = parseInstant(isoString) ?: return "N/A"
        return displayDateFormatter.format(instant.atZone(zoneId).toLocalDate())
    }

    /**
     * Calculates the days remaining until dueAt from current local date.
     * Returns negative value if overdue.
     */
    fun daysUntilDue(dueAtIso: String): Long {
        val instant = parseInstant(dueAtIso) ?: return 0
        val dueDate = instant.atZone(zoneId).toLocalDate()
        val today = LocalDate.now(zoneId)
        return ChronoUnit.DAYS.between(today, dueDate)
    }

    /**
     * Checks whether dueAt is strictly in the past relative to now.
     */
    fun isOverdue(dueAtIso: String): Boolean {
        val instant = parseInstant(dueAtIso) ?: return false
        return instant.isBefore(Instant.now())
    }

    fun getDaysLeftDisplay(dueAtIso: String): String {
        val days = daysUntilDue(dueAtIso)
        return when {
            days < 0 -> "${-days} day${if (-days == 1L) "" else "s"} overdue"
            days == 0L -> "Due today"
            days == 1L -> "1 day left"
            else -> "$days days left"
        }
    }
}
