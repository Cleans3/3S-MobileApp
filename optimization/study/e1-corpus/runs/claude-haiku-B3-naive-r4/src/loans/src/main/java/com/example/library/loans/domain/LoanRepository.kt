package com.example.library.loans.domain

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface LoanRepository {
    /** Loans currently out to [memberId], ordered by due date. */
    fun observeActiveLoans(memberId: String): Flow<List<Loan>>

    /** Loans [memberId] has returned, most recently returned first. */
    fun observeHistory(memberId: String): Flow<List<Loan>>

    /** A single loan by id, or null if it does not exist. */
    fun observeLoan(loanId: String): Flow<Loan?>

    suspend fun renewLoan(loanId: String, today: LocalDate): RenewOutcome
}
