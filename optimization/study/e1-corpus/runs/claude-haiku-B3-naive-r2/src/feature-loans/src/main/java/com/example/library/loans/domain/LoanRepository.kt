package com.example.library.loans.domain

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

sealed interface RenewalResult {
    data class Renewed(val loan: Loan) : RenewalResult
    data class Rejected(val reason: DenialReason) : RenewalResult
    data object NotFound : RenewalResult
}

interface LoanRepository {
    /** All loans of the member, active and returned. */
    fun observeLoans(): Flow<List<Loan>>

    /** A single loan (and therefore its book), or null if it does not exist. */
    fun observeLoan(loanId: String): Flow<Loan?>

    suspend fun renewLoan(loanId: String, today: LocalDate): RenewalResult
}
