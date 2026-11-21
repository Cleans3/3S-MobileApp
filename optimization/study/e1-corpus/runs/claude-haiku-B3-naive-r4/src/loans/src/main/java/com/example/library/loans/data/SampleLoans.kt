package com.example.library.loans.data

import com.example.library.loans.domain.Book
import com.example.library.loans.domain.Loan
import com.example.library.loans.domain.LoanStatus
import java.time.LocalDate

/** Demo data, with dates relative to [today] so the sample always shows current, overdue and past loans. */
fun sampleLoans(today: LocalDate): List<Loan> {
    val pragmatic = Book(
        id = "b1",
        title = "The Pragmatic Programmer",
        author = "David Thomas & Andrew Hunt",
        isbn = "9780135957059",
        description = "A guide to pragmatic habits for software craftsmen, covering code, teams and tools.",
        pages = 352,
    )
    val cleanCode = Book(
        id = "b2",
        title = "Clean Code",
        author = "Robert C. Martin",
        isbn = "9780132350884",
        description = "Principles and practices for writing readable, maintainable code.",
        pages = 464,
    )
    val refactoring = Book(
        id = "b3",
        title = "Refactoring",
        author = "Martin Fowler",
        isbn = "9780134757599",
        description = "Improving the design of existing code through small, safe transformations.",
        pages = 448,
    )
    val kleppmann = Book(
        id = "b4",
        title = "Designing Data-Intensive Applications",
        author = "Martin Kleppmann",
        isbn = "9781449373320",
        description = "The big ideas behind reliable, scalable and maintainable data systems.",
        pages = 616,
    )
    val mythical = Book(
        id = "b5",
        title = "The Mythical Man-Month",
        author = "Frederick P. Brooks Jr.",
        isbn = "9780201835953",
        description = "Essays on the management of large software projects.",
        pages = 322,
    )

    return listOf(
        Loan(
            id = "l1",
            memberId = "member-1",
            book = pragmatic,
            borrowedOn = today.minusDays(14),
            dueDate = today.plusDays(7),
        ),
        Loan(
            id = "l2",
            memberId = "member-1",
            book = cleanCode,
            borrowedOn = today.minusDays(30),
            dueDate = today.plusDays(2),
            renewCount = 1,
        ),
        Loan(
            id = "l3",
            memberId = "member-1",
            book = refactoring,
            borrowedOn = today.minusDays(40),
            dueDate = today.minusDays(5),
            renewCount = 2,
        ),
        Loan(
            id = "h1",
            memberId = "member-1",
            book = kleppmann,
            borrowedOn = today.minusDays(70),
            dueDate = today.minusDays(49),
            status = LoanStatus.RETURNED,
            returnedOn = today.minusDays(45),
        ),
        Loan(
            id = "h2",
            memberId = "member-1",
            book = mythical,
            borrowedOn = today.minusDays(120),
            dueDate = today.minusDays(99),
            renewCount = 1,
            status = LoanStatus.RETURNED,
            returnedOn = today.minusDays(100),
        ),
        // Belongs to another member; must never appear for member-1.
        Loan(
            id = "l9",
            memberId = "member-2",
            book = pragmatic,
            borrowedOn = today.minusDays(3),
            dueDate = today.plusDays(18),
        ),
    )
}
