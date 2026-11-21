package com.example.libraryloans

import com.example.libraryloans.data.LoanRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class LoanRepositoryTest {

    private lateinit var repository: LoanRepository

    @Before
    fun setUp() {
        repository = LoanRepository()
    }

    @Test
    fun testInitialLoansLoaded() {
        val activeLoans = repository.activeLoans.value
        val history = repository.loanHistory.value

        assertTrue(activeLoans.isNotEmpty())
        assertTrue(history.isNotEmpty())
        assertEquals("M-90241", repository.member.value.id)
    }

    @Test
    fun testRenewValidLoanSuccess() {
        // L-101 has 2 renewals remaining and is not overdue
        val loan101 = repository.activeLoans.value.first { it.id == "L-101" }
        val originalDueDate = loan101.dueDate
        val originalRenewals = loan101.renewalsRemaining

        val result = repository.renewLoan("L-101")
        assertTrue(result.isSuccess)

        val updated = result.getOrNull()
        assertNotNull(updated)
        assertEquals(originalDueDate.plusDays(14), updated?.dueDate)
        assertEquals(originalRenewals - 1, updated?.renewalsRemaining)
        assertEquals(1, updated?.renewalCount)
    }

    @Test
    fun testRenewOverdueLoanFails() {
        // L-103 is overdue
        val loan103 = repository.activeLoans.value.first { it.id == "L-103" }
        assertTrue(loan103.isOverdue)

        val result = repository.renewLoan("L-103")
        assertTrue(result.isFailure)
    }

    @Test
    fun testReturnLoanMovesToHistory() {
        val initialActiveCount = repository.activeLoans.value.size
        val initialHistoryCount = repository.loanHistory.value.size

        val result = repository.returnLoan("L-101")
        assertTrue(result.isSuccess)

        assertEquals(initialActiveCount - 1, repository.activeLoans.value.size)
        assertEquals(initialHistoryCount + 1, repository.loanHistory.value.size)
        assertNotNull(repository.findHistoryByBookId("B-001"))
    }
}
