package shelftap.optlab.data

import androidx.compose.runtime.Immutable
import kotlin.random.Random

@Immutable
data class Book(val id: Int, val title: String, val author: String, val isbn: String, val year: Int, val coverIdx: Int)

/** Deterministic catalog: book(i) is a pure function of i, so every run and every variant sees the same data. */
object BookGenerator {
    const val TOTAL = 100_000
    const val COVERS = 64

    private val adjectives = listOf(
        "Silent", "Hidden", "Golden", "Broken", "Distant", "Crimson", "Forgotten", "Hollow", "Electric", "Quiet",
        "Midnight", "Ancient", "Restless", "Burning", "Paper", "Velvet", "Iron", "Glass", "Wandering", "Secret",
        "Northern", "Salt", "Winter", "Savage", "Gentle", "Lucky", "Bitter", "Pale", "Wild", "Lonely",
    )
    private val nouns = listOf(
        "River", "Garden", "Library", "Empire", "Compass", "Lantern", "Harbor", "Orchard", "Algorithm", "Machine",
        "Cathedral", "Voyage", "Archive", "Mirror", "Kingdom", "Letters", "Atlas", "Orchestra", "Theorem", "Island",
        "Bridge", "Window", "Horizon", "Fortress", "Ledger", "Whisper", "Meadow", "Signal", "Circuit", "Satellite",
        "Chronicle", "Manuscript", "Labyrinth", "Puzzle", "Engine", "Province", "Lighthouse", "Station", "Quarry", "Canyon",
    )
    private val places = listOf(
        "Prague", "Hanoi", "Lisbon", "Kyoto", "Nairobi", "Oslo", "Quito", "Cairo", "Lima", "Dublin",
        "Riga", "Hue", "Seoul", "Vienna", "Tbilisi", "Accra", "Bergen", "Porto", "Yangon", "Zagreb",
    )
    private val firstNames = listOf(
        "Anh", "Mai", "Lan", "Minh", "Elena", "Marcus", "Sofia", "Tariq", "Ingrid", "Kenji",
        "Amara", "Luca", "Noor", "Pavel", "Chloe", "Diego", "Hana", "Omar", "Freya", "Viktor",
    )
    private val lastNames = listOf(
        "Nguyen", "Tran", "Okafor", "Lindqvist", "Moreau", "Haddad", "Kowalski", "Tanaka", "Silva", "Petrov",
        "Mbeki", "Rossi", "Andersen", "Yilmaz", "Castillo", "Novak", "Fischer", "Park", "Duarte", "Ivanov",
    )

    fun book(id: Int): Book {
        val r = Random(id.toLong() * 0x9E3779B1L + 17)
        val title = when (r.nextInt(3)) {
            0 -> "The ${adjectives.pick(r)} ${nouns.pick(r)}"
            1 -> "${nouns.pick(r)} of ${places.pick(r)}"
            else -> "${adjectives.pick(r)} ${nouns.pick(r)} of ${places.pick(r)}"
        }
        val author = "${firstNames.pick(r)} ${lastNames.pick(r)}"
        val isbn = "978" + (0 until 10).joinToString("") { r.nextInt(10).toString() }
        return Book(id, title, author, isbn, 1950 + r.nextInt(75), id % COVERS)
    }

    fun all(n: Int = TOTAL): List<Book> = List(n) { book(it) }

    private fun List<String>.pick(r: Random) = this[r.nextInt(size)]
}
