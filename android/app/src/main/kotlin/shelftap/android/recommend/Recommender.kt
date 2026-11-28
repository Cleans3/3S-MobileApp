package shelftap.android.recommend

import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/** What the scorer knows of one book: public catalog data only (no member data). */
data class BookInfo(
    val titleId: String,
    val title: String,
    val authors: List<String>,
    val subjects: List<String>,
    val copiesAvailable: Int,
    val coverUrl: String?,
)

/** A book the member has, has had or has reserved, and how much it counts: 1 for a book on loan or reserved, less for a returned one as time passes. */
data class Signal(val book: BookInfo, val weight: Double)

/** One thing a book can have in common with another: an author or a subject. */
sealed interface Feature {
    val name: String

    data class Author(override val name: String) : Feature
    data class Subject(override val name: String) : Feature
}

/** Why a book is suggested: the feature that counted most. The screen turns it into words ("Because you borrowed a book by …"). */
sealed interface Reason {
    data class SameAuthor(val author: String) : Reason
    data class SameSubject(val subject: String) : Reason
}

data class Recommendation(val book: BookInfo, val reason: Reason, val score: Double)

/**
 * Content-based suggestions (plan `search-recommend-plan.md` B2), pure and in memory: nothing here reads or writes the member's data
 * outside the arguments, so the profile lives only as long as the call.
 *
 * 1. The profile adds up, for each author and subject of the member's books, the weight of the book times the feature's rarity.
 * 2. Rarity is `ln(N / df)`: a subject on most of the catalog ("Văn học Việt Nam") counts little, a rare one counts a lot.
 * 3. A candidate scores the cosine between the profile and its own features.
 * 4. Books the member already has, had or reserved, and books turned down, are left out.
 * 5. The order breaks ties towards a book on the shelf, and no more than [MAX_PER_AUTHOR] of one author fit in the list.
 */
object Recommender {
    /** A returned book counts half as much every 90 days (the half-life the plan proposes; the evaluation in B5 is where it gets tuned). */
    const val HALF_LIFE_DAYS = 90.0
    const val MAX_PER_AUTHOR = 2
    const val DEFAULT_LIMIT = 10

    /** Scores closer than this are a tie, and a book on the shelf goes first. */
    private const val TIE = 0.01

    /** The weight of a returned book: 1 on the day it came back, halving every [HALF_LIFE_DAYS]. A date in the future counts as today. */
    fun recencyWeight(ageDays: Double): Double = 0.5.pow(max(0.0, ageDays) / HALF_LIFE_DAYS)

    fun featuresOf(book: BookInfo): List<Feature> =
        book.authors.filter { it.isNotBlank() }.distinct().map { Feature.Author(it) } +
            book.subjects.filter { it.isNotBlank() }.distinct().map { Feature.Subject(it) }

    /** `ln(N / df)`, never below [MIN_IDF], so a feature found on every book still counts a little; [DEFAULT_IDF] when the count is not known. */
    fun idf(catalogSize: Long, df: Long?): Double {
        if (df == null || df <= 0 || catalogSize <= 0) return DEFAULT_IDF
        return max(MIN_IDF, ln(catalogSize.toDouble() / df.toDouble()))
    }

    const val MIN_IDF = 0.1
    const val DEFAULT_IDF = 1.0

    /** The member's taste: each feature of the signals, weighted by how much the books count and how rare the feature is. */
    fun profile(signals: List<Signal>, idf: (Feature) -> Double): Map<Feature, Double> {
        val raw = HashMap<Feature, Double>()
        for (s in signals) for (f in featuresOf(s.book)) raw[f] = (raw[f] ?: 0.0) + s.weight
        return raw.mapValues { (f, w) -> w * idf(f) }
    }

    fun recommend(
        signals: List<Signal>,
        candidates: Collection<BookInfo>,
        excluded: Set<String>,
        idf: (Feature) -> Double,
        limit: Int = DEFAULT_LIMIT,
    ): List<Recommendation> {
        val profile = profile(signals, idf)
        val norm = sqrt(profile.values.sumOf { it * it })
        if (profile.isEmpty() || norm == 0.0) return emptyList()
        val skip = excluded + signals.map { it.book.titleId }

        val scored = candidates.asSequence().distinctBy { it.titleId }.filter { it.titleId !in skip }.mapNotNull { book ->
            val features = featuresOf(book)
            if (features.isEmpty()) return@mapNotNull null
            var dot = 0.0
            var bookNorm = 0.0
            var best: Feature? = null
            var bestPart = 0.0
            for (f in features) {
                val v = idf(f)
                bookNorm += v * v
                val part = (profile[f] ?: 0.0) * v
                dot += part
                if (part > bestPart) { bestPart = part; best = f }
            }
            if (best == null || dot <= 0.0) return@mapNotNull null
            Recommendation(book, reasonOf(best), dot / (norm * sqrt(bookNorm)))
        }.toList()

        val ordered = scored.sortedWith(
            compareByDescending<Recommendation> { Math.round(it.score / TIE) }
                .thenByDescending { it.book.copiesAvailable > 0 }
                .thenByDescending { it.score }
                .thenBy { it.book.title },
        )
        // No more than MAX_PER_AUTHOR of one first author; a book with no author is not limited.
        val perAuthor = HashMap<String, Int>()
        val out = ArrayList<Recommendation>(limit)
        for (r in ordered) {
            val author = r.book.authors.firstOrNull()
            if (author != null) {
                val n = perAuthor[author] ?: 0
                if (n >= MAX_PER_AUTHOR) continue
                perAuthor[author] = n + 1
            }
            out += r
            if (out.size == limit) break
        }
        return out
    }

    private fun reasonOf(f: Feature): Reason = when (f) {
        is Feature.Author -> Reason.SameAuthor(f.name)
        is Feature.Subject -> Reason.SameSubject(f.name)
    }
}
