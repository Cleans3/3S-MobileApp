package com.example.librarycatalogue.data.local

import android.util.Log
import com.example.librarycatalogue.model.Genres
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Random

/**
 * High-performance seeder for generating and seeding up to 100,000 realistic books
 * into the SQLite database using bulk transactions.
 */
object BookDatabaseSeeder {

    private const val TAG = "BookDatabaseSeeder"
    const val TARGET_BOOK_COUNT = 100_000
    private const val BATCH_SIZE = 2_500

    private val TITLE_ADJECTIVES = listOf(
        "Silent", "Ancient", "Infinite", "Lost", "Forgotten", "Secret", "Hidden", "Eternal",
        "Celestial", "Digital", "Quantum", "Golden", "Crimson", "Shadowed", "Invisible",
        "Brave", "Luminous", "Dark", "Radiant", "Iron", "Echoing", "Shattered", "Uncharted",
        "Wandering", "Midnight", "Brilliant", "Fading", "Parallel", "Spectral", "Mythic"
    )

    private val TITLE_NOUNS = listOf(
        "Echoes", "Chronicles", "Horizon", "Empire", "Odyssey", "Symphony", "Paradox",
        "Legacy", "Whispers", "Journey", "Realm", "Tides", "Labyrinth", "Prophecy",
        "Voyage", "Canvas", "Constellation", "Algorithm", "Passage", "Beacon", "Mirror",
        "Fragments", "Illusion", "Frontier", "Monolith", "Sanctuary", "Vortex", "Tapestry"
    )

    private val TITLE_TOPICS = listOf(
        "of Time", "of the Universe", "of Tomorrow", "of the Mind", "of Starlight",
        "in the Shadows", "of Civilization", "Beyond the Sea", "of Reason", "and Chaos",
        "under the Sun", "of Destiny", "Across the Cosmos", "of Human Nature", "in the Void",
        "of Wisdom", "of Dreams", "and Solitude", "of the Earth", "of Eternity"
    )

    private val FIRST_NAMES = listOf(
        "Arthur", "Eleanor", "Marcus", "Helena", "Julian", "Clara", "Victor", "Sophia",
        "Alexander", "Beatrice", "Gabriel", "Iris", "Sebastian", "Valerie", "Felix",
        "Cordelia", "Theodore", "Genevieve", "Oliver", "Penelope", "Lucas", "Astrid",
        "Nathaniel", "Rosalind", "Dante", "Seraphina", "Jasper", "Evangeline", "Leo", "Diana"
    )

    private val LAST_NAMES = listOf(
        "Sterling", "Vance", "Blackwood", "Holloway", "Mercer", "Fairchild", "Hawthorne",
        "Pendleton", "Sinclair", "Montgomery", "Winter", "Carrington", "Ashford", "Kingsley",
        "Rutherford", "Ellington", "Donovan", "Castellan", "Morrison", "Bexley", "Vanderbilt",
        "Lovelace", "Somerset", "Prescott", "Waverly", "Aldridge", "Kensington", "North"
    )

    private val SHELF_SECTIONS = listOf("A", "B", "C", "D", "E", "F", "G", "H", "J", "K")

    suspend fun seedIfNeeded(database: LibraryDatabase, onProgress: (Int, Int) -> Unit = { _, _ -> }) {
        withContext(Dispatchers.IO) {
            val dao = database.bookDao()
            val existingCount = dao.getBookCount()

            if (existingCount >= TARGET_BOOK_COUNT) {
                Log.d(TAG, "Database already populated with $existingCount books.")
                onProgress(existingCount, TARGET_BOOK_COUNT)
                return@withContext
            }

            Log.d(TAG, "Seeding library database to $TARGET_BOOK_COUNT books...")
            val random = Random(42) // Deterministic seed for consistent catalogue
            val genresList = Genres.ALL.filter { it != "All Genres" }

            var currentCount = existingCount
            val batch = ArrayList<BookEntity>(BATCH_SIZE)

            while (currentCount < TARGET_BOOK_COUNT) {
                val booksToGenerate = minOf(BATCH_SIZE, TARGET_BOOK_COUNT - currentCount)
                batch.clear()

                for (i in 0 until booksToGenerate) {
                    val bookIndex = currentCount + i + 1
                    val genre = genresList[random.nextInt(genresList.size)]
                    
                    val adj = TITLE_ADJECTIVES[random.nextInt(TITLE_ADJECTIVES.size)]
                    val noun = TITLE_NOUNS[random.nextInt(TITLE_NOUNS.size)]
                    val topic = TITLE_TOPICS[random.nextInt(TITLE_TOPICS.size)]
                    val title = if (random.nextBoolean()) "$adj $noun" else "$noun $topic"

                    val firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.size)]
                    val lastName = LAST_NAMES[random.nextInt(LAST_NAMES.size)]
                    val author = "$firstName $lastName"

                    val isbn = String.format(
                        "978-%02d-%04d-%03d-%d",
                        random.nextInt(90) + 10,
                        random.nextInt(9000) + 1000,
                        random.nextInt(900) + 100,
                        random.nextInt(10)
                    )

                    val publicationYear = 1880 + random.nextInt(145)
                    val pageCount = 120 + random.nextInt(780)
                    val shelfSection = SHELF_SECTIONS[random.nextInt(SHELF_SECTIONS.size)]
                    val shelfRow = random.nextInt(12) + 1
                    val shelfCol = random.nextInt(20) + 1
                    val shelfLocation = "Floor ${(shelfRow % 3) + 1} • Bay $shelfSection-$shelfRow, Shelf $shelfCol"
                    val isAvailable = random.nextDouble() > 0.18 // ~82% available

                    val description = generateDescription(title, author, genre, publicationYear)
                    val coverColorSeed = (bookIndex * 2654435761L) xor (genre.hashCode().toLong())

                    batch.add(
                        BookEntity(
                            title = title,
                            author = author,
                            isbn = isbn,
                            genre = genre,
                            publicationYear = publicationYear,
                            pageCount = pageCount,
                            description = description,
                            shelfLocation = shelfLocation,
                            isAvailable = isAvailable,
                            coverColorSeed = coverColorSeed
                        )
                    )
                }

                database.runInTransaction {
                    dao.insertAll(batch)
                }

                currentCount += booksToGenerate
                onProgress(currentCount, TARGET_BOOK_COUNT)
            }

            Log.d(TAG, "Completed seeding $TARGET_BOOK_COUNT books.")
        }
    }

    private fun generateDescription(title: String, author: String, genre: String, year: Int): String {
        return "Published in $year, \"$title\" stands as an influential work of $genre by $author. " +
                "The text explores comprehensive perspectives, narrative depth, and thematic elegance, " +
                "offering both scholars and casual readers profound insight into its subject matter."
    }
}
