package com.example.libraryloans.data

import com.example.libraryloans.model.Book
import com.example.libraryloans.model.Loan
import com.example.libraryloans.model.LoanHistory
import com.example.libraryloans.model.Member
import java.time.LocalDate

object SampleData {
    val currentMember = Member(
        id = "M-90241",
        name = "Eleanor Vance",
        cardNumber = "LIB-2024-8842",
        email = "e.vance@readwell.org",
        memberSince = LocalDate.of(2021, 5, 12),
        cardExpiry = LocalDate.of(2027, 5, 12),
        maxLoansAllowed = 10
    )

    val book1 = Book(
        id = "B-001",
        title = "To Kill a Mockingbird",
        author = "Harper Lee",
        isbn = "978-0-06-112008-4",
        genre = "Classic Fiction",
        description = "The unforgettable novel of a childhood in a sleepy Southern town and the crisis of conscience that rocked it. Compassionate, dramatic, and deeply moving, 'To Kill A Mockingbird' takes readers to the roots of human behavior.",
        publisher = "J. B. Lippincott & Co.",
        publishedYear = 1960,
        pageCount = 336,
        coverColorHex = 0xFF2E4057,
        callNumber = "FIC LEE 1960"
    )

    val book2 = Book(
        id = "B-002",
        title = "Dune",
        author = "Frank Herbert",
        isbn = "978-0-441-17271-9",
        genre = "Science Fiction",
        description = "Set on the desert planet Arrakis, Dune is the story of the boy Paul Atreides, heir to a noble family tasked with ruling an inhospitable world where the only valuable thing is the spice melange.",
        publisher = "Chilton Books",
        publishedYear = 1965,
        pageCount = 688,
        coverColorHex = 0xFFD47A3B,
        callNumber = "SCIFI HER 1965"
    )

    val book3 = Book(
        id = "B-003",
        title = "Clean Code",
        author = "Robert C. Martin",
        isbn = "978-0-13-235088-4",
        genre = "Computer Science",
        description = "Even bad code can function. But if code isn't clean, it can bring a development organization to its knees. Every year, countless hours and significant resources are lost due to poorly written code.",
        publisher = "Prentice Hall",
        publishedYear = 2008,
        pageCount = 464,
        coverColorHex = 0xFF1D5A6C,
        callNumber = "005.1 MAR 2008"
    )

    val book4 = Book(
        id = "B-004",
        title = "The Hobbit",
        author = "J.R.R. Tolkien",
        isbn = "978-0-547-92822-7",
        genre = "Fantasy",
        description = "Bilbo Baggins is a hobbit who enjoys a comfortable, unambitious life, rarely traveling any further than his pantry or cellar. But his contentment is disturbed when the wizard Gandalf and a company of dwarves arrive on his doorstep.",
        publisher = "George Allen & Unwin",
        publishedYear = 1937,
        pageCount = 310,
        coverColorHex = 0xFF355E3B,
        callNumber = "FAN TOL 1937"
    )

    val book5 = Book(
        id = "B-005",
        title = "Klara and the Sun",
        author = "Kazuo Ishiguro",
        isbn = "978-0-593-31817-1",
        genre = "Literary Fiction",
        description = "Klara and the Sun tells the story of Klara, an Artificial Friend with outstanding observational qualities, who, from her place in the store, watches carefully the behaviour of those who come in to browse.",
        publisher = "Faber and Faber",
        publishedYear = 2021,
        pageCount = 303,
        coverColorHex = 0xFFB5525C,
        callNumber = "FIC ISH 2021"
    )

    val book6 = Book(
        id = "B-006",
        title = "Atomic Habits",
        author = "James Clear",
        isbn = "978-0-7352-1129-2",
        genre = "Self-Help / Psychology",
        description = "An easy and proven way to build good habits and break bad ones. If you're having trouble changing your habits, the problem isn't you; the problem is your system.",
        publisher = "Avery",
        publishedYear = 2018,
        pageCount = 320,
        coverColorHex = 0xFFE08D3C,
        callNumber = "158.1 CLE 2018"
    )

    val book7 = Book(
        id = "B-007",
        title = "Project Hail Mary",
        author = "Andy Weir",
        isbn = "978-0-593-13520-4",
        genre = "Science Fiction",
        description = "Ryland Grace is the sole survivor on a desperate, last-chance mission—and if he fails, humanity and the earth itself are doomed. Except right now, he doesn't know that. He can't even remember his own name.",
        publisher = "Ballantine Books",
        publishedYear = 2021,
        pageCount = 496,
        coverColorHex = 0xFF3A506B,
        callNumber = "SCIFI WEI 2021"
    )

    val initialActiveLoans = listOf(
        Loan(
            id = "L-101",
            book = book1,
            borrowDate = LocalDate.now().minusDays(10),
            dueDate = LocalDate.now().plusDays(4),
            renewalsRemaining = 2,
            renewalCount = 0
        ),
        Loan(
            id = "L-102",
            book = book2,
            borrowDate = LocalDate.now().minusDays(20),
            dueDate = LocalDate.now().plusDays(1), // due soon
            renewalsRemaining = 1,
            renewalCount = 1
        ),
        Loan(
            id = "L-103",
            book = book3,
            borrowDate = LocalDate.now().minusDays(18),
            dueDate = LocalDate.now().minusDays(2), // overdue
            renewalsRemaining = 0,
            renewalCount = 2
        )
    )

    val initialHistory = listOf(
        LoanHistory(
            id = "H-201",
            book = book4,
            borrowDate = LocalDate.now().minusDays(90),
            returnDate = LocalDate.now().minusDays(76),
            userRating = 5,
            notes = "Magnificent storytelling. A comfort reread."
        ),
        LoanHistory(
            id = "H-202",
            book = book5,
            borrowDate = LocalDate.now().minusDays(65),
            returnDate = LocalDate.now().minusDays(51),
            userRating = 4,
            notes = "Poignant and beautifully observed perspective on humanity."
        ),
        LoanHistory(
            id = "H-203",
            book = book6,
            borrowDate = LocalDate.now().minusDays(45),
            returnDate = LocalDate.now().minusDays(32),
            userRating = 5,
            notes = "Very practical insights on habit stacking and environmental cues."
        ),
        LoanHistory(
            id = "H-204",
            book = book7,
            borrowDate = LocalDate.now().minusDays(35),
            returnDate = LocalDate.now().minusDays(21),
            userRating = 5,
            notes = "Rocky is one of the best sci-fi characters ever written. AMAZE!"
        )
    )
}
