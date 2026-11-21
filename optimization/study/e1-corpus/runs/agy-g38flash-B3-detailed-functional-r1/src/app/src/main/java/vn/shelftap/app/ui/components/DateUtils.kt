package vn.shelftap.app.ui.components

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

object DateUtils {
    private val displayFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

    fun formatIsoDate(isoString: String?): String {
        if (isoString.isNullOrBlank()) return "—"
        return try {
            val instant = Instant.parse(isoString)
            val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
            localDate.format(displayFormatter)
        } catch (e: Exception) {
            isoString
        }
    }

    data class DueStatus(
        val isOverdue: Boolean,
        val daysLeftText: String,
        val formattedDueDate: String
    )

    fun calculateDueStatus(dueAtIso: String): DueStatus {
        val formattedDue = formatIsoDate(dueAtIso)
        return try {
            val dueInstant = Instant.parse(dueAtIso)
            val today = LocalDate.now()
            val dueDate = dueInstant.atZone(ZoneId.systemDefault()).toLocalDate()
            val days = ChronoUnit.DAYS.between(today, dueDate)

            when {
                days < 0 -> {
                    val daysPast = abs(days)
                    DueStatus(
                        isOverdue = true,
                        daysLeftText = if (daysPast == 1L) "1 day overdue" else "$daysPast days overdue",
                        formattedDueDate = formattedDue
                    )
                }
                days == 0L -> {
                    DueStatus(
                        isOverdue = false,
                        daysLeftText = "Due today",
                        formattedDueDate = formattedDue
                    )
                }
                days == 1L -> {
                    DueStatus(
                        isOverdue = false,
                        daysLeftText = "1 day left",
                        formattedDueDate = formattedDue
                    )
                }
                else -> {
                    DueStatus(
                        isOverdue = false,
                        daysLeftText = "$days days left",
                        formattedDueDate = formattedDue
                    )
                }
            }
        } catch (e: Exception) {
            DueStatus(
                isOverdue = false,
                daysLeftText = "",
                formattedDueDate = formattedDue
            )
        }
    }
}
