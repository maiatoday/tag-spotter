package net.maiatoday.spotcache.core.database

import android.content.ContextWrapper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.maiatoday.spotcache.core.model.Spot
import net.maiatoday.spotcache.core.model.SpotDetails
import net.maiatoday.spotcache.core.model.SpotImage
import net.maiatoday.spotcache.core.model.SpotNote
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream

class PackManagerTest {

    @TempDir
    lateinit var tempFolder: File

    class FakeContext(private val baseDir: File) : ContextWrapper(null) {
        override fun getCacheDir(): File {
            val cache = File(baseDir, "cache")
            cache.mkdirs()
            return cache
        }

        override fun getFilesDir(): File {
            val files = File(baseDir, "files")
            files.mkdirs()
            return files
        }
    }

    @Test
    fun testExportAndImportPack() = runTest {
        val baseDir = File(tempFolder, "fake_android_dir").apply { mkdirs() }
        val context = FakeContext(baseDir)
        val repository = FakeSpotRepository()

        // 1. Create dummy files to export
        val imageDir = File(tempFolder, "images").apply { mkdirs() }
        val dummyImageFile = File(imageDir, "my_art.jpg")
        dummyImageFile.writeText("fake image content bytes")

        val thumbDir = File(tempFolder, "thumbs").apply { mkdirs() }
        val dummyThumbFile = File(thumbDir, "my_art_thumb.jpg")
        dummyThumbFile.writeText("fake thumb bytes")

        // 2. Prepare SpotDetails to export
        val spot = Spot(
            id = 1L,
            latitude = 45.4642,
            longitude = 9.1899,
            createdAt = 123456789L,
            description = "Street art in Milan",
            tags = listOf("stencil", "milan"),
            category = "graffiti",
            status = "active",
            artists = listOf("Artiste"),
            photographer = "Photographer A"
        )
        val image = SpotImage(
            id = 10L,
            spotId = 1L,
            imagePath = dummyImageFile.absolutePath,
            thumbnailPath = dummyThumbFile.absolutePath,
            timestamp = 123456789L,
            isMain = true,
            rating = 4
        )
        val note = SpotNote(
            id = 100L,
            spotId = 1L,
            noteText = "Amazing colors",
            timestamp = 123456789L
        )
        val spotDetails = SpotDetails(spot, listOf(image), listOf(note))

        // 3. Export to Zip
        val outputStream = ByteArrayOutputStream()
        PackManager.exportPack(context, listOf(spotDetails), outputStream, minRating = 3)
        val zipBytes = outputStream.toByteArray()

        // Verify Zip entries
        val zipInputStream = ZipInputStream(ByteArrayInputStream(zipBytes))
        val entryNames = mutableListOf<String>()
        var entry = zipInputStream.nextEntry
        while (entry != null) {
            entryNames.add(entry.name)
            entry = zipInputStream.nextEntry
        }
        zipInputStream.close()

        // ZIP should contain spots.json, the image under images/ and thumbnail under thumbnails/
        assertEquals(3, entryNames.size)
        assertTrue(entryNames.contains("spots.json"))
        assertTrue(entryNames.contains("images/my_art.jpg"))
        assertTrue(entryNames.contains("thumbnails/my_art_thumb.jpg"))

        // 4. Import the Zip into a different repository
        val importRepository = FakeSpotRepository()
        // Initially empty
        assertEquals(0, importRepository.getAllSpots().first().size)

        val importedCount = PackManager.importPack(
            context = context,
            repository = importRepository,
            inputStream = ByteArrayInputStream(zipBytes),
            currentPhotographerName = "Photographer B" // different photographer name, so should mark as imported
        )

        assertEquals(1, importedCount)

        val importedSpots = importRepository.getAllSpots().first()
        assertEquals(1, importedSpots.size)

        val importedDetails = importedSpots.first()
        assertEquals("Street art in Milan", importedDetails.spot.description)
        assertEquals(true, importedDetails.spot.isImported) // Marked as imported because current photographer is different
        assertEquals(1, importedDetails.images.size)
        assertEquals(1, importedDetails.notes.size)
        assertEquals("Amazing colors", importedDetails.notes.first().noteText)

        // Verify files were copied to the fake filesDir (files/images and files/thumbnails)
        val importedImageFile = File(importedDetails.images.first().imagePath)
        val importedThumbFile = File(importedDetails.images.first().thumbnailPath)
        
        assertTrue(importedImageFile.exists())
        assertEquals("fake image content bytes", importedImageFile.readText())
        assertTrue(importedThumbFile.exists())
        assertEquals("fake thumb bytes", importedThumbFile.readText())
    }
}
