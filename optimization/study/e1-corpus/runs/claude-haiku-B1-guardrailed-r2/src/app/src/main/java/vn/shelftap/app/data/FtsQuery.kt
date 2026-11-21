package vn.shelftap.app.data

/**
 * Turns what the member typed into an FTS4 MATCH expression.
 *
 * Every word becomes a prefix term, and all words must match ("harry pot" finds "Harry Potter").
 * Punctuation is dropped, so "978-0-306" searches the ISBN digits. Quoting keeps user input
 * from being read as FTS operators such as NOT or NEAR.
 */
object FtsQuery {
    private val separators = Regex("[^\\p{L}\\p{N}]+")

    /** Returns null when the input has no searchable word. */
    fun build(input: String): String? {
        val words = input.split(separators).filter { it.isNotEmpty() }
        if (words.isEmpty()) return null
        return words.joinToString(separator = " ") { "\"$it*\"" }
    }
}
