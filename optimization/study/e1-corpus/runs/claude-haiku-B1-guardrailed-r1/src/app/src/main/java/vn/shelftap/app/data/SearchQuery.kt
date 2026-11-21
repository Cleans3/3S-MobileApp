package vn.shelftap.app.data

private val NON_TOKEN_CHARS = Regex("[^\\p{L}\\p{N}]+")

/**
 * Turns the text typed by the member into an FTS4 MATCH expression.
 * Each word becomes a quoted prefix term ("tol*") and the words are ANDed together.
 * Quoting keeps words such as AND, OR or NOT from being read as FTS operators.
 * Returns null when the input has no searchable word.
 */
fun buildSearchMatch(input: String): String? {
    val tokens = input.split(NON_TOKEN_CHARS).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return null
    return tokens.joinToString(separator = " ") { "\"$it*\"" }
}
