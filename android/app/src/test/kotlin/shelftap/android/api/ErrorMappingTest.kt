package shelftap.android.api

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ErrorMappingTest {
    private fun harness(block: suspend ApiHarness.() -> Unit) = ApiHarness().use { h -> runBlocking { h.block() } }

    @Test
    fun everyDocumentedCodeMapsAndNothingElseDoes() {
        val documented = """VALIDATION_FAILED INVALID_CREDENTIALS ACCOUNT_LOCKED TOKEN_INVALID TOKEN_REUSED PASSWORD_CHANGE_REQUIRED
            FORBIDDEN NOT_FOUND CARD_NOT_ACTIVE CARD_AUTH_FAILED CHALLENGE_EXPIRED COPY_NOT_AVAILABLE LOAN_LIMIT_REACHED UNPAID_FINE
            RENEWAL_NOT_ALLOWED RESERVED_BY_OTHER ALREADY_RESERVED RATE_LIMITED STEP_UP_REQUIRED KEY_ALREADY_REGISTERED""".split(Regex("\\s+"))
        documented.forEach { assertEquals(it, ErrorCode.parse(it).name) }
        assertEquals(ErrorCode.UNKNOWN, ErrorCode.parse("SOMETHING_NEW"))
        assertEquals(ErrorCode.UNKNOWN, ErrorCode.parse(null))
        assertEquals(documented.size + 1, ErrorCode.entries.size)
    }

    @Test
    fun selfCheckoutShowsTheServersAnswerByCodeAndNeverByDetail() = harness {
        enqueueProblem(409, "COPY_NOT_AVAILABLE")
        val e = assertFailsWith<ApiException.Http> { repo.checkout("E004010000000001") }
        assertEquals(409, e.status)
        assertEquals(ErrorCode.COPY_NOT_AVAILABLE, e.code)
        assertFalse(e.message!!.contains("secret detail"))
        assertEquals("""{"tagId":"E004010000000001"}""", server.takeRequest().body!!.utf8())
    }

    @Test
    fun rateLimitedCarriesRetryAfter() = harness {
        enqueueProblem(429, "RATE_LIMITED", "Retry-After" to "30")
        val e = assertFailsWith<ApiException.Http> { repo.titles("dune", 0) }
        assertEquals(ErrorCode.RATE_LIMITED, e.code)
        assertEquals(30L, e.retryAfterSeconds)
    }

    @Test
    fun aBodyThatIsNotProblemJsonIsUnknown() = harness {
        enqueue(502, "<html>bad gateway</html>")
        assertEquals(ErrorCode.UNKNOWN, assertFailsWith<ApiException.Http> { repo.loans() }.code)
    }

    @Test
    fun catalogCallsUseTheDocumentedQueryAndParseThePage() = harness {
        enqueue(
            200,
            """{"items":[{"titleId":"t1","isbn":"978","title":"Dune","authors":["Herbert"],"subjects":[],"coverUrl":null,"copiesAvailable":2}],"page":0,"size":20,"total":1,"extra":"ignored"}""",
        )
        val page = repo.titles("dune", 0)
        assertEquals("Dune", page.items.single().title)
        assertEquals(1L, page.total)
        val url = server.takeRequest().url
        assertEquals("/api/titles", url.encodedPath)
        assertEquals("q=dune&page=0&size=20", url.encodedQuery)
    }

    @Test
    fun anUnreachableServerIsANetworkError() {
        val h = ApiHarness()
        h.server.close()
        runBlocking { assertFailsWith<ApiException.Network> { h.repo.loans() } }
    }
}
