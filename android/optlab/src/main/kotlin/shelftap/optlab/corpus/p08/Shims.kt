package shelftap.optlab.corpus.p08

import androidx.compose.ui.graphics.Color

// Symbols BookCover.kt and BookCard.kt import from the generated app (agy-g38flash-B1-naive-r1): model/Book.kt and ui/theme/Color.kt, copied unchanged.

data class Book(
    val id: Long,
    val title: String,
    val author: String,
    val isbn: String,
    val genre: String,
    val publicationYear: Int,
    val pageCount: Int,
    val description: String,
    val shelfLocation: String,
    val isAvailable: Boolean,
    val coverColorSeed: Long,
    val coverImageUrl: String? = null
)

val AvailableGreen = Color(0xFF2E7D32)
val UnavailableRed = Color(0xFFC62828)

// Cover Palette Sets for Procedural Covers
val CoverPalettes = listOf(
    Pair(Color(0xFF2C3E50), Color(0xFFBDC3C7)), // Slate & Silver
    Pair(Color(0xFF1E3A8A), Color(0xFFFDE68A)), // Deep Navy & Warm Gold
    Pair(Color(0xFF064E3B), Color(0xFFA7F3D0)), // Forest Green & Mint
    Pair(Color(0xFF7C2D12), Color(0xFFFED7AA)), // Mahogany & Apricot
    Pair(Color(0xFF4C1D95), Color(0xFFE9D5FF)), // Royal Purple & Lilac
    Pair(Color(0xFF831843), Color(0xFFFCE7F3)), // Burgundy & Rose
    Pair(Color(0xFF134E4A), Color(0xFF99F6E4)), // Deep Teal & Aqua
    Pair(Color(0xFF312E81), Color(0xFFC7D2FE)), // Indigo & Periwinkle
    Pair(Color(0xFF451A03), Color(0xFFFEF3C7)), // Antique Leather & Cream
    Pair(Color(0xFF374151), Color(0xFFF3F4F6))  // Charcoal & Cloud
)
