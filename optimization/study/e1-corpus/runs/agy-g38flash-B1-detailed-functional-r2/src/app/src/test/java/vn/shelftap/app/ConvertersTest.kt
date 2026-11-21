package vn.shelftap.app

import org.junit.Assert.assertEquals
import org.junit.Test
import vn.shelftap.app.data.local.Converters

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun testStringListConversion() {
        val list = listOf("Fiction", "Mystery", "Detective")
        val json = converters.fromStringList(list)
        val deserialized = converters.toStringList(json)

        assertEquals(list, deserialized)
    }

    @Test
    fun testEmptyListConversion() {
        val empty = emptyList<String>()
        val json = converters.fromStringList(empty)
        val deserialized = converters.toStringList(json)

        assertEquals(empty, deserialized)
    }

    @Test
    fun testNullAndBlankHandling() {
        assertEquals(emptyList<String>(), converters.toStringList(null))
        assertEquals(emptyList<String>(), converters.toStringList(""))
        assertEquals(emptyList<String>(), converters.toStringList("   "))
    }
}
