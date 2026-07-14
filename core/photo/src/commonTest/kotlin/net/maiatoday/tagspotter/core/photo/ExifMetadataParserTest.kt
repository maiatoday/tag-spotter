package net.maiatoday.tagspotter.core.photo

import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class ExifMetadataParserTest {

    @Test
    fun testExtractMetadataWithEmptyBytesReturnsNull() {
        val result = ExifMetadataParser.extractMetadata(byteArrayOf())
        assertNull(result)
    }

    @Test
    fun testFakePhotoProcessorExceptionSimulations() {
        val fake = FakePhotoProcessor()
        fake.tempCameraFileException = RuntimeException("Camera disconnected")
        
        try {
            fake.createTempCameraFile()
            assertTrue(false, "Should have thrown exception")
        } catch (e: Exception) {
            assertTrue(e is RuntimeException)
            assertEquals("Camera disconnected", e.message)
        }
    }
}
