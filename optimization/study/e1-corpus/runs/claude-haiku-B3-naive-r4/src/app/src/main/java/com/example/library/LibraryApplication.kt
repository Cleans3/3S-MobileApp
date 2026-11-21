package com.example.library

import android.app.Application
import com.example.library.loans.data.InMemoryLoanRepository
import com.example.library.loans.domain.LoanRepository

class LibraryApplication : Application() {

    // Kept at application scope so loan state survives configuration changes.
    val loanRepository: LoanRepository by lazy { InMemoryLoanRepository() }
}
