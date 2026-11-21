package net.maiatoday.tagspotter.core.photo

import kotlinx.coroutines.test.runTest
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RealPhotoProcessorJvmTest {

    @Test
    fun testUrlEncodedFileUriWithSpacesResolvesAndDownscalesToJpeg() = runTest {
        val tempDir = Files.createTempDirectory("tagspotter photo test ").toFile()
        val imageFile = File(tempDir, "my mural photo 01.png")
        try {
            // Create a 2000x1000 PNG with alpha channel (ARGB)
            val source = BufferedImage(2000, 1000, BufferedImage.TYPE_INT_ARGB)
            val g = source.createGraphics()
            g.color = Color(0, 255, 128, 180)
            g.fillRect(0, 0, 2000, 1000)
            g.dispose()
            assertTrue(ImageIO.write(source, "png", imageFile))

            val encodedUri = imageFile.toURI().toString()
            assertTrue(encodedUri.contains("%20"), "Expected URL-encoded spaces in URI: $encodedUri")

            val processor = RealPhotoProcessor()
            assertTrue(processor.fileExists(encodedUri), "fileExists should resolve URL-encoded file: URI")

            val scaledBytes = processor.decodeScaledBitmap(encodedUri, maxDimension = 1024)
            assertNotNull(scaledBytes, "decodeScaledBitmap should return JPEG bytes for URL-encoded PNG")
            assertTrue(scaledBytes.size > 4)
            // JPEG SOI magic bytes: 0xFF 0xD8
            assertEquals(0xFF.toByte(), scaledBytes[0], "Output should start with JPEG SOI byte 0xFF")
            assertEquals(0xD8.toByte(), scaledBytes[1], "Output should start with JPEG SOI byte 0xD8")

            val decoded = ImageIO.read(ByteArrayInputStream(scaledBytes))
            assertNotNull(decoded)
            assertEquals(1024, decoded.width, "Width should be scaled down to maxDimension")
            assertEquals(512, decoded.height, "Height should preserve 2:1 aspect ratio")

            assertTrue(processor.deleteFile(encodedUri), "deleteFile should delete URL-encoded file: URI")
            assertFalse(imageFile.exists())
        } finally {
            imageFile.delete()
            tempDir.delete()
        }
    }

    @Test
    fun testSmallImageIsNotUpscaledAndEncodedAsJpeg() = runTest {
        val tempDir = Files.createTempDirectory("tagspotter_small_test").toFile()
        val imageFile = File(tempDir, "small.png")
        try {
            val source = BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB)
            assertTrue(ImageIO.write(source, "png", imageFile))

            val processor = RealPhotoProcessor()
            val bytes = processor.decodeScaledBitmap(imageFile.absolutePath, maxDimension = 1024)
            assertNotNull(bytes)
            assertEquals(0xFF.toByte(), bytes[0])
            assertEquals(0xD8.toByte(), bytes[1])

            val decoded = ImageIO.read(ByteArrayInputStream(bytes))
            assertNotNull(decoded)
            assertEquals(400, decoded.width)
            assertEquals(300, decoded.height)
        } finally {
            imageFile.delete()
            tempDir.delete()
        }
    }
}
