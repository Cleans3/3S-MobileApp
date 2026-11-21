package com.example.libraryloans.data

import com.example.libraryloans.model.Book
import com.example.libraryloans.model.Loan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

class LoanRepository {

    private val initialBooks = listOf(
        Book(
            id = "b1",
            title = "Project Hail Mary",
            author = "Andy Weir",
            isbn = "978-0593135204",
            genre = "Science Fiction",
            publishYear = 2021,
            pageCount = 496,
            publisher = "Ballantine Books",
            callNumber = "FIC WEI 2021",
            synopsis = "Ryland Grace is the sole survivor on a desperate, last-chance mission—and if he fails, humanity and the earth itself are doomed. Except right now, he doesn't know that. He can't even remember his own name, let alone the nature of his assignment or how to complete it.",
            coverAccentHex = 0xFF1E3A8A
        ),
        Book(
            id = "b2",
            title = "Klara and the Sun",
            author = "Kazuo Ishiguro",
            isbn = "978-0593318171",
            genre = "Literary Fiction",
            publishYear = 2021,
            pageCount = 320,
            publisher = "Knopf",
            callNumber = "FIC ISH 2021",
            synopsis = "From the bestselling and Nobel Prize-winning author, a magnificent novel that asks: What does it mean to love? Klara, an Artificial Friend with outstanding observational qualities, watches carefully the behavior of those who come in to browse.",
            coverAccentHex = 0xFFB45309
        ),
        Book(
            id = "b3",
            title = "Designing Data-Intensive Applications",
            author = "Martin Kleppmann",
            isbn = "978-1449373320",
            genre = "Computer Science",
            publishYear = 2017,
            pageCount = 616,
            publisher = "O'Reilly Media",
            callNumber = "QA76.9.D3 K54",
            synopsis = "Data is at the center of many challenges in system design today. Difficult issues need to be figured out, such as scalability, consistency, reliability, efficiency, and maintainability. In this practical and comprehensive guide, Martin Kleppmann helps you navigate this diverse landscape.",
            coverAccentHex = 0xFF047857
        ),
        Book(
            id = "b4",
            title = "Atomic Habits",
            author = "James Clear",
            isbn = "978-0735211292",
            genre = "Self-Help / Psychology",
            publishYear = 2018,
            pageCount = 320,
            publisher = "Avery",
            callNumber = "BF637.H3 C54",
            synopsis = "No matter your goals, Atomic Habits offers a proven framework for improving--every day. James Clear, one of the world's leading experts on habit formation, reveals practical strategies that will teach you exactly how to form good habits, break bad ones, and master tiny behaviors that lead to remarkable results.",
            coverAccentHex = 0xFF991B1B
        ),
        Book(
            id = "b5",
            title = "Dune",
            author = "Frank Herbert",
            isbn = "978-0441172719",
            genre = "Science Fiction",
            publishYear = 1965,
            pageCount = 688,
            publisher = "Chilton Books",
            callNumber = "FIC HER 1965",
            synopsis = "Set on the desert planet Arrakis, Dune is the story of the boy Paul Atreides, heir to a noble family tasked with ruling an inhospitable world where the only thing of value is the 'spice' melange, a drug capable of extending life and enhancing consciousness.",
            coverAccentHex = 0xFFD97706
        ),
        Book(
            id = "b6",
            title = "The Silent Patient",
            author = "Alex Michaelides",
            isbn = "978-1250301696",
            genre = "Psychological Thriller",
            publishYear = 2019,
            pageCount = 336,
            publisher = "Celadon Books",
            callNumber = "FIC MIC 2019",
            synopsis = "Alicia Berenson's life is seemingly perfect. A famous painter married to an in-demand fashion photographer, she lives in a grand house overlooking a park in one of London's most desirable areas. One evening, she shoots her husband five times in the face and never speaks another word.",
            coverAccentHex = 0xFF475569
        ),
        Book(
            id = "b7",
            title = "Thinking, Fast and Slow",
            author = "Daniel Kahneman",
            isbn = "978-0374533557",
            genre = "Cognitive Psychology",
            publishYear = 2011,
            pageCount = 512,
            publisher = "Farrar, Straus and Giroux",
            callNumber = "BF441 .K238",
            synopsis = "In the international bestseller, Daniel Kahneman, the renowned psychologist and winner of the Nobel Prize in Economics, takes us on a groundbreaking tour of the mind and explains the two systems that drive the way we think.",
            coverAccentHex = 0xFF4338CA
        )
    )

    private val today = LocalDate.now()

    private val _loans = MutableStateFlow<List<Loan>>(
        listOf(
            Loan(
                id = "loan-001",
                book = initialBooks[0], // Project Hail Mary
                borrowDate = today.minusDays(10),
                dueDate = today.plusDays(11),
                renewalCount = 0,
                maxRenewals = 2
            ),
            Loan(
                id = "loan-002",
                book = initialBooks[1], // Klara and the Sun
                borrowDate = today.minusDays(18),
                dueDate = today.plusDays(3), // Due soon
                renewalCount = 1,
                maxRenewals = 2
            ),
            Loan(
                id = "loan-003",
                book = initialBooks[2], // Designing Data-Intensive Applications
                borrowDate = today.minusDays(25),
                dueDate = today.plusDays(17),
                renewalCount = 1,
                maxRenewals = 2
            ),
            // Returned loans (history)
            Loan(
                id = "loan-004",
                book = initialBooks[3], // Atomic Habits
                borrowDate = today.minusDays(90),
                dueDate = today.minusDays(69),
                returnDate = today.minusDays(70),
                renewalCount = 0
            ),
            Loan(
                id = "loan-005",
                book = initialBooks[4], // Dune
                borrowDate = today.minusDays(60),
                dueDate = today.minusDays(39),
                returnDate = today.minusDays(41),
                renewalCount = 1
            ),
            Loan(
                id = "loan-006",
                book = initialBooks[5], // The Silent Patient
                borrowDate = today.minusDays(45),
                dueDate = today.minusDays(24),
                returnDate = today.minusDays(25),
                renewalCount = 0
            ),
            Loan(
                id = "loan-007",
                book = initialBooks[6], // Thinking, Fast and Slow
                borrowDate = today.minusDays(120),
                dueDate = today.minusDays(99),
                returnDate = today.minusDays(98),
                renewalCount = 1
            )
        )
    )

    val allLoans: StateFlow<List<Loan>> = _loans.asStateFlow()

    fun renewLoan(loanId: String): Result<Loan> {
        var renewedLoan: Loan? = null
        var errorMessage: String? = null

        _loans.update { currentList ->
            currentList.map { loan ->
                if (loan.id == loanId) {
                    if (loan.isReturned) {
                        errorMessage = "Cannot renew a book that has already been returned."
                        loan
                    } else if (loan.renewalCount >= loan.maxRenewals) {
                        errorMessage = "Maximum renewals reached (${loan.maxRenewals} times)."
                        loan
                    } else {
                        val extendedDueDate = loan.dueDate.plusDays(14)
                        val updated = loan.copy(
                            dueDate = extendedDueDate,
                            renewalCount = loan.renewalCount + 1
                        )
                        renewedLoan = updated
                        updated
                    }
                } else {
                    loan
                }
            }
        }

        return if (renewedLoan != null) {
            Result.success(renewedLoan!!)
        } else {
            Result.failure(Exception(errorMessage ?: "Loan not found"))
        }
    }
}
