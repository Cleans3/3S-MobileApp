package com.example.library.loans.data

import com.example.library.loans.model.Book
import com.example.library.loans.model.Loan
import java.time.LocalDate

/** Demo data relative to [today], so the active and overdue examples stay meaningful. */
object SampleData {
    private val dune = Book(
        id = "book-dune",
        title = "Dune",
        author = "Frank Herbert",
        isbn = "9780441013593",
        pages = 412,
        description = "A desert planet, a precious spice and a young heir with a destiny.",
    )
    private val neuromancer = Book(
        id = "book-neuromancer",
        title = "Neuromancer",
        author = "William Gibson",
        isbn = "9780441569595",
        pages = 271,
        description = "A washed-up hacker takes one last job in a world of cyberspace.",
    )
    private val prideAndPrejudice = Book(
        id = "book-pride",
        title = "Pride and Prejudice",
        author = "Jane Austen",
        isbn = "9780141439518",
        pages = 432,
        description = "Elizabeth Bennet navigates love, class and first impressions in Regency England.",
    )
    private val hobbit = Book(
        id = "book-hobbit",
        title = "The Hobbit",
        author = "J. R. R. Tolkien",
        isbn = "9780547928227",
        pages = 310,
        description = "Bilbo Baggins is swept into an adventure to reclaim a lost treasure.",
    )
    private val nineteenEightyFour = Book(
        id = "book-1984",
        title = "Nineteen Eighty-Four",
        author = "George Orwell",
        isbn = "9780451524935",
        pages = 328,
        description = "A totalitarian state watches its citizens every moment of their lives.",
    )

    fun loans(today: LocalDate = LocalDate.now()): List<Loan> = listOf(
        Loan("loan-1", dune, today.minusDays(10), today.plusDays(4)),
        Loan("loan-2", neuromancer, today.minusDays(30), today.minusDays(2)),
        Loan(
            "loan-3", prideAndPrejudice, today.minusDays(90), today.minusDays(76),
            renewalCount = 0, returnedOn = today.minusDays(80),
        ),
        Loan(
            "loan-4", hobbit, today.minusDays(200), today.minusDays(172),
            renewalCount = 1, returnedOn = today.minusDays(190),
        ),
        Loan(
            "loan-5", nineteenEightyFour, today.minusDays(120), today.minusDays(106),
            returnedOn = today.minusDays(104),
        ),
    )
}
