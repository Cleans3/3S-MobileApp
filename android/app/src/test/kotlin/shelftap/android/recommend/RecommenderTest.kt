package shelftap.android.recommend

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** The scoring of plan B2, on books made up for the test: what is suggested, in which order, and why. */
class RecommenderTest {
    private fun book(id: String, authors: List<String> = emptyList(), subjects: List<String> = emptyList(), copies: Int = 1) =
        BookInfo(id, "Book $id", authors, subjects, copies, null)

    /** Every feature as rare as the next, so the tests show the effect of the weights and of nothing else. */
    private val flat: (Feature) -> Double = { 1.0 }

    @Test
    fun aBookByTheSameAuthorComesFirstAndSaysSo() {
        val has = Signal(book("a", listOf("Nam Cao"), listOf("Truyện ngắn")), 1.0)
        val out = Recommender.recommend(
            listOf(has),
            listOf(book("x", listOf("Nam Cao"), listOf("Truyện ngắn")), book("y", listOf("Other"), listOf("Truyện ngắn")), book("z", listOf("Other"), listOf("Toán"))),
            emptySet(), flat,
        )
        assertEquals(listOf("x", "y"), out.map { it.book.titleId })
        assertEquals(Reason.SameAuthor("Nam Cao"), out[0].reason)
        assertEquals(Reason.SameSubject("Truyện ngắn"), out[1].reason)
    }

    @Test
    fun booksTheMemberAlreadyHasOrTurnedDownAreLeftOut() {
        val has = Signal(book("a", listOf("A"), listOf("S")), 1.0)
        val candidates = listOf(book("a", listOf("A"), listOf("S")), book("b", listOf("A")), book("c", listOf("A")), book("d", listOf("A")))
        val out = Recommender.recommend(listOf(has), candidates, excluded = setOf("b"), idf = flat)
        assertEquals(listOf("c", "d"), out.map { it.book.titleId }.sorted())
    }

    @Test
    fun aSubjectOnAlmostTheWholeCatalogCountsLessThanARareOne() {
        val has = Signal(book("a", subjects = listOf("Common", "Rare")), 1.0)
        val common = book("c", subjects = listOf("Common"))
        val rare = book("r", subjects = listOf("Rare"))
        // 100 000 books; 90 000 are "Common", 50 are "Rare"
        val idf: (Feature) -> Double = { f -> Recommender.idf(100_000, if (f.name == "Common") 90_000 else 50) }
        val out = Recommender.recommend(listOf(has), listOf(common, rare), emptySet(), idf)
        assertEquals(listOf("r", "c"), out.map { it.book.titleId })
        assertTrue(out[0].score > out[1].score)
    }

    @Test
    fun aRecentBookCountsMoreThanAnOldOne() {
        val recent = Signal(book("a", subjects = listOf("New")), Recommender.recencyWeight(5.0))
        val old = Signal(book("b", subjects = listOf("Old")), Recommender.recencyWeight(400.0))
        val out = Recommender.recommend(listOf(recent, old), listOf(book("n", subjects = listOf("New")), book("o", subjects = listOf("Old"))), emptySet(), flat)
        assertEquals(listOf("n", "o"), out.map { it.book.titleId })
    }

    @Test
    fun theWeightHalvesEveryNinetyDaysAndNeverGrows() {
        assertEquals(1.0, Recommender.recencyWeight(0.0), 1e-9)
        assertEquals(0.5, Recommender.recencyWeight(90.0), 1e-9)
        assertEquals(0.25, Recommender.recencyWeight(180.0), 1e-9)
        assertEquals(1.0, Recommender.recencyWeight(-30.0), 1e-9) // a clock that is a little behind
    }

    @Test
    fun whenScoresAreAlmostEqualABookOnTheShelfGoesFirst() {
        val has = Signal(book("a", listOf("A")), 1.0)
        val out = Recommender.recommend(listOf(has), listOf(book("out", listOf("A"), copies = 0), book("in", listOf("A"), copies = 2)), emptySet(), flat)
        assertEquals(listOf("in", "out"), out.map { it.book.titleId })
    }

    @Test
    fun noMoreThanTwoBooksOfOneAuthorInTheList() {
        val has = Signal(book("a", listOf("Nam Cao"), listOf("S")), 1.0)
        val candidates = (1..5).map { book("n$it", listOf("Nam Cao"), listOf("S")) } + book("o1", listOf("Other"), listOf("S"))
        val out = Recommender.recommend(listOf(has), candidates, emptySet(), flat)
        assertEquals(2, out.count { it.book.authors.first() == "Nam Cao" })
        assertTrue(out.any { it.book.titleId == "o1" })
    }

    @Test
    fun theListHoldsAtMostTheLimit() {
        val has = Signal(book("a", subjects = listOf("S")), 1.0)
        val out = Recommender.recommend(listOf(has), (1..30).map { book("b$it", listOf("Author $it"), listOf("S")) }, emptySet(), flat, limit = 10)
        assertEquals(10, out.size)
    }

    @Test
    fun noSignalsOrNothingInCommonGivesNothing() {
        assertTrue(Recommender.recommend(emptyList(), listOf(book("x", listOf("A"))), emptySet(), flat).isEmpty())
        val out = Recommender.recommend(listOf(Signal(book("a", listOf("A")), 1.0)), listOf(book("x", listOf("B")), book("y")), emptySet(), flat)
        assertTrue(out.isEmpty())
    }

    @Test
    fun scoresAreCosinesSoABookCannotScoreAboveOne() {
        val has = Signal(book("a", listOf("A"), listOf("S")), 3.0)
        val out = Recommender.recommend(listOf(has), listOf(book("x", listOf("A"), listOf("S"))), emptySet(), flat)
        assertTrue(abs(out.single().score - 1.0) < 1e-9)
        assertIs<Reason.SameAuthor>(out.single().reason)
    }

    @Test
    fun theSameBookListedTwiceIsSuggestedOnce() {
        val has = Signal(book("a", listOf("A")), 1.0)
        val out = Recommender.recommend(listOf(has), listOf(book("x", listOf("A")), book("x", listOf("A"))), emptySet(), flat)
        assertEquals(1, out.size)
    }

    @Test
    fun theRarityOfAnUnknownCountIsNeutralAndNeverNegative() {
        assertEquals(Recommender.DEFAULT_IDF, Recommender.idf(1000, null), 0.0)
        assertEquals(Recommender.DEFAULT_IDF, Recommender.idf(0, 5), 0.0)
        assertEquals(Recommender.MIN_IDF, Recommender.idf(1000, 1000), 0.0) // on every book: still a little
        assertTrue(Recommender.idf(1000, 2000) >= Recommender.MIN_IDF) // a count larger than the catalog (a loose search)
    }
}
