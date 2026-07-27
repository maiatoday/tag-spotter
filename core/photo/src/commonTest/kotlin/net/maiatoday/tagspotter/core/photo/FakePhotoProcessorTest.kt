package net.maiatoday.tagspotter.core.photo

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FakePhotoProcessorTest {

    @Test
    fun testFakePhotoProcessorMethods() = runTest {
        val processor = FakePhotoProcessor()

        // 1. Save image to public gallery
        val publicUri = processor.saveImageToPublicGallery("/path/to/img.jpg")
        assertEquals("public_uri", publicUri)
        assertEquals("/path/to/img.jpg", processor.saveImageCalledWith)

        // 2. Create thumbnail from file
        val thumbFile = processor.createThumbnailFromFile("/path/to/img.jpg")
        assertEquals("thumb_path", thumbFile)
        assertEquals("/path/to/img.jpg", processor.createThumbnailFromFileCalledWith)

        // 3. Create thumbnail from URI
        val thumbUri = processor.createThumbnailFromUri("content://media/123")
        assertEquals("thumb_path", thumbUri)
        assertEquals("content://media/123", processor.createThumbnailFromUriCalledWith)

        // 4. Extract metadata from URI
        val metadata = processor.extractMetadataFromUri("content://media/123")
        assertNotNull(metadata)
        assertEquals(12.34, metadata.latitude)
        assertEquals(56.78, metadata.longitude)
        assertEquals("content://media/123", processor.extractMetadataFromUriCalledWith)

        // 5. Create temp camera file
        val tempDetails = processor.createTempCameraFile()
        assertEquals("temp_uri", tempDetails.uriString)
        assertEquals("temp_path", tempDetails.fileAbsolutePath)

        // 6. Delete file
        val deleted = processor.deleteFile("/path/to/temp.jpg")
        assertTrue(deleted)
        assertEquals("/path/to/temp.jpg", processor.deleteFileCalledWith)

        // 7. Write bytes to file
        val written = processor.writeBytesToFile(byteArrayOf(1, 2, 3), "/path/to/out.jpg")
        assertTrue(written)
        assertTrue(processor.writeBytesCalled)
        assertEquals("/path/to/out.jpg", processor.writeBytesPath)
    }
}
