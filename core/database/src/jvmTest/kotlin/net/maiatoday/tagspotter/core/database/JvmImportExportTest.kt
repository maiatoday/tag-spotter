package net.maiatoday.tagspotter.core.database

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.maiatoday.tagspotter.core.model.Spot
import net.maiatoday.tagspotter.core.photo.FakePhotoProcessor
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JvmImportExportTest {

    private lateinit var db: SpotDatabase
    private lateinit var repository: LocalSpotRepository
    private lateinit var tempDir: File

    @BeforeTest
    fun setup() {
        db = getTestDatabaseBuilder().build()
        repository = LocalSpotRepository(db.spotDao(), FakePhotoProcessor())
        tempDir = Files.createTempDirectory("jvm_import_export_test").toFile()
    }

    @AfterTest
    fun cleanup() {
        db.close()
        tempDir.deleteRecursively()
    }

    @Test
    fun testExportAndImportPackOnJvm() = runTest {
        val filesDir = File(tempDir, "files").apply { mkdirs() }
        val cacheDir = File(tempDir, "cache").apply { mkdirs() }

        val thumbnailsDir = File(filesDir, "thumbnails").apply { mkdirs() }
        val imagesDir = File(filesDir, "images").apply { mkdirs() }

        val dummyThumbFile = File(thumbnailsDir, "dummy_thumb.jpg").apply { writeText("fake thumb data") }
        val dummyImageFile = File(imagesDir, "dummy_image.jpg").apply { writeText("fake image data") }

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
        val packFile = File(cacheDir, "export_test.ts_pack")
        MultiplatformPackExporter.exportPack(
            spots = initialSpots,
            destZipFilePath = packFile.absolutePath,
            cacheDir = cacheDir.absolutePath,
            minRating = 0
        )

        assertTrue(packFile.exists())
        assertTrue(packFile.length() > 0)

        // 3. Clear repository database and delete files
        repository.deleteSpot(initialSpotDetails)
        val spotsAfterDelete = repository.getAllSpots().first()
        assertTrue(spotsAfterDelete.isEmpty())

        // 4. Import pack
        val importedCount = MultiplatformPackImporter.importPack(
            repository = repository,
            packFilePath = packFile.absolutePath,
            filesDir = filesDir.absolutePath,
            cacheDir = cacheDir.absolutePath,
            currentPhotographerName = "",
            createThumbnail = { "dummy_thumb_path" }
        )

        assertEquals(1, importedCount)

        // 5. Verify imported spot details
        val importedSpots = repository.getAllSpots().first()
        assertEquals(1, importedSpots.size)

        val importedDetail = importedSpots.first()
        assertEquals("Original Stencil NYC", importedDetail.spot.description)
        assertTrue(importedDetail.spot.isImported)

        // Clean up
        packFile.delete()
    }
}
