package net.maiatoday.tagspotter.core.database

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.maiatoday.tagspotter.core.model.Spot
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.model.SpotImage
import net.maiatoday.tagspotter.core.model.SpotNote
import net.maiatoday.tagspotter.core.model.SpotOrigin
import net.maiatoday.tagspotter.core.photo.PhotoMetadata
import net.maiatoday.tagspotter.core.photo.PhotoProcessor
import net.maiatoday.tagspotter.core.photo.TempFileDetails
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private class TestPhotoProcessor(
    private val existingFiles: MutableSet<String> = mutableSetOf()
) : PhotoProcessor {
    override suspend fun saveImageToPublicGallery(filePath: String): String? = null
    override suspend fun createThumbnailFromUri(uriString: String): String? = null
    override suspend fun createThumbnailFromFile(filePath: String): String? = null
    override suspend fun extractMetadataFromUri(uriString: String): PhotoMetadata? = null
    override fun createTempCameraFile(): TempFileDetails = TempFileDetails("", "")
    override suspend fun decodeScaledBitmap(imagePath: String, maxDimension: Int): ByteArray? = null
    override suspend fun writeBytesToFile(bytes: ByteArray, filePath: String): Boolean = false
    override fun deleteFile(filePath: String): Boolean = existingFiles.remove(filePath)
    override fun fileExists(filePath: String): Boolean = filePath in existingFiles
}

class SpotOriginAndImagePreservationTest : BaseDaoTest() {

    private lateinit var db: SpotDatabase
    private lateinit var photoProcessor: TestPhotoProcessor
    private lateinit var repo: LocalSpotRepository

    @BeforeTest
    fun setup() {
        db = getTestDatabaseBuilder().build()
        photoProcessor = TestPhotoProcessor()
        repo = LocalSpotRepository(db.spotDao(), photoProcessor)
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun saveSyncedSpotUpdatesWithoutCascadeDeletingLocalImages() = runTest {
        repo.activeUid = "user-a"
        val localThumb = "/local/thumbs/img1.jpg"
        val localImg = "/local/images/img1.jpg"
        photoProcessor = TestPhotoProcessor(mutableSetOf(localThumb, localImg))
        repo = LocalSpotRepository(db.spotDao(), photoProcessor)
        repo.activeUid = "user-a"

        val spotId = repo.saveSpot(
            spot = Spot(
                latitude = 45.46,
                longitude = 9.19,
                createdAt = 1000L,
                description = "Original",
                tags = listOf("mural"),
                category = "graffiti",
                status = "active",
                uuid = "spot-own-1"
            ),
            imagePath = localImg,
            thumbnailPath = localThumb
        )
        val initial = repo.getSpotById(spotId).first()!!
        val imgUuid = initial.images.first().uuid

        // Simulate remote sync update coming back with HTTPS URLs
        val remoteUpdate = initial.copy(
            spot = initial.spot.copy(description = "Updated remotely", lastEditedAt = 2000L),
            images = listOf(
                initial.images.first().copy(
                    imagePath = "https://storage.example/spots/user-a/spot-own-1/thumbnails/$imgUuid.jpg",
                    thumbnailPath = "https://storage.example/spots/user-a/spot-own-1/thumbnails/$imgUuid.jpg",
                    remoteThumbnailUrl = "https://storage.example/spots/user-a/spot-own-1/thumbnails/$imgUuid.jpg"
                )
            )
        )
        repo.saveSyncedSpot(remoteUpdate)

        val afterSync = repo.getSpotById(spotId).first()!!
        assertEquals("Updated remotely", afterSync.spot.description)
        assertEquals(1, afterSync.images.size)
        // Local file paths must be preserved AND remoteThumbnailUrl populated
        assertEquals(localThumb, afterSync.images.first().thumbnailPath)
        assertEquals(localImg, afterSync.images.first().imagePath)
        assertEquals(
            "https://storage.example/spots/user-a/spot-own-1/thumbnails/$imgUuid.jpg",
            afterSync.images.first().remoteThumbnailUrl
        )
    }

    @Test
    fun importedAndPublicCacheSpotsAreNeverReturnedAsUnsynced() = runTest {
        repo.activeUid = "user-a"

        // 1. Own spot (unsynced)
        repo.saveSpot(
            spot = Spot(
                latitude = 45.46,
                longitude = 9.19,
                createdAt = 1000L,
                description = "My own spot",
                tags = emptyList(),
                category = "graffiti",
                status = "active",
                uuid = "own-1"
            ),
            imagePath = "",
            thumbnailPath = ""
        )

        // 2. Imported pack spot — seeded directly with isSynced = false and active ownerUid
        // to prove DAO filters by origin rather than relying on saveImportedSpot's isSynced = true
        db.spotDao().insertSpot(
            SpotEntity(
                latitude = 45.47,
                longitude = 9.20,
                createdAt = 1100L,
                description = "Imported spot",
                tags = emptyList(),
                category = "graffiti",
                status = "active",
                uuid = "imported-1",
                ownerUid = "user-a",
                parentPackId = "PACK01",
                isImported = true,
                isSynced = false,
                origin = SpotOrigin.IMPORTED.name
            )
        )

        // 3. Cached public spot — also seeded directly with isSynced = false and active ownerUid
        db.spotDao().insertSpot(
            SpotEntity(
                latitude = 45.48,
                longitude = 9.21,
                createdAt = 1200L,
                description = "Other user public spot",
                tags = emptyList(),
                category = "graffiti",
                status = "active",
                uuid = "public-1",
                ownerUid = "user-a",
                isPublic = true,
                isSynced = false,
                origin = SpotOrigin.PUBLIC_CACHE.name
            )
        )

        val unsynced = repo.getUnsyncedSpots()
        assertEquals(1, unsynced.size)
        assertEquals("own-1", unsynced.first().spot.uuid)
        assertEquals(SpotOrigin.OWN, unsynced.first().spot.origin)
    }

    @Test
    fun publicRefreshCannotOverwriteOwnOrImportedSpotsEvenWhenSignedOut() = runTest {
        repo.activeUid = "user-a"
        repo.saveSpot(
            spot = Spot(
                latitude = 45.46,
                longitude = 9.19,
                createdAt = 1000L,
                description = "My local edit",
                tags = emptyList(),
                category = "graffiti",
                status = "active",
                uuid = "own-public-1",
                isPublic = true
            ),
            imagePath = "",
            thumbnailPath = ""
        )

        // Also seed an imported pack spot before sign-out
        repo.saveImportedSpot(
            SpotDetails(
                spot = Spot(
                    latitude = 45.47,
                    longitude = 9.20,
                    createdAt = 1100L,
                    description = "Imported pack spot",
                    tags = emptyList(),
                    category = "graffiti",
                    status = "active",
                    uuid = "imported-pack-1",
                    parentPackId = "PACK99",
                    isImported = true
                ),
                images = emptyList(),
                notes = emptyList()
            )
        )

        // Sign out! activeUid becomes null
        repo.activeUid = null

        // Account A's public spot remains visible as a read-only public spot on the same device
        val signedOutSpots = repo.getAllSpots().first()
        val publicWhileSignedOut = signedOutSpots.find { it.spot.uuid == "own-public-1" }
        assertNotNull(publicWhileSignedOut)
        assertTrue(publicWhileSignedOut.spot.isOtherUsersPublic(null))
        assertFalse(publicWhileSignedOut.spot.isEditableBy(null))

        // Switching to another account on the same device also sees Account A's public spot as read-only
        repo.activeUid = "user-b"
        val userBSpots = repo.getAllSpots().first()
        val publicForUserB = userBSpots.find { it.spot.uuid == "own-public-1" }
        assertNotNull(publicForUserB)
        assertTrue(publicForUserB.spot.isOtherUsersPublic("user-b"))
        assertFalse(publicForUserB.spot.isEditableBy("user-b"))

        // Back to signed out for public refresh & prune guard checks
        repo.activeUid = null

        // Public refresh tries to save same UUIDs for both OWN and IMPORTED, then prunes
        repo.savePublicSpot(
            SpotDetails(
                spot = Spot(
                    latitude = 45.46,
                    longitude = 9.19,
                    createdAt = 1000L,
                    description = "Stale remote public copy",
                    tags = emptyList(),
                    category = "graffiti",
                    status = "active",
                    uuid = "own-public-1",
                    ownerUid = "user-a",
                    isPublic = true
                ),
                images = emptyList(),
                notes = emptyList()
            )
        )
        repo.savePublicSpot(
            SpotDetails(
                spot = Spot(
                    latitude = 45.47,
                    longitude = 9.20,
                    createdAt = 1100L,
                    description = "Public copy colliding with imported",
                    tags = emptyList(),
                    category = "graffiti",
                    status = "active",
                    uuid = "imported-pack-1",
                    ownerUid = "user-b",
                    isPublic = true
                ),
                images = emptyList(),
                notes = emptyList()
            )
        )
        repo.pruneRemovedPublicSpots(emptySet())

        // Re-sign in and verify both OWN and IMPORTED spots are intact
        repo.activeUid = "user-a"
        val spots = repo.getAllSpots().first()
        val own = spots.find { it.spot.uuid == "own-public-1" }
        assertNotNull(own)
        assertEquals("My local edit", own.spot.description)
        assertEquals(SpotOrigin.OWN, own.spot.origin)

        val imported = spots.find { it.spot.uuid == "imported-pack-1" }
        assertNotNull(imported)
        assertEquals("Imported pack spot", imported.spot.description)
        assertEquals(SpotOrigin.IMPORTED, imported.spot.origin)

        // Importing a shared pack containing user-a's own spot UUID must not convert OWN -> IMPORTED
        repo.saveImportedSpot(
            SpotDetails(
                spot = Spot(
                    latitude = 45.46,
                    longitude = 9.19,
                    createdAt = 1000L,
                    description = "Re-imported copy of own spot",
                    tags = emptyList(),
                    category = "graffiti",
                    status = "active",
                    uuid = "own-public-1",
                    parentPackId = "PACK99",
                    isImported = true
                ),
                images = emptyList(),
                notes = emptyList()
            )
        )
        val ownAfterPackImport = repo.getAllSpots().first().find { it.spot.uuid == "own-public-1" }!!
        assertEquals(SpotOrigin.OWN, ownAfterPackImport.spot.origin)
        assertEquals("My local edit", ownAfterPackImport.spot.description)
    }

    @Test
    fun publicSpotChildImageLookupIsScopedToParentSpot() = runTest {
        repo.activeUid = "user-a"
        val ownSpotId = repo.saveSpot(
            spot = Spot(
                latitude = 45.46,
                longitude = 9.19,
                createdAt = 1000L,
                description = "Own spot",
                tags = emptyList(),
                category = "graffiti",
                status = "active",
                uuid = "own-spot-uuid"
            ),
            imagePath = "/local/private.jpg",
            thumbnailPath = "/local/private_thumb.jpg"
        )
        val ownImageUuid = repo.getSpotById(ownSpotId).first()!!.images.first().uuid

        // Malicious or colliding public spot carries the exact same image UUID
        repo.savePublicSpot(
            SpotDetails(
                spot = Spot(
                    latitude = 48.85,
                    longitude = 2.35,
                    createdAt = 2000L,
                    description = "Public spot with colliding image UUID",
                    tags = emptyList(),
                    category = "graffiti",
                    status = "active",
                    uuid = "public-spot-uuid",
                    ownerUid = "user-b",
                    isPublic = true
                ),
                images = listOf(
                    SpotImage(
                        spotId = 0L,
                        imagePath = "https://example.com/public.jpg",
                        thumbnailPath = "https://example.com/public.jpg",
                        timestamp = 2000L,
                        uuid = ownImageUuid,
                        remoteThumbnailUrl = "https://example.com/public.jpg"
                    )
                ),
                notes = listOf(
                    SpotNote(
                        spotId = 0L,
                        noteText = "Public note",
                        timestamp = 2000L,
                        uuid = "note-uuid-1"
                    )
                )
            )
        )

        // Own spot must still retain its private image untouched
        val ownAfter = repo.getSpotById(ownSpotId).first()!!
        assertEquals(1, ownAfter.images.size)
        assertEquals("/local/private_thumb.jpg", ownAfter.images.first().thumbnailPath)
    }

    @Test
    fun publicSpotsCanBeFilteredByAuthorAndPreserveStarsOnRefresh() = runTest {
        repo.activeUid = "user-a"
        val aliceDetail = SpotDetails(
            spot = Spot(
                latitude = 45.46,
                longitude = 9.19,
                createdAt = 1000L,
                description = "Alice v1",
                tags = emptyList(),
                category = "graffiti",
                status = "active",
                uuid = "alice-1",
                ownerUid = "alice",
                isPublic = true
            ),
            images = emptyList(),
            notes = emptyList()
        )
        val bobDetail = SpotDetails(
            spot = Spot(
                latitude = 51.50,
                longitude = -0.12,
                createdAt = 2000L,
                description = "Bob v1",
                tags = emptyList(),
                category = "sculpture",
                status = "active",
                uuid = "bob-1",
                ownerUid = "bob",
                isPublic = true
            ),
            images = emptyList(),
            notes = emptyList()
        )

        repo.savePublicSpot(aliceDetail)
        repo.savePublicSpot(bobDetail)

        val aliceRow = repo.getPublicSpotsByAuthor("alice").first()
        assertEquals(1, aliceRow.size)
        assertEquals("alice-1", aliceRow.first().spot.uuid)

        // Star Alice's spot locally, then refresh it
        repo.updateSpotStarred(aliceRow.first().spot.id, true)
        repo.savePublicSpot(aliceDetail.copy(spot = aliceDetail.spot.copy(description = "Alice v2")))

        val refreshedAlice = repo.getPublicSpotsByAuthor("alice").first().first()
        assertEquals("Alice v2", refreshedAlice.spot.description)
        assertTrue(refreshedAlice.spot.isStarred)
        assertFalse(refreshedAlice.spot.isEditableBy("user-a"))
        assertFalse(refreshedAlice.spot.isDeletableBy("user-a"))
    }

    @Test
    fun publicRefreshHealsBrokenCrossDeviceImageOnSyncedOwnRowWithoutOverwritingOrigin() = runTest {
        repo.activeUid = "user-a"
        val spotId = repo.saveSpot(
            spot = Spot(
                latitude = 52.52,
                longitude = 13.37,
                createdAt = 1000L,
                description = "Cube",
                tags = emptyList(),
                category = "architecture",
                status = "active",
                uuid = "cube-uuid",
                isPublic = true,
                isSynced = true
            ),
            imagePath = "/data/user/0/missing.jpg",
            thumbnailPath = "/data/user/0/missing_thumb.jpg"
        )
        val imgUuid = repo.getSpotById(spotId).first()!!.images.first().uuid

        // Sign out on shared device (activeUid = null) and pull updated public spot with HTTPS URL
        repo.activeUid = null
        val remoteUrl = "https://firebasestorage.googleapis.com/v0/b/test/o/spots%2Fuser-a%2Fcube-uuid%2Fthumbnails%2F$imgUuid.jpg?alt=media"
        repo.savePublicSpot(
            SpotDetails(
                spot = Spot(
                    latitude = 52.52,
                    longitude = 13.37,
                    createdAt = 1000L,
                    description = "Cube remote",
                    tags = emptyList(),
                    category = "architecture",
                    status = "active",
                    uuid = "cube-uuid",
                    ownerUid = "user-a",
                    isPublic = true
                ),
                images = listOf(
                    SpotImage(
                        spotId = 0L,
                        imagePath = remoteUrl,
                        thumbnailPath = remoteUrl,
                        timestamp = 1000L,
                        uuid = imgUuid,
                        ownerUid = "user-a",
                        remoteThumbnailUrl = remoteUrl
                    )
                ),
                notes = emptyList()
            )
        )

        val healed = repo.getSpotById(spotId).first()!!
        assertEquals(SpotOrigin.OWN, healed.spot.origin)
        assertEquals("Cube", healed.spot.description)
        assertEquals(remoteUrl, healed.images.first().thumbnailPath)
        assertEquals(remoteUrl, healed.images.first().remoteThumbnailUrl)
    }
}
