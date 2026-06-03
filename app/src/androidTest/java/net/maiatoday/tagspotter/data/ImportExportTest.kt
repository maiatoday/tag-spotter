package net.maiatoday.tagspotter.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.maiatoday.tagspotter.utils.PackManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ImportExportTest {

    private lateinit var db: SpotDatabase
    private lateinit var repository: LocalSpotRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, SpotDatabase::class.java).build()
        repository = LocalSpotRepository(db.spotDao())
    }

    @After
    fun cleanup() {
        db.close()
    }

    @Test
    fun testExportAndImportPackWithImagesAndThumbnails() {
        runBlocking {
        // 1. Setup local files and database spot
        val thumbnailsDir = File(context.filesDir, "thumbnails").apply { mkdirs() }
        val imagesDir = File(context.filesDir, "images").apply { mkdirs() }

        val dummyThumbFile = File(thumbnailsDir, "dummy_thumb_${System.currentTimeMillis()}.jpg").apply { writeText("fake thumb data") }
        val dummyImageFile = File(imagesDir, "dummy_image_${System.currentTimeMillis()}.jpg").apply { writeText("fake image data") }

        val spot = Spot(
            id = 1L,
            latitude = 40.7128,
            longitude = -74.0060,
            createdAt = System.currentTimeMillis(),
            description = "Original Stencil NYC",
            tags = listOf("nyc", "stencil"),
            category = "graffiti",
            status = "active"
        )
        repository.saveSpot(spot, dummyImageFile.absolutePath, dummyThumbFile.absolutePath)

        // Verify spot exists
        val initialSpots = repository.getAllSpots().first()
        assertEquals(1, initialSpots.size)
        val initialSpotDetails = initialSpots.first()
        assertEquals("Original Stencil NYC", initialSpotDetails.spot.description)

        // 2. Export pack
        val packFile = File(context.cacheDir, "export_test.ts_pack")
        packFile.outputStream().use { fos ->
            PackManager.exportPack(context, initialSpots, fos)
        }

        assertTrue(packFile.exists())
        assertTrue(packFile.length() > 0)

        // 3. Clear repository database and delete files
        repository.deleteSpot(initialSpotDetails)
        val spotsAfterDelete = repository.getAllSpots().first()
        assertTrue(spotsAfterDelete.isEmpty())
        assertTrue(!dummyThumbFile.exists()) // deleteSpot should delete the thumbnail
        assertTrue(!dummyImageFile.exists()) // deleteSpot should delete the local image file too!

        // 4. Import pack
        val importedCount = packFile.inputStream().use { fis ->
            PackManager.importPack(context, repository, fis, "")
        }

        assertEquals(1, importedCount)

        // 5. Verify imported spot details
        val importedSpots = repository.getAllSpots().first()
        assertEquals(1, importedSpots.size)

        val importedDetail = importedSpots.first()
        assertEquals("Original Stencil NYC", importedDetail.spot.description)
        assertTrue(importedDetail.spot.isImported)

        // Verify files were extracted and paths updated
        val importedImage = importedDetail.images.first()
        assertNotEquals(dummyImageFile.absolutePath, importedImage.imagePath)
        assertNotEquals(dummyThumbFile.absolutePath, importedImage.thumbnailPath)

        val newThumbFile = File(importedImage.thumbnailPath)
        val newImageFile = File(importedImage.imagePath)

        assertTrue(newThumbFile.exists())
        assertTrue(newImageFile.exists())

        assertEquals("fake thumb data", newThumbFile.readText())
        assertEquals("fake image data", newImageFile.readText())

        // Cleanup
        newThumbFile.delete()
        newImageFile.delete()
        packFile.delete()
        }
    }

    @Test
    fun testImportPackOwnPhotographerNotMarkedAsImported() {
        runBlocking {
            // Setup
            val thumbnailsDir = File(context.filesDir, "thumbnails").apply { mkdirs() }
            val imagesDir = File(context.filesDir, "images").apply { mkdirs() }

            val dummyThumbFile = File(thumbnailsDir, "dummy_thumb_own_${System.currentTimeMillis()}.jpg").apply { writeText("fake thumb data") }
            val dummyImageFile = File(imagesDir, "dummy_image_own_${System.currentTimeMillis()}.jpg").apply { writeText("fake image data") }

            // Photographer is "Alice"
            val spot = Spot(
                id = 2L,
                latitude = 45.0,
                longitude = 9.0,
                createdAt = System.currentTimeMillis(),
                description = "Own Spot",
                tags = emptyList(),
                category = "graffiti",
                status = "active",
                photographer = "Alice"
            )
            repository.saveSpot(spot, dummyImageFile.absolutePath, dummyThumbFile.absolutePath)
            val spots = repository.getAllSpots().first()

            // Export
            val packFile = File(context.cacheDir, "export_own_test.ts_pack")
            packFile.outputStream().use { fos ->
                PackManager.exportPack(context, spots, fos)
            }

            // Delete
            repository.deleteSpot(spots.first())

            // 1. Import with DIFFERENT photographer name ("Bob") -> Should be marked as imported
            val count1 = packFile.inputStream().use { fis ->
                PackManager.importPack(context, repository, fis, "Bob")
            }
            assertEquals(1, count1)
            var importedSpots = repository.getAllSpots().first()
            assertEquals(1, importedSpots.size)
            assertTrue(importedSpots.first().spot.isImported)

            // Cleanup spot from database and files
            repository.deleteSpot(importedSpots.first())

            // 2. Import with SAME photographer name ("Alice") -> Should NOT be marked as imported
            val count2 = packFile.inputStream().use { fis ->
                PackManager.importPack(context, repository, fis, "Alice")
            }
            assertEquals(1, count2)
            importedSpots = repository.getAllSpots().first()
            assertEquals(1, importedSpots.size)
            assertTrue(!importedSpots.first().spot.isImported) // isImported should be false!

            // Cleanup spot from database and files
            repository.deleteSpot(importedSpots.first())
            packFile.delete()
        }
    }
}
