package net.maiatoday.tagspotter.core.database

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import net.maiatoday.tagspotter.core.model.BackupWrapper
import net.maiatoday.tagspotter.core.model.Spot
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.model.SpotNote
import net.maiatoday.tagspotter.core.photo.FakePhotoProcessor
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    @Test
    fun testLegacyV1PackImport() = runTest {
        val filesDir = File(tempDir, "files_v1").apply { mkdirs() }
        val cacheDir = File(tempDir, "cache_v1").apply { mkdirs() }

        // Create a V1 staging folder
        val stagingDir = File(tempDir, "v1_staging").apply { mkdirs() }

        // Generate V1 legacy list (direct List<SpotDetails> without wrappers, with blank UUIDs)
        val legacySpot = Spot(
            id = 0L,
            latitude = 45.4642,
            longitude = 9.1899,
            createdAt = 100000L,
            description = "Legacy V1 Spot Milan",
            tags = listOf("milan", "duomo"),
            category = "graffiti",
            status = "active",
            uuid = "",
            lastEditedAt = 0L
        )
        val legacyDetails = SpotDetails(legacySpot, emptyList(), emptyList())
        val jsonString = Json.encodeToString(listOf(legacyDetails))

        val spotsJsonFile = File(stagingDir, "spots.json")
        spotsJsonFile.writeText(jsonString)

        // Compress stage to .ts_pack
        val packFile = File(cacheDir, "legacy_v1.ts_pack")
        zip(stagingDir.absolutePath, packFile.absolutePath)

        assertTrue(packFile.exists())

        // Import the V1 package
        val importedCount = MultiplatformPackImporter.importPack(
            repository = repository,
            packFilePath = packFile.absolutePath,
            filesDir = filesDir.absolutePath,
            cacheDir = cacheDir.absolutePath,
            currentPhotographerName = "",
            createThumbnail = { null }
        )

        assertEquals(1, importedCount)

        // Verify the database now contains the legacy spot with hydrated UUID and correct lastEditedAt
        val allSpots = repository.getAllSpots().first()
        assertEquals(1, allSpots.size)
        val importedSpot = allSpots.first().spot

        assertFalse(importedSpot.uuid.isEmpty(), "UUID should be populated on the fly for legacy spots.")
        assertEquals(100000L, importedSpot.lastEditedAt, "lastEditedAt should be hydrated to createdAt.")
        assertEquals("Legacy V1 Spot Milan", importedSpot.description)

        // Try importing the same pack again to verify exact-session deduplication
        val reImportCount = MultiplatformPackImporter.importPack(
            repository = repository,
            packFilePath = packFile.absolutePath,
            filesDir = filesDir.absolutePath,
            cacheDir = cacheDir.absolutePath,
            currentPhotographerName = "",
            createThumbnail = { null }
        )
        // Deduplication rule should prevent importing the exact same createdAt timestamp spot
        assertEquals(0, reImportCount, "V1 Exact-Session duplicate should have been skipped.")
    }

    @Test
    fun testV2PackImportWithLwwOverwrite() = runTest {
        val filesDir = File(tempDir, "files_v2").apply { mkdirs() }
        val cacheDir = File(tempDir, "cache_v2").apply { mkdirs() }

        // Setup local database state with an initial spot
        val localSpot = Spot(
            id = 0L,
            latitude = 48.8566,
            longitude = 2.3522,
            createdAt = 1000L,
            description = "Paris Local Version",
            tags = listOf("paris"),
            category = "graffiti",
            status = "active",
            uuid = "unique-test-uuid-paris",
            lastEditedAt = 1000L,
            isSynced = true
        )
        repository.saveSpotDetails(SpotDetails(localSpot, emptyList(), emptyList()))

        // Verify local spot is inserted
        val beforeImport = repository.getAllSpots().first()
        assertEquals(1, beforeImport.size)
        assertEquals("Paris Local Version", beforeImport.first().spot.description)

        // Case A: Import a NEWER version of the same spot (LWW)
        val stagingDirNew = File(tempDir, "v2_staging_new").apply { mkdirs() }
        val importedSpotNew = localSpot.copy(
            description = "Paris Imported Newer Version",
            lastEditedAt = 2000L // Newer timestamp!
        )
        val importedNote = SpotNote(id = 0L, spotId = 0L, noteText = "Imported Note", timestamp = 2000L, uuid = "imported-note-uuid")
        val detailsNew = SpotDetails(importedSpotNew, emptyList(), listOf(importedNote))
        val wrapperNew = BackupWrapper(backupVersion = 2, spots = listOf(detailsNew))
        
        File(stagingDirNew, "spots.json").writeText(Json.encodeToString(wrapperNew))
        val packFileNew = File(cacheDir, "v2_new.ts_pack")
        zip(stagingDirNew.absolutePath, packFileNew.absolutePath)

        val importNewCount = MultiplatformPackImporter.importPack(
            repository = repository,
            packFilePath = packFileNew.absolutePath,
            filesDir = filesDir.absolutePath,
            cacheDir = cacheDir.absolutePath,
            currentPhotographerName = "",
            createThumbnail = { null }
        )

        assertEquals(1, importNewCount)

        // Verify local spot is updated and note is imported
        val afterImportNew = repository.getAllSpots().first()
        assertEquals(1, afterImportNew.size)
        val updatedDetail = afterImportNew.first()
        assertEquals("Paris Imported Newer Version", updatedDetail.spot.description)
        assertEquals(2000L, updatedDetail.spot.lastEditedAt)
        assertEquals(1, updatedDetail.notes.size)
        assertEquals("Imported Note", updatedDetail.notes.first().noteText)
        assertFalse(updatedDetail.spot.isSynced, "Overwritten spot should be marked as unsynced to queue sync updates.")

        // Case B: Import an OLDER version of the same spot (LWW - should ignore)
        val stagingDirOld = File(tempDir, "v2_staging_old").apply { mkdirs() }
        val importedSpotOld = localSpot.copy(
            description = "Paris Imported Older Version",
            lastEditedAt = 1500L // Older than 2000L!
        )
        val detailsOld = SpotDetails(importedSpotOld, emptyList(), emptyList())
        val wrapperOld = BackupWrapper(backupVersion = 2, spots = listOf(detailsOld))

        File(stagingDirOld, "spots.json").writeText(Json.encodeToString(wrapperOld))
        val packFileOld = File(cacheDir, "v2_old.ts_pack")
        zip(stagingDirOld.absolutePath, packFileOld.absolutePath)

        val importOldCount = MultiplatformPackImporter.importPack(
            repository = repository,
            packFilePath = packFileOld.absolutePath,
            filesDir = filesDir.absolutePath,
            cacheDir = cacheDir.absolutePath,
            currentPhotographerName = "",
            createThumbnail = { null }
        )

        // Count is 0 (or skipped because the older version did not trigger an overwrite)
        assertEquals(0, importOldCount, "Older imported version should be ignored.")

        // Verify that local spot remained as the newer version
        val finalSpots = repository.getAllSpots().first()
        assertEquals(1, finalSpots.size)
        assertEquals("Paris Imported Newer Version", finalSpots.first().spot.description)
    }
}
