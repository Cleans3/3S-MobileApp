package vn.shelftap.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object DateUtils {

    fun parseIsoInstant(isoString: String?): Instant? {
        if (isoString.isNullOrBlank()) return null
        return try {
            Instant.parse(isoString)
        } catch (e: Exception) {
            try {
                OffsetDateTime.parse(isoString).toInstant()
            } catch (e2: Exception) {
                null
            }
        }
    }

    fun formatDate(isoString: String?): String {
        val instant = parseIsoInstant(isoString) ?: return isoString ?: "—"
        val zone = ZoneId.systemDefault()
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy").withZone(zone)
        return formatter.format(instant)
    }

    /**
     * Calculates days left until due date and whether it is overdue.
     * Returns Pair(daysLeft, isOverdue).
     * If due date is today, daysLeft is 0 and isOverdue is false.
     * If due date was yesterday, daysLeft is -1 and isOverdue is true.
     */
    fun calculateDaysLeft(dueAtIso: String?): Pair<Long, Boolean> {
        val instant = parseIsoInstant(dueAtIso) ?: return Pair(0L, false)
        val zone = ZoneId.systemDefault()
        val dueDate = instant.atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        val days = ChronoUnit.DAYS.between(today, dueDate)
        val isOverdue = days < 0
        return Pair(days, isOverdue)
    }

    fun formatDaysLeft(daysLeft: Long, isOverdue: Boolean): String {
        return when {
            isOverdue -> {
                val absDays = kotlin.math.abs(daysLeft)
                if (absDays == 1L) "1 day overdue" else "$absDays days overdue"
            }
            daysLeft == 0L -> "Due today"
            daysLeft == 1L -> "1 day left"
            else -> "$daysLeft days left"
        }
    }
}
