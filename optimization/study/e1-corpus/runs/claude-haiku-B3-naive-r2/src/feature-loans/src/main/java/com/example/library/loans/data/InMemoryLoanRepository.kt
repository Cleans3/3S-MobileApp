package com.example.library.loans.data

import com.example.library.loans.domain.Loan
import com.example.library.loans.domain.LoanRepository
import com.example.library.loans.domain.RenewalDecision
import com.example.library.loans.domain.RenewalPolicy
import com.example.library.loans.domain.RenewalResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/**
 * Keeps loans in memory. Replace with a network- or database-backed implementation
 * by providing another [LoanRepository] to the feature.
 */
class InMemoryLoanRepository(initialLoans: List<Loan>) : LoanRepository {

    private val loans = MutableStateFlow(initialLoans)
    private val renewalLock = Mutex()

    override fun observeLoans(): Flow<List<Loan>> = loans.asStateFlow()

    override fun observeLoan(loanId: String): Flow<Loan?> =
        loans.map { list -> list.firstOrNull { it.id == loanId } }

    override suspend fun renewLoan(loanId: String, today: LocalDate): RenewalResult =
        renewalLock.withLock {
            val current = loans.value.firstOrNull { it.id == loanId }
                ?: return@withLock RenewalResult.NotFound

            when (val decision = RenewalPolicy.check(current, today)) {
                is RenewalDecision.Allowed -> {
                    val updated = current.copy(
                        dueDate = decision.newDueDate,
                        renewalCount = current.renewalCount + 1,
                    )
                    loans.update { list -> list.map { if (it.id == loanId) updated else it } }
                    RenewalResult.Renewed(updated)
                }
                is RenewalDecision.Denied -> RenewalResult.Rejected(decision.reason)
            }
        }
}
