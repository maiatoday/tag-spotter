package net.maiatoday.tagspotter.core.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.maiatoday.tagspotter.core.database.SpotRepository
import net.maiatoday.tagspotter.core.model.LoadedPack
import net.maiatoday.tagspotter.core.model.SharedPack
import net.maiatoday.tagspotter.core.model.Spot
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.model.SpotImage
import net.maiatoday.tagspotter.core.model.SpotNote
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HybridSpotRepositoryTest {

    private fun createSpotDetail(
        uuid: String,
        lat: Double,
        lon: Double,
        isPublic: Boolean = true,
        localPath: String = "",
        remoteThumbnailUrl: String = ""
    ): SpotDetails {
        return SpotDetails(
            spot = Spot(
                uuid = uuid,
                latitude = lat,
                longitude = lon,
                createdAt = 1000L,
                description = "Spot $uuid",
                tags = listOf("graffiti"),
                category = "graffiti",
                status = "active",
                isPublic = isPublic
            ),
            images = if (localPath.isNotEmpty() || remoteThumbnailUrl.isNotEmpty()) {
                listOf(
                    SpotImage(
                        id = 1L,
                        spotId = 1L,
                        imagePath = localPath,
                        thumbnailPath = remoteThumbnailUrl,
                        timestamp = 1000L,
                        uuid = "img-$uuid"
                    )
                )
            } else {
                emptyList()
            },
            notes = emptyList()
        )
    }

    private class TestSpotRepository(
        initialSpots: List<SpotDetails>
    ) : SpotRepository {
        val spotsList = initialSpots.toMutableList()
        override var activeUid: String? = null
        override fun getAllSpots(): Flow<List<SpotDetails>> = flowOf(spotsList)
        override fun getSpotsByCategory(category: String): Flow<List<SpotDetails>> = flowOf(emptyList())
        override fun getSpotById(id: Long): Flow<SpotDetails?> = flowOf(spotsList.find { it.spot.id == id })
        override suspend fun saveSpot(spot: Spot, imagePath: String, thumbnailPath: String, rating: Long, isMain: Boolean): Long {
            val id = if (spot.id != 0L) spot.id else (spotsList.size + 1).toLong()
            val saved = spot.copy(id = id)
            spotsList.add(
                SpotDetails(
                    spot = saved,
                    images = if (imagePath.isNotEmpty() || thumbnailPath.isNotEmpty()) {
                        listOf(SpotImage(id = 1L, spotId = id, imagePath = imagePath, thumbnailPath = thumbnailPath, timestamp = 1000L))
                    } else emptyList(),
                    notes = emptyList()
                )
            )
            return id
        }
        override suspend fun addImageToSpot(spotId: Long, imagePath: String, thumbnailPath: String, timestamp: Long, rating: Long, isMain: Boolean): Long = 0L
        override suspend fun addNoteToSpot(spotId: Long, noteText: String, timestamp: Long): Long = 0L
        override suspend fun updateSpotStatus(spotId: Long, status: String) {}
        override suspend fun updateSpotCategory(spotId: Long, category: String) {}
        override suspend fun updateSpotArtists(spotId: Long, artists: List<String>) {}
        override suspend fun updateSpotPhotographer(spotId: Long, photographer: String) {}
        override suspend fun updateSpotTags(spotId: Long, tags: List<String>) {}
        override suspend fun updateSpotLocation(spotId: Long, latitude: Double, longitude: Double) {}
        override suspend fun updateSpotDescription(spotId: Long, description: String) {}
        override suspend fun deleteSpot(spotDetails: SpotDetails) {}
        override fun getRecentCustomTags(predefinedTags: Set<String>): Flow<List<String>> = flowOf(emptyList())
        override suspend fun loadTestData() {}
        override suspend fun unloadTestData() {}
        override suspend fun saveSpotDetails(spotDetails: SpotDetails): Long = 0L
        override suspend fun importSpots(spots: List<SpotDetails>): Int = 0
        override suspend fun importPack(packFilePath: String, filesDir: String, cacheDir: String, currentPhotographerName: String, createThumbnail: suspend (String) -> String?): Int = 0
        override suspend fun updateSpotStarred(spotId: Long, isStarred: Boolean) {
            val idx = spotsList.indexOfFirst { it.spot.id == spotId }
            if (idx >= 0) {
                spotsList[idx] = spotsList[idx].copy(spot = spotsList[idx].spot.copy(isStarred = isStarred))
            }
        }
        override suspend fun updateSpotPublic(spotId: Long, isPublic: Boolean) {}
        override suspend fun getStarredSpots(): List<Spot> = emptyList()
        override suspend fun getStarredSpotsCount(): Int = 0
        override suspend fun setMainImage(spotId: Long, imageId: Long) {}
        override suspend fun deleteImage(image: SpotImage) {}
        override suspend fun updateImageRating(imageId: Long, rating: Long) {}
        override suspend fun updateSpotArtworkDate(spotId: Long, artworkDate: String) {}
        override suspend fun deleteNote(noteId: Long) {}
        override suspend fun updateNote(noteId: Long, noteText: String) {}
        override suspend fun getUnsyncedSpots(): List<SpotDetails> = emptyList()
        override suspend fun markSpotAsSynced(spotUuid: String) {}
        override suspend fun saveSyncedSpot(spotDetails: SpotDetails) {}
        override suspend fun adoptLocalSpots(userUid: String, backup: Boolean) {}
        override suspend fun clearUserCache(userUid: String) {}
        override suspend fun saveImportedSpot(spotDetails: SpotDetails) {
            val id = if (spotDetails.spot.id != 0L) spotDetails.spot.id else (spotsList.size + 1).toLong()
            spotsList.add(spotDetails.copy(spot = spotDetails.spot.copy(id = id)))
        }
        override fun getAllLoadedPacks(): Flow<List<LoadedPack>> = flowOf(emptyList())
        override suspend fun saveLoadedPack(pack: LoadedPack) {}
        override suspend fun deleteLoadedPack(packId: String) {}
    }

    private class TestSyncManager(
        private val publicSpots: List<SpotDetails>
    ) : SyncManager {
        override val isSyncing: StateFlow<Boolean> = MutableStateFlow(false)
        override suspend fun syncNow() {}
        override fun startRealtimeSync(userId: String) {}
        override fun stopRealtimeSync() {}
        override suspend fun deleteSpot(uuid: String) {}
        override suspend fun sharePack(title: String, description: String, authorName: String, spots: List<SpotDetails>): String = ""
        override suspend fun importPackByCode(code: String): SharedPack = SharedPack("", "", "", "", emptyList())
        override suspend fun saveImportedPack(sharedPack: SharedPack) {}

        override suspend fun fetchPublicSpots(
            minLat: Double,
            maxLat: Double,
            minLon: Double,
            maxLon: Double
        ): List<SpotDetails> {
            return publicSpots.filter { detail ->
                detail.spot.latitude in minLat..maxLat &&
                detail.spot.longitude in minLon..maxLon
            }
        }
    }

    @Test
    fun testGetNearbySpotsMergesAndDeduplicates() = runTest {
        // Base coordinate: Milan Duomo (45.4642, 9.1900)
        val localSpot = createSpotDetail(
            uuid = "spot-local-only",
            lat = 45.4640,
            lon = 9.1910,
            localPath = "/local/path/1.jpg"
        )
        val duplicateLocal = createSpotDetail(
            uuid = "spot-shared",
            lat = 45.4645,
            lon = 9.1905,
            localPath = "/local/path/shared.jpg",
            remoteThumbnailUrl = ""
        )
        val duplicateRemote = createSpotDetail(
            uuid = "spot-shared",
            lat = 45.4645,
            lon = 9.1905,
            localPath = "",
            remoteThumbnailUrl = "https://storage.googleapis.com/public_spots/shared.jpg"
        )
        val remoteSpot = createSpotDetail(
            uuid = "spot-remote-only",
            lat = 45.4650,
            lon = 9.1920,
            remoteThumbnailUrl = "https://storage.googleapis.com/public_spots/remote.jpg"
        )
        val farSpot = createSpotDetail(
            uuid = "spot-far",
            lat = 48.8566,
            lon = 2.3522 // Paris (> 600km away)
        )

        val localRepo = TestSpotRepository(listOf(localSpot, duplicateLocal))
        val syncManager = TestSyncManager(listOf(duplicateRemote, remoteSpot, farSpot))

        val hybridRepo = HybridSpotRepository(localRepo, syncManager)

        val nearby = hybridRepo.getNearbySpots(
            latitude = 45.4642,
            longitude = 9.1900,
            radiusKm = 2.0
        )

        // Must contain local-only, shared (deduplicated), and remote-only; must exclude far spot
        assertEquals(3, nearby.size)
        val uuids = nearby.map { it.spot.uuid }.toSet()
        assertTrue(uuids.contains("spot-local-only"))
        assertTrue(uuids.contains("spot-shared"))
        assertTrue(uuids.contains("spot-remote-only"))

        // For the deduplicated spot, verify it preserves the local imagePath and adopted remote thumbnail URL
        val sharedResult = nearby.first { it.spot.uuid == "spot-shared" }
        assertEquals("/local/path/shared.jpg", sharedResult.images.first().imagePath)
        assertEquals("https://storage.googleapis.com/public_spots/shared.jpg", sharedResult.images.first().thumbnailPath)
    }

    @Test
    fun testGetSpotByUuid() = runTest {
        val localSpot = createSpotDetail(uuid = "local-1", lat = 45.0, lon = 9.0)
        val remoteSpot = createSpotDetail(uuid = "remote-1", lat = 46.0, lon = 10.0)

        val localRepo = TestSpotRepository(listOf(localSpot))
        val syncManager = TestSyncManager(listOf(remoteSpot))

        val hybridRepo = HybridSpotRepository(localRepo, syncManager)

        val foundLocal = hybridRepo.getSpotByUuid("local-1")
        assertNotNull(foundLocal)
        assertEquals("local-1", foundLocal.spot.uuid)

        val foundRemote = hybridRepo.getSpotByUuid("remote-1")
        assertNotNull(foundRemote)
        assertEquals("remote-1", foundRemote.spot.uuid)

        val notFound = hybridRepo.getSpotByUuid("unknown")
        assertNull(notFound)
    }

    @Test
    fun testSaveSpotDelegatesToLocalRepository() = runTest {
        val localRepo = TestSpotRepository(emptyList())
        val syncManager = TestSyncManager(emptyList())
        val hybridRepo = HybridSpotRepository(localRepo, syncManager)

        val spot = Spot(
            uuid = "new-uuid-1",
            latitude = 52.5200,
            longitude = 13.4050,
            createdAt = 12345L,
            description = "Berlin Stencil",
            tags = listOf("stencil", "graffiti"),
            category = "graffiti",
            status = "active"
        )

        val savedId = hybridRepo.saveSpot(spot, imagePath = "/img/1.jpg", thumbnailPath = "/thumb/1.jpg")
        assertTrue(savedId > 0)

        val found = hybridRepo.getSpotByUuid("new-uuid-1")
        assertNotNull(found)
        assertEquals("Berlin Stencil", found.spot.description)
        assertEquals("/img/1.jpg", found.images.first().imagePath)
    }

    @Test
    fun testToggleSpotStarLocalSpot() = runTest {
        val localSpot = createSpotDetail(uuid = "star-local", lat = 45.0, lon = 9.0)
        val localRepo = TestSpotRepository(listOf(localSpot))
        val syncManager = TestSyncManager(emptyList())
        val hybridRepo = HybridSpotRepository(localRepo, syncManager)

        val newStar = hybridRepo.toggleSpotStar("star-local")
        assertTrue(newStar)

        val updated = hybridRepo.getSpotByUuid("star-local")
        assertNotNull(updated)
        assertTrue(updated.spot.isStarred)

        val toggledAgain = hybridRepo.toggleSpotStar("star-local")
        kotlin.test.assertFalse(toggledAgain)
    }

    @Test
    fun testToggleSpotStarRemoteSpotImportsAndStars() = runTest {
        val remoteSpot = createSpotDetail(uuid = "star-remote", lat = 46.0, lon = 10.0)
        val localRepo = TestSpotRepository(emptyList())
        val syncManager = TestSyncManager(listOf(remoteSpot))
        val hybridRepo = HybridSpotRepository(localRepo, syncManager)

        val newStar = hybridRepo.toggleSpotStar("star-remote")
        assertTrue(newStar)

        val imported = hybridRepo.getSpotByUuid("star-remote")
        assertNotNull(imported)
        assertTrue(imported.spot.isStarred)
    }
}
