package vn.shelftap.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchQueryTest {

    @Test
    fun blankInputHasNoMatchExpression() {
        assertNull(buildSearchMatch("   "))
    }

    @Test
    fun punctuationSplitsWords() {
        assertEquals("\"978*\" \"0*\"", buildSearchMatch("978-0"))
    }

    @Test
    fun everyWordBecomesAQuotedPrefixTerm() {
        assertEquals("\"Tol*\" \"ring*\"", buildSearchMatch("Tol ring"))
    }
}
