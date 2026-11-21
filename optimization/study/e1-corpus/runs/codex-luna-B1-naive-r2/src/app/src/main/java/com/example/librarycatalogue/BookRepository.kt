package com.example.librarycatalogue

/** A page-oriented source: a real implementation can replace page() with a database/API call. */
class BookRepository {
    val totalCount = 100_000
    private val titles = listOf(
        "The Quiet Atlas" to "Mara Ellis", "A Field Guide to Wonder" to "Jon Bell", "Tomorrow's Garden" to "Nia Carter",
        "The Cartographer's Room" to "Lena Ortiz", "Small Things Like These" to "Claire Keegan", "The Long Way Home" to "Peter Grant",
        "Letters from the Sea" to "Iris Monroe", "A History of Rain" to "David Park", "The Art of Noticing" to "Rob Walker",
        "Night Train to Lisbon" to "Pascal Mercier", "The Book of Delights" to "Ross Gay", "The Hidden Life of Trees" to "Peter Wohlleben"
    )
    fun page(offset: Int, limit: Int, query: String = ""): List<Book> {
        val q = query.trim().lowercase()
        val source = (0 until totalCount).asSequence()
        return source.map { id ->
            val base = titles[id % titles.size]
            Book(id, if (id < titles.size) base.first else "${base.first} · Volume ${id / titles.size + 1}", base.second, 2024 - (id % 35), "978-0-${1000000 + id}", id % 5)
        }.filter { q.isEmpty() || "${it.title} ${it.author} ${it.isbn}".lowercase().contains(q) }
            .drop(offset).take(limit).toList()
    }
}
