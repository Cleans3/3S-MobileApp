package com.example.library.loans.data

import com.example.library.loans.domain.Book
import com.example.library.loans.domain.Loan
import java.time.LocalDate

object SampleData {

    fun books(): List<Book> = listOf(
        Book(
            id = "book-1",
            title = "The Pragmatic Programmer",
            author = "Andrew Hunt, David Thomas",
            isbn = "9780135957059",
            pages = 352,
            description = "A guide to pragmatic habits for software craftsmanship.",
        ),
        Book(
            id = "book-2",
            title = "Clean Code",
            author = "Robert C. Martin",
            isbn = "9780132350884",
            pages = 464,
            description = "Principles and practices for writing readable, maintainable code.",
        ),
        Book(
            id = "book-3",
            title = "Dune",
            author = "Frank Herbert",
            isbn = "9780441172719",
            pages = 688,
            description = "Epic science fiction about politics, ecology and prophecy on Arrakis.",
        ),
        Book(
            id = "book-4",
            title = "The Hobbit",
            author = "J.R.R. Tolkien",
            isbn = "9780547928227",
            pages = 310,
            description = "Bilbo Baggins joins a company of dwarves on a quest to reclaim treasure.",
        ),
        Book(
            id = "book-5",
            title = "Sapiens",
            author = "Yuval Noah Harari",
            isbn = "9780062316097",
            pages = 443,
            description = "A brief history of humankind from the Stone Age to the present.",
        ),
        Book(
            id = "book-6",
            title = "Refactoring",
            author = "Martin Fowler",
            isbn = "9780134757599",
            pages = 448,
            description = "Improving the design of existing code, one small step at a time.",
        ),
    )

    fun loans(today: LocalDate): List<Loan> = listOf(
        // Active loans of member-1
        Loan(
            id = "loan-1",
            bookId = "book-2",
            memberId = "member-1",
            borrowedOn = today.minusDays(10),
            dueDate = today.plusDays(4),
        ),
        Loan(
            id = "loan-2",
            bookId = "book-3",
            memberId = "member-1",
            borrowedOn = today.minusDays(20),
            dueDate = today.minusDays(6), // overdue: cannot be renewed
            renewalCount = 1,
        ),
        Loan(
            id = "loan-3",
            bookId = "book-6",
            memberId = "member-1",
            borrowedOn = today.minusDays(3),
            dueDate = today.plusDays(11),
        ),
        Loan(
            id = "loan-4",
            bookId = "book-5",
            memberId = "member-1",
            borrowedOn = today.minusDays(25),
            dueDate = today.plusDays(5),
            renewalCount = 2, // limit reached
        ),
        // Past loans of member-1
        Loan(
            id = "loan-5",
            bookId = "book-1",
            memberId = "member-1",
            borrowedOn = today.minusDays(120),
            dueDate = today.minusDays(106),
            returnedOn = today.minusDays(100),
            renewalCount = 1,
        ),
        Loan(
            id = "loan-6",
            bookId = "book-4",
            memberId = "member-1",
            borrowedOn = today.minusDays(300),
            dueDate = today.minusDays(286),
            returnedOn = today.minusDays(270),
        ),
        // Another member, used to show that histories are per member
        Loan(
            id = "loan-7",
            bookId = "book-3",
            memberId = "member-2",
            borrowedOn = today.minusDays(60),
            dueDate = today.minusDays(46),
            returnedOn = today.minusDays(40),
        ),
    )
}
