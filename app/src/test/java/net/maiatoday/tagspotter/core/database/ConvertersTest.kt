package net.maiatoday.tagspotter.core.database

import org.junit.Assert
import org.junit.Test

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun testSerializationAndDeserialization() {
        val list = listOf("tag1", "tag2", "tag3")
        val json = converters.fromStringList(list)
        val deserialized = converters.toStringList(json)
        Assert.assertEquals(list, deserialized)
    }

    @Test
    fun testSerializationWithCommas() {
        val list = listOf("tag,one", "tag,two", "tag three")
        val json = converters.fromStringList(list)
        val deserialized = converters.toStringList(json)
        Assert.assertEquals(list, deserialized)
    }

    @Test
    fun testFallbackForOldFormat() {
        val oldFormat = "tag1,tag2,tag3"
        val expected = listOf("tag1", "tag2", "tag3")
        val deserialized = converters.toStringList(oldFormat)
        Assert.assertEquals(expected, deserialized)
    }

    @Test
    fun testFallbackForEmptyString() {
        val empty = ""
        val expected = emptyList<String>()
        val deserialized = converters.toStringList(empty)
        Assert.assertEquals(expected, deserialized)
    }
}