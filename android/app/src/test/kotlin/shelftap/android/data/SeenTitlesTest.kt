package shelftap.android.data

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import org.junit.Test
import shelftap.android.api.ApiHarness

/** A book's page opens from what the list showed: the repository remembers listed books, and only those. */
class SeenTitlesTest {
    private fun page(vararg ids: String) =
        """{"items":[${ids.joinToString(",") { """{"titleId":"$it","isbn":"978$it","title":"Book $it","authors":["A"],"subjects":["S"],"copiesAvailable":2}""" }}],"page":0,"size":20,"total":${ids.size}}"""

    @Test
    fun aListedBookIsRememberedAndAnUnlistedOneIsNot(): Unit = ApiHarness().use { h ->
        runBlocking {
            assertNull(h.repo.seenTitle("t1"))
            h.enqueue(200, page("t1", "t2"))
            h.repo.titles("", 0)
            val seen = assertNotNull(h.repo.seenTitle("t1"))
            assertEquals("Book t1", seen.title)
            assertEquals(2, seen.copiesAvailable)
            assertNull(h.repo.seenTitle("t9"))
        }
    }

    @Test
    fun theMemoryIsBoundedAndKeepsTheNewest(): Unit = ApiHarness().use { h ->
        runBlocking {
            val ids = (1..300).map { "b$it" }
            ids.chunked(100).forEach { h.enqueue(200, page(*it.toTypedArray())); h.repo.titles("", 0) }
            assertNull(h.repo.seenTitle("b1"))
            assertNotNull(h.repo.seenTitle("b300"))
        }
    }
}
