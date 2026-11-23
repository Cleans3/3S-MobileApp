package shelftap.optlab.corpus.p09

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Symbols BookDetailSheet.kt imports from the generated app (agy-g38flash-B3-naive-r1): model/Book.kt, model/Loan.kt, ui/theme/Color.kt and
// LoansViewModel.LoanWithBook, copied unchanged (Loan without the unused LoanStatus helper).

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val isbn: String,
    val publisher: String,
    val publishYear: Int,
    val genre: String,
    val pages: Int,
    val description: String,
    val coverAccentHex: Long = 0xFF1E3A8A,
    val shelfLocation: String = "Section B, Floor 2"
)

data class Loan(
    val id: String,
    val bookId: String,
    val borrowDate: LocalDate,
    val dueDate: LocalDate,
    val returnDate: LocalDate? = null,
    val renewalCount: Int = 0,
    val maxRenewals: Int = 2,
    val userRating: Int? = null, // 1 to 5 stars if returned and rated
    val userNotes: String? = null
) {
    val isReturned: Boolean
        get() = returnDate != null

    fun daysRemaining(today: LocalDate = LocalDate.now()): Long {
        return ChronoUnit.DAYS.between(today, dueDate)
    }

    val canRenew: Boolean
        get() = !isReturned && renewalCount < maxRenewals
}

data class LoanWithBook(
    val loan: Loan,
    val book: Book
)

val NavyPrimary = Color(0xFF1E3A8A)
val Slate100 = Color(0xFFF1F5F9)
val Slate600 = Color(0xFF475569)
val Slate800 = Color(0xFF1E293B)
val OverdueRed = Color(0xFFDC2626)
val ActiveGreen = Color(0xFF16A34A)

/** Stable parameter of the tuned B of P03+P04 (content equality, so an equal copy of the list lets the sheet skip). */
@Immutable
data class LoanList(val items: List<Loan>)

// material-icons-extended is not a dependency of the lab (it would swell the noR8 APK of P10): the two icons the file uses from it map to core icons.
val Icons.Filled.Autorenew: ImageVector get() = Icons.Filled.Refresh
val Icons.Filled.Book: ImageVector get() = Icons.Filled.Info
val Icons.Filled.History: ImageVector get() = Icons.Filled.DateRange
