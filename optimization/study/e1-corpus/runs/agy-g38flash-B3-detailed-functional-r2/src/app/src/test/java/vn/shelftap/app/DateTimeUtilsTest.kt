package vn.shelftap.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.shelftap.app.util.DateTimeUtils
import java.time.Instant
import java.time.temporal.ChronoUnit

class DateTimeUtilsTest {

    @Test
    fun testParseInstant() {
        val instantStr = "2026-11-20T10:15:30Z"
        val instant = DateTimeUtils.parseInstant(instantStr)
        assertEquals(Instant.parse(instantStr), instant)
    }

    @Test
    fun testIsOverdueForPastDate() {
        val pastDate = Instant.now().minus(2, ChronoUnit.DAYS).toString()
        assertTrue(DateTimeUtils.isOverdue(pastDate))
    }

    @Test
    fun testIsOverdueForFutureDate() {
        val futureDate = Instant.now().plus(5, ChronoUnit.DAYS).toString()
        assertFalse(DateTimeUtils.isOverdue(futureDate))
    }

    @Test
    fun testDaysLeftDisplayFuture() {
        val futureDate = Instant.now().plus(4, ChronoUnit.DAYS).toString()
        val display = DateTimeUtils.getDaysLeftDisplay(futureDate)
        assertTrue(display.contains("days left") || display.contains("day left"))
    }

    @Test
    fun testDaysLeftDisplayOverdue() {
        val pastDate = Instant.now().minus(3, ChronoUnit.DAYS).toString()
        val display = DateTimeUtils.getDaysLeftDisplay(pastDate)
        assertTrue(display.contains("overdue"))
    }
}
