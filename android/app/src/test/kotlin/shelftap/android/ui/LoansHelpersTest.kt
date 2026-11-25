package shelftap.android.ui

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import shelftap.android.api.Fine

/** The figures on the loans screen: how many days are left, whether a loan is overdue, and what is owed. */
class LoansHelpersTest {
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")
    private val now = Instant.parse("2026-11-25T03:00:00Z") // 10:00 on the 25th in Ho Chi Minh City

    @Test
    fun daysToDueCountsCalendarDaysInTheLocalZone() {
        assertEquals(14, daysToDue("2026-12-09T03:00:00Z", now, zone))
        assertEquals(0, daysToDue("2026-11-25T16:00:00Z", now, zone)) // later today
        assertEquals(-2, daysToDue("2026-11-23T03:00:00Z", now, zone))
        assertEquals(1, daysToDue("2026-11-25T17:30:00Z", now, zone)) // 00:30 tomorrow locally, though the same UTC day
    }

    @Test
    fun anUnreadableDateIsNeitherOverdueNorCounted() {
        assertNull(daysToDue("soon", now, zone))
        assertFalse(isOverdue("soon", now))
    }

    @Test
    fun overdueMeansTheDueInstantHasPassed() {
        assertTrue(isOverdue("2026-11-25T02:59:59Z", now))
        assertFalse(isOverdue("2026-11-25T03:00:01Z", now))
    }

    @Test
    fun owedIsTheSumOfThisLoansUnpaidFinesOnly() {
        val fines = listOf(
            Fine("f1", "l1", 2000, null), Fine("f2", "l1", 3000, null), Fine("f3", "l1", 9000, "2026-11-20T00:00:00Z"), Fine("f4", "l2", 5000, null),
        )
        assertEquals(5000, unpaidFor(fines, "l1"))
        assertEquals(5000, unpaidFor(fines, "l2"))
        assertEquals(0, unpaidFor(fines, "l3"))
    }

    @Test
    fun amountsUseTheLocalesGrouping() {
        assertEquals("2.000 ₫", formatVnd(2000, Locale.forLanguageTag("vi-VN")))
        assertEquals("0 ₫", formatVnd(0, Locale.forLanguageTag("vi-VN")))
        assertEquals("1,250,000 ₫", formatVnd(1_250_000, Locale.US))
    }
}
