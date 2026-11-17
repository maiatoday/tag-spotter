package net.maiatoday.tagspotter.core.agent

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import net.maiatoday.tagspotter.core.database.SpotRepository
import net.maiatoday.tagspotter.core.model.LoadedPack
import net.maiatoday.tagspotter.core.model.SharedPack
import net.maiatoday.tagspotter.core.model.Spot
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.model.SpotImage
import net.maiatoday.tagspotter.core.sync.HybridSpotRepository
import net.maiatoday.tagspotter.core.sync.SyncManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StreetArtToolsTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun createSpot(
        uuid: String,
        title: String,
        artists: List<String>,
        lat: Double,
        lon: Double,
        tags: List<String> = listOf("mural", "graffiti"),
        category: String = "graffiti"
    ): SpotDetails {
        return SpotDetails(
            spot = Spot(
                uuid = uuid,
                latitude = lat,
                longitude = lon,
                createdAt = 1000L,
                description = title,
                tags = tags,
                category = category,
                status = "active",
                artists = artists,
                isPublic = true
            ),
            images = listOf(
                SpotImage(
                    id = 1L,
                    spotId = 1L,
                    imagePath = "/data/images/$uuid.jpg",
                    thumbnailPath = "https://images.streetart/$uuid-thumb.jpg",
                    timestamp = 1000L,
                    uuid = "img-$uuid"
                )
            ),
            notes = emptyList()
        )
    }

    private class FakeSpotRepository(
        private val spots: List<SpotDetails>
    ) : SpotRepository {
        val savedSpots = mutableListOf<Spot>()
        override var activeUid: String? = null
        override fun getAllSpots(): Flow<List<SpotDetails>> = flowOf(spots)
        override fun getSpotsByCategory(category: String): Flow<List<SpotDetails>> = flowOf(emptyList())
        override fun getSpotById(id: Long): Flow<SpotDetails?> = flowOf(null)
        override suspend fun saveSpot(spot: Spot, imagePath: String, thumbnailPath: String, rating: Long, isMain: Boolean): Long {
            savedSpots.add(spot)
            return savedSpots.size.toLong()
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
        override suspend fun updateSpotStarred(spotId: Long, isStarred: Boolean) {}
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
        override suspend fun saveImportedSpot(spotDetails: SpotDetails) {}
        override fun getAllLoadedPacks(): Flow<List<LoadedPack>> = flowOf(emptyList())
        override suspend fun saveLoadedPack(pack: LoadedPack) {}
        override suspend fun deleteLoadedPack(packId: String) {}
    }

    private class FakeSyncManager(
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
        override suspend fun fetchPublicSpots(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): List<SpotDetails> = publicSpots
    }

    private val sampleSpots = listOf(
        createSpot("spot-1", "Neon Cyber Panther", listOf("Inkie"), -33.9249, 18.4241, listOf("stencil", "neon")),
        createSpot("spot-2", "District Six Memories", listOf("Falko One"), -33.9260, 18.4250, listOf("elephant", "heritage")),
        createSpot("spot-3", "Geometric Sunset", listOf("Inkie", "Faith47"), -33.9270, 18.4260, listOf("geometric", "abstract")),
        createSpot("spot-4", "Out of Town Far Away", listOf("Mak1one"), -34.5000, 19.5000, listOf("abstract"))
    )

    private fun createTools(): StreetArtTools {
        val repo = HybridSpotRepository(FakeSpotRepository(sampleSpots), FakeSyncManager(emptyList()))
        return StreetArtTools(repo)
    }

    @Test
    fun getNearbySpots_filtersByRadiusAndReturnsFormattedJson() = runTest {
        val tools = createTools()
        val jsonString = tools.getNearbySpots(-33.9250, 18.4240, radiusMeters = 500.0)
        val spots = json.decodeFromString<List<SpotSummaryDto>>(jsonString)

        assertTrue(spots.isNotEmpty())
        assertEquals(3, spots.size)
        assertTrue(spots.none { it.uuid == "spot-4" })
        assertEquals("Neon Cyber Panther", spots[0].title)
        assertEquals("Inkie", spots[0].artist)
        assertEquals("https://images.streetart/spot-1-thumb.jpg", spots[0].imageUrl)
    }

    @Test
    fun generateWalkingTour_createsSequencedStops() = runTest {
        val tools = createTools()
        val jsonString = tools.generateWalkingTour(-33.9250, 18.4240, maxStops = 2, maxMinutes = 30)
        val tour = json.decodeFromString<TourPlanDto>(jsonString)

        assertEquals("Urban Street Art Discovery", tour.tourTitle)
        assertEquals(30, tour.estimatedDurationMinutes)
        assertEquals(2, tour.stops.size)
        assertEquals(1, tour.stops[0].stopOrder)
        assertEquals(2, tour.stops[1].stopOrder)
        assertTrue(tour.totalDistanceMeters >= 400)
    }

    @Test
    fun getArtistBio_aggregatesMuralCountAndStyles() = runTest {
        val tools = createTools()
        val jsonString = tools.getArtistBio("Inkie")
        val bio = json.decodeFromString<ArtistBioDto>(jsonString)

        assertEquals("Inkie", bio.artistName)
        assertEquals(2, bio.muralCount)
        assertTrue(bio.knownStyles.contains("neon"))
        assertTrue(bio.knownStyles.contains("stencil"))
        assertTrue(bio.notableMurals.contains("Neon Cyber Panther"))
    }

    @Test
    fun identifyMuralByPhoto_matchesKeywordsSuccessfully() = runTest {
        val tools = createTools()
        val jsonString = tools.identifyMuralByPhoto("I see an elephant and memories of district six")
        val result = json.decodeFromString<MuralIdentificationDto>(jsonString)

        assertTrue(result.matched)
        assertEquals("spot-2", result.spotId)
        assertEquals("Falko One", result.artist)
        assertTrue(result.confidenceScore > 0.0)
    }

    @Test
    fun identifyMuralByPhoto_returnsUnmatchedWhenNoKeywordsFound() = runTest {
        val tools = createTools()
        val jsonString = tools.identifyMuralByPhoto("flying spaceship in outer space with purple aliens")
        val result = json.decodeFromString<MuralIdentificationDto>(jsonString)

        assertFalse(result.matched)
        assertEquals(0.0, result.confidenceScore)
    }

    @Test
    fun saveNewSpot_validCategory_savesSpotAndReturnsConfirmation() = runTest {
        val fakeRepo = FakeSpotRepository(emptyList())
        val hybridRepo = HybridSpotRepository(fakeRepo, FakeSyncManager(emptyList()))
        val tools = StreetArtTools(hybridRepo)

        val jsonString = tools.saveNewSpot(
            title = "Bronze Panther Statue",
            category = "sculpture",
            latitude = 52.5200,
            longitude = 13.4050,
            artist = "Inkie",
            description = "A striking bronze panther",
            tags = listOf("metal", "outdoor"),
            imageUrl = "/images/panther.jpg"
        )
        val confirmation = json.decodeFromString<SavedSpotConfirmationDto>(jsonString)

        assertTrue(confirmation.isSaved)
        assertEquals("Bronze Panther Statue", confirmation.title)
        assertEquals("sculpture", confirmation.category)
        assertTrue(confirmation.uuid.isNotBlank())

        assertEquals(1, fakeRepo.savedSpots.size)
        val saved = fakeRepo.savedSpots.first()
        assertTrue(saved.description.contains("Bronze Panther Statue"))
        assertTrue(saved.description.contains("A striking bronze panther"))
        assertEquals("sculpture", saved.category)
        assertEquals(listOf("Inkie"), saved.artists)
        assertEquals(listOf("metal", "outdoor"), saved.tags)
    }

    @Test
    fun saveNewSpot_streetArtStyle_mapsToGraffitiAndAppendsToTags() = runTest {
        val fakeRepo = FakeSpotRepository(emptyList())
        val hybridRepo = HybridSpotRepository(fakeRepo, FakeSyncManager(emptyList()))
        val tools = StreetArtTools(hybridRepo)

        val jsonString = tools.saveNewSpot(
            title = "Cosmic Alley Piece",
            category = "mural",
            latitude = 52.5100,
            longitude = 13.4000,
            artist = "Falko One",
            tags = listOf("colorful", "elephant")
        )
        val confirmation = json.decodeFromString<SavedSpotConfirmationDto>(jsonString)

        assertTrue(confirmation.isSaved)
        assertEquals("Cosmic Alley Piece", confirmation.title)
        assertEquals("graffiti", confirmation.category)

        assertEquals(1, fakeRepo.savedSpots.size)
        val saved = fakeRepo.savedSpots.first()
        assertEquals("graffiti", saved.category)
        assertTrue(saved.tags.contains("mural"))
        assertTrue(saved.tags.contains("elephant"))
    }

    @Test
    fun toTools_generatesExpectedAdkTools() {
        val tools = createTools()
        val adkTools = tools.toTools()

        assertEquals(5, adkTools.size)
        val toolNames = adkTools.map { it.name }.toSet()
        assertTrue(toolNames.contains("getNearbySpots"))
        assertTrue(toolNames.contains("generateWalkingTour"))
        assertTrue(toolNames.contains("getArtistBio"))
        assertTrue(toolNames.contains("identifyMuralByPhoto"))
        assertTrue(toolNames.contains("saveNewSpot"))
    }
}
