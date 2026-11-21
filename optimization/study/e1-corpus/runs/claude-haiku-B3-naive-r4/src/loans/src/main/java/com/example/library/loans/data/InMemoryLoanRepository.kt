package com.example.library.loans.data

import com.example.library.loans.domain.Loan
import com.example.library.loans.domain.LoanRepository
import com.example.library.loans.domain.LoanStatus
import com.example.library.loans.domain.RenewOutcome
import com.example.library.loans.domain.RenewRejection
import com.example.library.loans.domain.renew
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/** Repository backed by process memory. Suitable for demos and tests; no network access. */
class InMemoryLoanRepository(
    initialLoans: List<Loan> = sampleLoans(LocalDate.now()),
) : LoanRepository {

    private val loans = MutableStateFlow(initialLoans)
    private val writeLock = Mutex()

    override fun observeActiveLoans(memberId: String): Flow<List<Loan>> =
        loans.map { all ->
            all.filter { it.memberId == memberId && it.status == LoanStatus.ACTIVE }
                .sortedBy { it.dueDate }
        }

    override fun observeHistory(memberId: String): Flow<List<Loan>> =
        loans.map { all ->
            all.filter { it.memberId == memberId && it.status == LoanStatus.RETURNED }
                .sortedByDescending { it.returnedOn ?: LocalDate.MIN }
        }

    override fun observeLoan(loanId: String): Flow<Loan?> =
        loans.map { all -> all.firstOrNull { it.id == loanId } }

    override suspend fun renewLoan(loanId: String, today: LocalDate): RenewOutcome =
        writeLock.withLock {
            val current = loans.value.firstOrNull { it.id == loanId }
                ?: return@withLock RenewOutcome.Rejected(RenewRejection.NOT_FOUND)
            val outcome = current.renew(today)
            if (outcome is RenewOutcome.Renewed) {
                loans.update { all -> all.map { if (it.id == loanId) outcome.loan else it } }
            }
            outcome
        }
}
