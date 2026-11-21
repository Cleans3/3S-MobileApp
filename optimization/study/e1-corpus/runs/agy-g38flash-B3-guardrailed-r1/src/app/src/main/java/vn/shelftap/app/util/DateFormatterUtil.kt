package vn.shelftap.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

object DateFormatterUtil {
    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    fun parseInstant(isoUtcString: String?): Instant? {
        if (isoUtcString.isNullOrBlank()) return null
        return try {
            Instant.parse(isoUtcString)
        } catch (e: Exception) {
            null
        }
    }

    fun formatDate(isoUtcString: String?): String {
        if (isoUtcString.isNullOrBlank()) return "—"
        val instant = parseInstant(isoUtcString) ?: return isoUtcString
        val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        return localDate.format(dateFormatter)
    }

    data class DueStatus(
        val formattedDueDate: String,
        val daysLeftText: String,
        val isOverdue: Boolean
    )

    fun calculateDueStatus(dueAtIso: String): DueStatus {
        val instant = parseInstant(dueAtIso)
        val formatted = formatDate(dueAtIso)
        if (instant == null) {
            return DueStatus(formattedDueDate = formatted, daysLeftText = "", isOverdue = false)
        }

        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val dueDate = instant.atZone(zoneId).toLocalDate()
        val daysBetween = ChronoUnit.DAYS.between(today, dueDate)
        val isOverdue = today.isAfter(dueDate)

        val daysLeftText = when {
            isOverdue -> "Overdue"
            daysBetween == 0L -> "Due today"
            daysBetween == 1L -> "1 day left"
            else -> "$daysBetween days left"
        }

        return DueStatus(
            formattedDueDate = formatted,
            daysLeftText = daysLeftText,
            isOverdue = isOverdue
        )
    }
}
