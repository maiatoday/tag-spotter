package net.maiatoday.spotcache.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.maiatoday.spotcache.core.photo.AndroidPhotoProcessor
import net.maiatoday.spotcache.core.model.Spot
import org.junit.After
import org.junit.Assert
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
        repository = LocalSpotRepository(db.spotDao(), AndroidPhotoProcessor(context))
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

            val dummyThumbFile = File(
                thumbnailsDir,
                "dummy_thumb_${System.currentTimeMillis()}.jpg"
            ).apply { writeText("fake thumb data") }
            val dummyImageFile = File(
                imagesDir,
                "dummy_image_${System.currentTimeMillis()}.jpg"
            ).apply { writeText("fake image data") }

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
            Assert.assertEquals(1, initialSpots.size)
            val initialSpotDetails = initialSpots.first()
            Assert.assertEquals("Original Stencil NYC", initialSpotDetails.spot.description)

            // 2. Export pack
            val packFile = File(context.cacheDir, "export_test.ts_pack")
            packFile.outputStream().use { fos ->
                PackManager.exportPack(context, initialSpots, fos)
            }

            Assert.assertTrue(packFile.exists())
            Assert.assertTrue(packFile.length() > 0)

            // 3. Clear repository database and delete files
            repository.deleteSpot(initialSpotDetails)
            val spotsAfterDelete = repository.getAllSpots().first()
            Assert.assertTrue(spotsAfterDelete.isEmpty())
            Assert.assertTrue(!dummyThumbFile.exists()) // deleteSpot should delete the thumbnail
            Assert.assertTrue(!dummyImageFile.exists()) // deleteSpot should delete the local image file too!

            // 4. Import pack
            val importedCount = packFile.inputStream().use { fis ->
                PackManager.importPack(context, repository, fis, "")
            }

            Assert.assertEquals(1, importedCount)

            // 5. Verify imported spot details
            val importedSpots = repository.getAllSpots().first()
            Assert.assertEquals(1, importedSpots.size)

            val importedDetail = importedSpots.first()
            Assert.assertEquals("Original Stencil NYC", importedDetail.spot.description)
            Assert.assertTrue(importedDetail.spot.isImported)

            // Verify files were extracted and paths updated
            val importedImage = importedDetail.images.first()
            Assert.assertNotEquals(dummyImageFile.absolutePath, importedImage.imagePath)
            Assert.assertNotEquals(dummyThumbFile.absolutePath, importedImage.thumbnailPath)

            val newThumbFile = File(importedImage.thumbnailPath)
            val newImageFile = File(importedImage.imagePath)

            Assert.assertTrue(newThumbFile.exists())
            Assert.assertTrue(newImageFile.exists())

            Assert.assertEquals("fake thumb data", newThumbFile.readText())
            Assert.assertEquals("fake image data", newImageFile.readText())

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

            val dummyThumbFile = File(
                thumbnailsDir,
                "dummy_thumb_own_${System.currentTimeMillis()}.jpg"
            ).apply { writeText("fake thumb data") }
            val dummyImageFile = File(
                imagesDir,
                "dummy_image_own_${System.currentTimeMillis()}.jpg"
            ).apply { writeText("fake image data") }

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
            Assert.assertEquals(1, count1)
            var importedSpots = repository.getAllSpots().first()
            Assert.assertEquals(1, importedSpots.size)
            Assert.assertTrue(importedSpots.first().spot.isImported)

            // Cleanup spot from database and files
            repository.deleteSpot(importedSpots.first())

            // 2. Import with SAME photographer name ("Alice") -> Should NOT be marked as imported
            val count2 = packFile.inputStream().use { fis ->
                PackManager.importPack(context, repository, fis, "Alice")
            }
            Assert.assertEquals(1, count2)
            importedSpots = repository.getAllSpots().first()
            Assert.assertEquals(1, importedSpots.size)
            Assert.assertTrue(!importedSpots.first().spot.isImported) // isImported should be false!

            // Cleanup spot from database and files
            repository.deleteSpot(importedSpots.first())
            packFile.delete()
        }
    }

    @Test
    fun testExportWithMinRatingFilterAndHeroPreservation() {
        runBlocking {
            val thumbnailsDir = File(context.filesDir, "thumbnails").apply { mkdirs() }
            val imagesDir = File(context.filesDir, "images").apply { mkdirs() }

            val dummyThumbFile1 = File(
                thumbnailsDir,
                "dummy_thumb_1_${System.currentTimeMillis()}.jpg"
            ).apply { writeText("thumb1") }
            val dummyImageFile1 = File(
                imagesDir,
                "dummy_image_1_${System.currentTimeMillis()}.jpg"
            ).apply { writeText("img1") }

            val dummyThumbFile2 = File(
                thumbnailsDir,
                "dummy_thumb_2_${System.currentTimeMillis()}.jpg"
            ).apply { writeText("thumb2") }
            val dummyImageFile2 = File(
                imagesDir,
                "dummy_image_2_${System.currentTimeMillis()}.jpg"
            ).apply { writeText("img2") }

            val dummyThumbFile3 = File(
                thumbnailsDir,
                "dummy_thumb_3_${System.currentTimeMillis()}.jpg"
            ).apply { writeText("thumb3") }
            val dummyImageFile3 = File(
                imagesDir,
                "dummy_image_3_${System.currentTimeMillis()}.jpg"
            ).apply { writeText("img3") }

            val spot = Spot(
                id = 10L,
                latitude = 40.0,
                longitude = -74.0,
                createdAt = System.currentTimeMillis(),
                description = "Export Filter Test Spot",
                tags = emptyList(),
                category = "graffiti",
                status = "active"
            )
            val spotId = repository.saveSpot(
                spot,
                dummyImageFile1.absolutePath,
                dummyThumbFile1.absolutePath
            )

            // Add two more images
            val imgId2 = repository.addImageToSpot(
                spotId,
                dummyImageFile2.absolutePath,
                dummyThumbFile2.absolutePath,
                System.currentTimeMillis() + 1000
            )
            val imgId3 = repository.addImageToSpot(
                spotId,
                dummyImageFile3.absolutePath,
                dummyThumbFile3.absolutePath,
                System.currentTimeMillis() + 2000
            )

            // Fetch the inserted images to get the first one's ID
            val initialSpotDetails = repository.getSpotById(spotId).first()!!
            val imgId1 =
                initialSpotDetails.images.first { it.imagePath == dummyImageFile1.absolutePath }.id

            // Set main image (hero image) to the first image (rated 0 stars)
            repository.setMainImage(spotId, imgId1)

            // Update ratings:
            // img1 (hero): 0 stars
            // img2: 1 star
            // img3: 5 stars
            repository.updateImageRating(imgId1, 0)
            repository.updateImageRating(imgId2, 1)
            repository.updateImageRating(imgId3, 5)

            // Fetch updated spot details to verify ratings are set
            val spotsToExport = repository.getAllSpots().first()

            // Export with minRating = 3
            val packFile = File(context.cacheDir, "rating_export_test.ts_pack")
            packFile.outputStream().use { fos ->
                PackManager.exportPack(context, spotsToExport, fos, minRating = 3)
            }

            // Cleanup local DB spot and files to avoid clashes when importing
            repository.deleteSpot(initialSpotDetails)

            // Import the pack
            val importedCount = packFile.inputStream().use { fis ->
                PackManager.importPack(context, repository, fis, "")
            }
            Assert.assertEquals(1, importedCount)

            // Verify imported spot details
            val importedSpots = repository.getAllSpots().first()
            Assert.assertEquals(1, importedSpots.size)
            val importedDetail = importedSpots.first()

            // Verify that exactly 2 images were imported:
            // 1. The 5-star image (rating >= 3)
            // 2. The 0-star hero image (preserved because it's the hero image)
            // The 1-star image should be excluded.
            Assert.assertEquals(2, importedDetail.images.size)

            val rating0Image = importedDetail.images.find { it.rating == 0 }
            val rating5Image = importedDetail.images.find { it.rating == 5 }
            val rating1Image = importedDetail.images.find { it.rating == 1 }

            Assert.assertNotNull("Hero image with 0 stars should be imported", rating0Image)
            Assert.assertNotNull("5-star image should be imported", rating5Image)
            Assert.assertTrue("1-star image should NOT be imported", rating1Image == null)

            // Clean up files and database
            repository.deleteSpot(importedDetail)
            packFile.delete()
        }
    }

    @Test
    fun testImportExportWithArtworkDate() {
        runBlocking {
            val thumbnailsDir = File(context.filesDir, "thumbnails").apply { mkdirs() }
            val imagesDir = File(context.filesDir, "images").apply { mkdirs() }

            val dummyThumbFile = File(
                thumbnailsDir,
                "dummy_thumb_date_${System.currentTimeMillis()}.jpg"
            ).apply { writeText("thumb") }
            val dummyImageFile = File(
                imagesDir,
                "dummy_image_date_${System.currentTimeMillis()}.jpg"
            ).apply { writeText("img") }

            val spot = Spot(
                id = 20L,
                latitude = 40.0,
                longitude = -74.0,
                createdAt = System.currentTimeMillis(),
                description = "Artwork Date Spot",
                tags = emptyList(),
                category = "sculpture",
                status = "active",
                artworkDate = "circa 2005"
            )
            val spotId =
                repository.saveSpot(spot, dummyImageFile.absolutePath, dummyThumbFile.absolutePath)

            val initialSpots = repository.getAllSpots().first()
            val initialDetails = initialSpots.first { it.spot.id == spotId }
            Assert.assertEquals("circa 2005", initialDetails.spot.artworkDate)

            // Export
            val packFile = File(context.cacheDir, "artwork_date_test.ts_pack")
            packFile.outputStream().use { fos ->
                PackManager.exportPack(context, listOf(initialDetails), fos)
            }

            // Cleanup local DB spot and files to avoid clashes when importing
            repository.deleteSpot(initialDetails)

            // Import
            val importedCount = packFile.inputStream().use { fis ->
                PackManager.importPack(context, repository, fis, "")
            }
            Assert.assertEquals(1, importedCount)

            // Verify imported spot has the correct artworkDate
            val importedSpots = repository.getAllSpots().first()
            val importedDetail = importedSpots.first()
            Assert.assertEquals("circa 2005", importedDetail.spot.artworkDate)

            // Cleanup
            repository.deleteSpot(importedDetail)
            packFile.delete()
        }
    }
}