package com.library.catalogue.data.generator

import android.database.sqlite.SQLiteDatabase
import com.library.catalogue.data.local.LibraryDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.Random

class BookDataSeeder(private val database: LibraryDatabase) {

    private val _seedingProgress = MutableStateFlow(0f)
    val seedingProgress: Flow<Float> = _seedingProgress.asStateFlow()

    private val genres = listOf(
        "Fiction", "Science Fiction", "Fantasy", "Mystery", "Thriller",
        "Historical Fiction", "Romance", "Biography", "History",
        "Science & Tech", "Philosophy", "Psychology", "Poetry",
        "Self-Help", "Business", "Art & Design"
    )

    private val titlePrefixes = listOf(
        "The Secret of", "Chronicles of", "The Art of", "The Journey to",
        "Beyond the", "Echoes of", "Shadows over", "A Guide to", "The Last",
        "Tales from", "The Lost", "Whispers in the", "Rise of the", "The Song of",
        "Memories of", "Legacy of", "Mastering", "The Mystery of", "Kingdom of",
        "Dance of", "Principles of", "The Promise of", "Under the", "Return to",
        "The Age of", "In Search of", "A Brief History of", "Wonders of"
    )

    private val titleNouns = listOf(
        "Stars", "Time", "Tomorrow", "Eternity", "Silence", "Darkness", "Light",
        "Wind", "Fire", "Ocean", "Kingdom", "Empire", "Labyrinth", "Galaxy",
        "Memory", "Echo", "Winter", "Summer", "Autumn", "Spring", "Destiny",
        "Dreams", "Shadows", "Midnight", "Dawn", "Cosmos", "Mind", "Heart",
        "World", "Future", "Past", "Universe", "Horizon", "Infinity", "Truth"
    )

    private val firstNames = listOf(
        "James", "Mary", "Robert", "Patricia", "John", "Jennifer", "Michael", "Linda",
        "William", "Elizabeth", "David", "Barbara", "Richard", "Susan", "Joseph", "Jessica",
        "Thomas", "Sarah", "Charles", "Karen", "Christopher", "Nancy", "Daniel", "Margaret",
        "Arthur", "Eleanor", "Oliver", "Charlotte", "Alexander", "Sophia", "Henry", "Emma",
        "Julian", "Clara", "Victor", "Aria", "Sebastian", "Isabella", "Gabriel", "Elena"
    )

    private val lastNames = listOf(
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
        "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson",
        "Thomas", "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson",
        "White", "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson", "Walker",
        "Young", "Allen", "King", "Wright", "Scott", "Torres", "Nguyen", "Hill", "Flores"
    )

    private val paletteColors = listOf(
        "#1E3A8A", "#1E40AF", "#0369A1", "#0E7490", "#047857",
        "#15803D", "#4D7C0F", "#A16207", "#C2410C", "#B91C1C",
        "#BE185D", "#6D28D9", "#4338CA", "#374151", "#18181B"
    )

    suspend fun seedDatabaseIfNeeded(targetCount: Int = 100_000) = withContext(Dispatchers.IO) {
        val currentCount = database.bookDao().getBookCount()
        if (currentCount >= targetCount) {
            _seedingProgress.value = 1.0f
            return@withContext
        }

        val remaining = targetCount - currentCount
        val openHelper = database.openHelper
        val db = openHelper.writableDatabase

        val batchSize = 2_500
        val random = Random(42)

        val sql = """
            INSERT INTO books (
                title, author, isbn, genre, publicationYear, 
                description, coverUrl, coverColorHex, totalCopies, 
                availableCopies, isAvailable, rating, pages
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        var insertedSoFar = currentCount
        val totalBatches = (remaining + batchSize - 1) / batchSize

        for (batch in 0 until totalBatches) {
            val countInBatch = minOf(batchSize, targetCount - insertedSoFar)
            db.beginTransactionNonExclusive()
            try {
                val stmt = db.compileStatement(sql)
                for (i in 0 until countInBatch) {
                    val globalIdx = insertedSoFar + i + 1
                    val prefix = titlePrefixes[random.nextInt(titlePrefixes.size)]
                    val noun = titleNouns[random.nextInt(titleNouns.size)]
                    val suffixNum = if (globalIdx > 1000) " Vol. ${ (globalIdx % 99) + 1 }" else ""
                    val title = "$prefix $noun$suffixNum"

                    val author = "${firstNames[random.nextInt(firstNames.size)]} ${lastNames[random.nextInt(lastNames.size)]}"
                    val genre = genres[random.nextInt(genres.size)]
                    val year = 1920 + random.nextInt(105)
                    val isbn = String.format("978-%02d-%05d-%03d-%01d", 
                        random.nextInt(100), 
                        random.nextInt(100000), 
                        random.nextInt(1000), 
                        random.nextInt(10)
                    )
                    val total = 1 + random.nextInt(8)
                    val available = random.nextInt(total + 1)
                    val rating = 3.0f + (random.nextFloat() * 2.0f)
                    val pages = 120 + random.nextInt(780)
                    val color = paletteColors[random.nextInt(paletteColors.size)]
                    val desc = "An extraordinary $genre work written by $author, delving into themes of $noun and the human experience. Published in $year."
                    val coverUrl = "https://covers.openlibrary.org/b/isbn/$isbn-M.jpg"

                    stmt.bindString(1, title)
                    stmt.bindString(2, author)
                    stmt.bindString(3, isbn)
                    stmt.bindString(4, genre)
                    stmt.bindLong(5, year.toLong())
                    stmt.bindString(6, desc)
                    stmt.bindString(7, coverUrl)
                    stmt.bindString(8, color)
                    stmt.bindLong(9, total.toLong())
                    stmt.bindLong(10, available.toLong())
                    stmt.bindLong(11, if (available > 0) 1L else 0L)
                    stmt.bindDouble(12, rating.toDouble())
                    stmt.bindLong(13, pages.toLong())
                    stmt.executeInsert()
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }

            insertedSoFar += countInBatch
            _seedingProgress.value = insertedSoFar.toFloat() / targetCount.toFloat()
        }
        _seedingProgress.value = 1.0f
    }
}
