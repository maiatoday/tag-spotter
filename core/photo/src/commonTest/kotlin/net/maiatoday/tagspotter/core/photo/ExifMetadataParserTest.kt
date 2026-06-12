package net.maiatoday.tagspotter.core.photo

import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExifMetadataParserTest {

    @Test
    fun testExtractMetadataWithEmptyBytesReturnsNull() {
        val result = ExifMetadataParser.extractMetadata(byteArrayOf())
        assertNull(result)
    }

    @Test
    fun testFakePhotoProcessor() {
        val fake = FakePhotoProcessor()
        assertNull(fake.decodeScaledBitmapResult)
        fake.decodeScaledBitmapResult = byteArrayOf(1, 2, 3)
        assertTrue(fake.decodeScaledBitmapResult!!.contentEquals(byteArrayOf(1, 2, 3)))
    }
}
