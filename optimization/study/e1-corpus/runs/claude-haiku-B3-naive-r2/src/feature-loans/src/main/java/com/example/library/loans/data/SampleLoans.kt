package com.example.library.loans.data

import com.example.library.loans.domain.Book
import com.example.library.loans.domain.Loan
import java.time.LocalDate

/** Demo data relative to [today] so the screens always show a realistic mix of states. */
object SampleLoans {

    fun create(today: LocalDate): List<Loan> {
        val dune = Book(
            id = "b-dune",
            title = "Dune",
            author = "Frank Herbert",
            isbn = "9780441013593",
            publishedYear = 1965,
            pageCount = 688,
            description = "A desert planet, a precious spice and a prophecy that binds a young heir to a dangerous destiny.",
        )
        val pride = Book(
            id = "b-pride",
            title = "Pride and Prejudice",
            author = "Jane Austen",
            isbn = "9780141439518",
            publishedYear = 1813,
            pageCount = 432,
            description = "The witty and independent Elizabeth Bennet navigates love, class and family expectations.",
        )
        val hobbit = Book(
            id = "b-hobbit",
            title = "The Hobbit",
            author = "J. R. R. Tolkien",
            isbn = "9780547928227",
            publishedYear = 1937,
            pageCount = 310,
            description = "Bilbo Baggins is swept into an adventure to reclaim a treasure guarded by a dragon.",
        )
        val neuromancer = Book(
            id = "b-neuromancer",
            title = "Neuromancer",
            author = "William Gibson",
            isbn = "9780441569595",
            publishedYear = 1984,
            pageCount = 271,
            description = "A washed-up hacker takes one last job that could change the future of cyberspace.",
        )

        return listOf(
            Loan(
                id = "loan-1",
                book = dune,
                borrowedOn = today.minusDays(10),
                dueDate = today.plusDays(4),
                returnedOn = null,
                renewalCount = 1,
            ),
            Loan(
                id = "loan-2",
                book = pride,
                borrowedOn = today.minusDays(30),
                dueDate = today.minusDays(2),
                returnedOn = null,
                renewalCount = 0,
            ),
            Loan(
                id = "loan-3",
                book = hobbit,
                borrowedOn = today.minusDays(70),
                dueDate = today.minusDays(40),
                returnedOn = today.minusDays(42),
                renewalCount = 1,
            ),
            Loan(
                id = "loan-4",
                book = neuromancer,
                borrowedOn = today.minusDays(150),
                dueDate = today.minusDays(120),
                returnedOn = today.minusDays(118),
                renewalCount = 2,
            ),
        )
    }
}
