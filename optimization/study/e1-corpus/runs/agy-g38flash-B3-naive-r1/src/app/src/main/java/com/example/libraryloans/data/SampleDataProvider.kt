package com.example.libraryloans.data

import com.example.libraryloans.model.Book
import com.example.libraryloans.model.Loan
import java.time.LocalDate

object SampleDataProvider {

    val books: Map<String, Book> = listOf(
        Book(
            id = "book_1",
            title = "Clean Architecture: A Craftsman's Guide to Software Structure",
            author = "Robert C. Martin",
            isbn = "978-0134494166",
            publisher = "Prentice Hall",
            publishYear = 2017,
            genre = "Software Engineering",
            pages = 432,
            description = "By applying universal rules of software architecture, you can dramatically improve developer productivity throughout the life of any software system. Uncle Bob provides practical solutions for the real challenges you'll face.",
            coverAccentHex = 0xFF1E3A8A,
            shelfLocation = "Tech Stacks 3A-12"
        ),
        Book(
            id = "book_2",
            title = "To Kill a Mockingbird",
            author = "Harper Lee",
            isbn = "978-0060935467",
            publisher = "Harper Perennial",
            publishYear = 1960,
            genre = "Classic Fiction",
            pages = 323,
            description = "The unforgettable novel of a childhood in a sleepy Southern town and the crisis of conscience that rocked it. Compassionate, dramatic, and deeply moving, taking readers to the roots of human behavior.",
            coverAccentHex = 0xFF854D0E,
            shelfLocation = "Classics Shelf 4B"
        ),
        Book(
            id = "book_3",
            title = "Dune",
            author = "Frank Herbert",
            isbn = "978-0441172719",
            publisher = "Chilton Books",
            publishYear = 1965,
            genre = "Science Fiction",
            pages = 688,
            description = "Set on the desert planet Arrakis, Dune is the story of the boy Paul Atreides, heir to a noble family tasked with ruling an inhospitable world where the only thing of value is the 'spice' melange.",
            coverAccentHex = 0xFFC2410C,
            shelfLocation = "Sci-Fi Floor 1-D"
        ),
        Book(
            id = "book_4",
            title = "Designing Data-Intensive Applications",
            author = "Martin Kleppmann",
            isbn = "978-1449373320",
            publisher = "O'Reilly Media",
            publishYear = 2017,
            genre = "Data Engineering",
            pages = 616,
            description = "Data is at the center of many challenges in system design today. Difficult issues need to be figured out, such as scalability, consistency, reliability, efficiency, and maintainability.",
            coverAccentHex = 0xFF047857,
            shelfLocation = "Tech Stacks 3A-19"
        ),
        Book(
            id = "book_5",
            title = "The Great Gatsby",
            author = "F. Scott Fitzgerald",
            isbn = "978-0743273565",
            publisher = "Scribner",
            publishYear = 1925,
            genre = "Classic Fiction",
            pages = 180,
            description = "The exemplary novel of the Jazz Age, exploring themes of decadence, idealism, social upheaval, and excess in the Roaring Twenties on Long Island.",
            coverAccentHex = 0xFF4338CA,
            shelfLocation = "Classics Shelf 2A"
        ),
        Book(
            id = "book_6",
            title = "1984",
            author = "George Orwell",
            isbn = "978-0451524935",
            publisher = "Signet Classics",
            publishYear = 1949,
            genre = "Dystopian Fiction",
            pages = 328,
            description = "Winston Smith toes the Party line, rewriting history to satisfy the Ministry of Truth. With each lie he writes, Winston grows to hate the Party that seeks power for its own sake.",
            coverAccentHex = 0xFFB91C1C,
            shelfLocation = "Classics Shelf 1F"
        ),
        Book(
            id = "book_7",
            title = "Kotlin in Action",
            author = "Dmitry Jemerov & Svetlana Isakova",
            isbn = "978-1617293290",
            publisher = "Manning Publications",
            publishYear = 2017,
            genre = "Programming",
            pages = 360,
            description = "Kotlin in Action guides experienced Java developers from the basics through advanced features to create robust and elegant code on the JVM and Android.",
            coverAccentHex = 0xFF7E22CE,
            shelfLocation = "Tech Stacks 2C-05"
        ),
        Book(
            id = "book_8",
            title = "Project Hail Mary",
            author = "Andy Weir",
            isbn = "978-0593135204",
            publisher = "Ballantine Books",
            publishYear = 2021,
            genre = "Science Fiction",
            pages = 496,
            description = "Ryland Grace is the sole survivor on a desperate, last-chance mission—and if he fails, humanity and the earth itself are doomed. He just doesn't remember who he is.",
            coverAccentHex = 0xFF0284C7,
            shelfLocation = "Sci-Fi Floor 1-G"
        )
    ).associateBy { it.id }

    fun initialLoans(): List<Loan> {
        val today = LocalDate.now()
        return listOf(
            // Active loans
            Loan(
                id = "loan_active_1",
                bookId = "book_1",
                borrowDate = today.minusDays(18),
                dueDate = today.plusDays(3),
                renewalCount = 0,
                maxRenewals = 2
            ),
            Loan(
                id = "loan_active_2",
                bookId = "book_2",
                borrowDate = today.minusDays(27),
                dueDate = today.plusDays(1),
                renewalCount = 1,
                maxRenewals = 2
            ),
            Loan(
                id = "loan_active_3",
                bookId = "book_3",
                borrowDate = today.minusDays(32),
                dueDate = today.minusDays(2), // Overdue!
                renewalCount = 2, // Max renewals reached
                maxRenewals = 2
            ),
            Loan(
                id = "loan_active_4",
                bookId = "book_4",
                borrowDate = today.minusDays(7),
                dueDate = today.plusDays(14),
                renewalCount = 0,
                maxRenewals = 2
            ),
            // History loans (books read before)
            Loan(
                id = "loan_hist_1",
                bookId = "book_5",
                borrowDate = today.minusDays(90),
                dueDate = today.minusDays(69),
                returnDate = today.minusDays(71),
                renewalCount = 0,
                userRating = 5,
                userNotes = "Brilliant prose and evocative storytelling."
            ),
            Loan(
                id = "loan_hist_2",
                bookId = "book_6",
                borrowDate = today.minusDays(120),
                dueDate = today.minusDays(99),
                returnDate = today.minusDays(100),
                renewalCount = 0,
                userRating = 4,
                userNotes = "Chilling and timeless masterpiece."
            ),
            Loan(
                id = "loan_hist_3",
                bookId = "book_7",
                borrowDate = today.minusDays(160),
                dueDate = today.minusDays(139),
                returnDate = today.minusDays(138), // 1 day late
                renewalCount = 1,
                userRating = 5,
                userNotes = "Super practical guide for modern Android development."
            ),
            Loan(
                id = "loan_hist_4",
                bookId = "book_8",
                borrowDate = today.minusDays(210),
                dueDate = today.minusDays(189),
                returnDate = today.minusDays(195),
                renewalCount = 0,
                userRating = 5,
                userNotes = "Couldn't put it down. Amaze amaze amaze!"
            )
        )
    }
}
