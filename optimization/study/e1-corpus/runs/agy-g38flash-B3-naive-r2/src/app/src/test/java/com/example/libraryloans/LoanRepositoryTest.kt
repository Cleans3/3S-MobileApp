package com.example.libraryloans

import com.example.libraryloans.data.LoanRepository
import com.example.libraryloans.model.LoanStatus
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
    fun testActiveLoansAndHistorySeparation() {
        val allLoans = repository.allLoans.value
        val activeLoans = allLoans.filter { !it.isReturned }
        val historyLoans = allLoans.filter { it.isReturned }

        assertTrue("Should have active loans", activeLoans.isNotEmpty())
        assertTrue("Should have history loans", historyLoans.isNotEmpty())
        assertEquals(allLoans.size, activeLoans.size + historyLoans.size)
    }

    @Test
    fun testRenewLoanExtendsDueDate() {
        val initialLoans = repository.allLoans.value
        val renewableLoan = initialLoans.first { it.canRenew }
        val originalDueDate = renewableLoan.dueDate
        val originalRenewCount = renewableLoan.renewalCount

        val result = repository.renewLoan(renewableLoan.id)

        assertTrue("Renew should succeed", result.isSuccess)
        val renewed = result.getOrNull()
        assertNotNull(renewed)
        assertEquals(originalDueDate.plusDays(14), renewed!!.dueDate)
        assertEquals(originalRenewCount + 1, renewed.renewalCount)
    }

    @Test
    fun testRenewLoanRespectsMaxLimit() {
        val initialLoans = repository.allLoans.value
        val loan = initialLoans.first { it.canRenew }
        var currentLoanId = loan.id

        // Renew until max reached
        val maxRenewals = loan.maxRenewals
        for (i in (loan.renewalCount until maxRenewals)) {
            val res = repository.renewLoan(currentLoanId)
            assertTrue("Renewal $i should succeed", res.isSuccess)
        }

        // Next renewal should fail
        val excessResult = repository.renewLoan(currentLoanId)
        assertTrue("Exceeding max renewals should fail", excessResult.isFailure)
    }

    @Test
    fun testCannotRenewReturnedLoan() {
        val returnedLoan = repository.allLoans.value.first { it.isReturned }
        val result = repository.renewLoan(returnedLoan.id)

        assertTrue("Renewing returned loan should fail", result.isFailure)
    }
}
