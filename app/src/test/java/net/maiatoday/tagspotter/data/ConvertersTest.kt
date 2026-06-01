package net.maiatoday.tagspotter.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun testSerializationAndDeserialization() {
        val list = listOf("tag1", "tag2", "tag3")
        val json = converters.fromStringList(list)
        val deserialized = converters.toStringList(json)
        assertEquals(list, deserialized)
    }

    @Test
    fun testSerializationWithCommas() {
        val list = listOf("tag,one", "tag,two", "tag three")
        val json = converters.fromStringList(list)
        val deserialized = converters.toStringList(json)
        assertEquals(list, deserialized)
    }

    @Test
    fun testFallbackForOldFormat() {
        val oldFormat = "tag1,tag2,tag3"
        val expected = listOf("tag1", "tag2", "tag3")
        val deserialized = converters.toStringList(oldFormat)
        assertEquals(expected, deserialized)
    }

    @Test
    fun testFallbackForEmptyString() {
        val empty = ""
        val expected = emptyList<String>()
        val deserialized = converters.toStringList(empty)
        assertEquals(expected, deserialized)
    }
}
