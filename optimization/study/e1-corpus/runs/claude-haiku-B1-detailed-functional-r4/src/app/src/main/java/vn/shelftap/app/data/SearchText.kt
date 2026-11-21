package vn.shelftap.app.data

import java.text.Normalizer
import java.util.Locale

/** Normalisation shared by the stored search column and the typed query. */
object SearchText {

    private val combiningMarks = Regex("\\p{Mn}+")

    /** Lower-cases, strips diacritics (so "Nguyen" matches "Nguyễn") and maps đ to d. */
    fun normalize(text: String): String {
        val lower = text.lowercase(Locale.ROOT)
        val decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD)
        return combiningMarks.replace(decomposed, "").replace('đ', 'd')
    }

    /**
     * Builds a LIKE pattern that matches anywhere in the stored text. The wildcard characters
     * typed by the member are escaped with '!', which the DAO declares as the ESCAPE character.
     */
    fun likePattern(query: String): String {
        val escaped = normalize(query.trim())
            .replace("!", "!!")
            .replace("%", "!%")
            .replace("_", "!_")
        return "%$escaped%"
    }
}
