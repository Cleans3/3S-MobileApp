package vn.shelftap.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FtsQueryTest {

    @Test
    fun wordsBecomeAndedPrefixTerms() {
        assertEquals("978* 0*", ftsMatchExpression("978-0"))
    }

    @Test
    fun operatorWordsAreLowercasedSoTheyAreNotOperators() {
        assertEquals("and* or*", ftsMatchExpression("AND OR"))
    }

    @Test
    fun punctuationOnlyQueryHasNoMatchExpression() {
        assertNull(ftsMatchExpression("--"))
    }
}
