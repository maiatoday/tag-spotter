package net.maiatoday.tagspotter.feature.detail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlin.test.BeforeTest
import kotlin.test.AfterTest


import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import app.cash.turbine.test
import kotlinx.coroutines.flow.first

import net.maiatoday.tagspotter.core.ai.AiSuggestion
import net.maiatoday.tagspotter.core.ai.FakeAiRecognitionService
import net.maiatoday.tagspotter.core.database.FakeSpotRepository
import net.maiatoday.tagspotter.core.location.WearSyncManager
import net.maiatoday.tagspotter.core.model.Spot
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.model.SpotImage
import net.maiatoday.tagspotter.core.settings.FakeSecretsProvider
import net.maiatoday.tagspotter.core.settings.FakeSettingsRepository
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val repository = FakeSpotRepository()
    private val settingsRepository = FakeSettingsRepository("Initial Photographer")
    private val aiRecognitionService = FakeAiRecognitionService()
    private val secretsProvider = FakeSecretsProvider()
    private val wearSyncManager = FakeWearSyncManager()
    private val syncManager = FakeSyncManager()

    @Test
    fun loadSpotDetailsAndUpdatesWorkCorrectly() = runTest {
        val spotId = 123L
        val spot = Spot(
            id = spotId,
            latitude = 12.34,
            longitude = 56.78,
            createdAt = 1000L,
            description = "Original Description",
            tags = listOf("tagA"),
            category = "graffiti",
            status = "active",
            artists = listOf("Artist A"),
            photographer = "Photographer A"
        )
        val spotDetails = SpotDetails(spot, emptyList(), emptyList())
        repository.setSpots(listOf(spotDetails))

        val viewModel = DetailViewModel(
            spotId,
            repository,
            settingsRepository,
            aiRecognitionService,
            secretsProvider,
            wearSyncManager,
            syncManager
        )

        // Collect StateFlows in backgroundScope to trigger WhileSubscribed updates
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.spotDetails.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.defaultPhotographer.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.recentCustomTags.collect {}
        }

        // Verify initial state
        assertEquals("Initial Photographer", viewModel.defaultPhotographer.value)
        val initialLoadedDetails = viewModel.spotDetails.value
        assertNotNull(initialLoadedDetails)
        assertEquals("Original Description", initialLoadedDetails.spot.description)
        assertEquals("active", initialLoadedDetails.spot.status)
        assertEquals(listOf("Artist A"), initialLoadedDetails.spot.artists)
        assertEquals("Photographer A", initialLoadedDetails.spot.photographer)

        // Update status
        viewModel.updateStatus("erased")
        assertEquals("erased", viewModel.spotDetails.value?.spot?.status)

        // Update artists
        viewModel.updateArtists(listOf("Artist B", "Artist C"))
        assertEquals(listOf("Artist B", "Artist C"), viewModel.spotDetails.value?.spot?.artists)

        // Update photographer
        viewModel.updatePhotographer("Photographer B")
        assertEquals("Photographer B", viewModel.spotDetails.value?.spot?.photographer)

        // Update description
        viewModel.updateDescription("New Description")
        assertEquals("New Description", viewModel.spotDetails.value?.spot?.description)

        // Update category
        viewModel.updateCategory("sculpture")
        assertEquals("sculpture", viewModel.spotDetails.value?.spot?.category)

        // Update location
        viewModel.updateLocation(43.21, 87.65)
        assertEquals(43.21, viewModel.spotDetails.value?.spot?.latitude)
        assertEquals(87.65, viewModel.spotDetails.value?.spot?.longitude)

        // Update tags
        viewModel.updateTags(listOf("tagB", "tagC"))
        assertEquals(listOf("tagB", "tagC"), viewModel.spotDetails.value?.spot?.tags)

        // Add note
        viewModel.addNote("Nice spot", 2000L)
        val notes = viewModel.spotDetails.value?.notes
        assertEquals(1, notes?.size)
        assertEquals("Nice spot", notes?.first()?.noteText)
        assertEquals(2000L, notes?.first()?.timestamp)

        // Delete note
        val noteId = notes?.first()?.id ?: 0L
        viewModel.deleteNote(noteId)
        assertEquals(0, viewModel.spotDetails.value?.notes?.size)

        // Add image
        viewModel.addImage("/path/to/image.png", "/path/to/thumbnail.png", 3000L)
        val images = viewModel.spotDetails.value?.images
        assertEquals(1, images?.size)
        assertEquals("/path/to/image.png", images?.first()?.imagePath)
        assertEquals("/path/to/thumbnail.png", images?.first()?.thumbnailPath)
        assertEquals(3000L, images?.first()?.timestamp)

        // Delete spot
        var deleted = false
        val currentDetails = viewModel.spotDetails.value!!
        viewModel.deleteSpot(currentDetails) {
            deleted = true
        }
        assertNull(viewModel.spotDetails.value)
        assertEquals(true, deleted)
    }

    @Test
    fun deleteImageWorksCorrectly() = runTest {
        val spotId = 124L
        val spot = Spot(
            id = spotId,
            latitude = 12.34,
            longitude = 56.78,
            createdAt = 1000L,
            description = "Original Description",
            tags = listOf("tagA"),
            category = "graffiti",
            status = "active"
        )
        val image1 = SpotImage(
            id = 1L,
            spotId = spotId,
            imagePath = "/path/1.png",
            thumbnailPath = "/path/1_thumb.png",
            timestamp = 1000L,
            isMain = true
        )
        val image2 = SpotImage(
            id = 2L,
            spotId = spotId,
            imagePath = "/path/2.png",
            thumbnailPath = "/path/2_thumb.png",
            timestamp = 1100L,
            isMain = false
        )
        val spotDetails = SpotDetails(spot, listOf(image1, image2), emptyList())
        repository.setSpots(listOf(spotDetails))

        val viewModel = DetailViewModel(
            spotId,
            repository,
            settingsRepository,
            aiRecognitionService,
            secretsProvider,
            wearSyncManager,
            syncManager
        )

        // Collect StateFlow in backgroundScope
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.spotDetails.collect {}
        }

        // Verify initially two images, image 1 is main
        assertEquals(2, viewModel.spotDetails.value?.images?.size)
        assertEquals(true, viewModel.spotDetails.value?.images?.find { it.id == 1L }?.isMain)

        // Delete the main image (image1)
        viewModel.deleteImage(image1)

        // Verify only 1 image remains, and it should be promoted to main
        assertEquals(1, viewModel.spotDetails.value?.images?.size)
        val remainingImage = viewModel.spotDetails.value?.images?.first()
        assertEquals(2L, remainingImage?.id)
        assertEquals(true, remainingImage?.isMain)
    }

    @Test
    fun starredToggleAndLimitExceeded() = runTest {
        val spotId = 1L
        val spot = Spot(
            id = spotId,
            latitude = 12.34,
            longitude = 56.78,
            createdAt = 1000L,
            description = "Test Spot",
            tags = emptyList(),
            category = "graffiti",
            status = "active",
            isStarred = false
        )
        val spotDetails = SpotDetails(spot, emptyList(), emptyList())
        repository.setSpots(listOf(spotDetails))

        val viewModel = DetailViewModel(
            spotId,
            repository,
            settingsRepository,
            aiRecognitionService,
            secretsProvider,
            wearSyncManager,
            syncManager
        )

        // Collect spotDetails StateFlow in backgroundScope
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.spotDetails.collect {}
        }

        // Initially not starred
        assertEquals(false, viewModel.spotDetails.value?.spot?.isStarred)

        // Toggle star to true
        viewModel.toggleStarred()
        assertEquals(true, viewModel.spotDetails.value?.spot?.isStarred)

        // Toggle star back to false
        viewModel.toggleStarred()
        assertEquals(false, viewModel.spotDetails.value?.spot?.isStarred)

        // Now mock 100 starred spots and try to star target spot
        val starredSpots = (1..100).map { i ->
            SpotDetails(
                Spot(
                    id = 1000L + i,
                    latitude = 0.0,
                    longitude = 0.0,
                    createdAt = 0L,
                    description = "",
                    tags = emptyList(),
                    category = "graffiti",
                    status = "active",
                    isStarred = true
                ),
                emptyList(),
                emptyList()
            )
        }
        repository.setSpots(starredSpots + spotDetails)

        var limitExceededEmitted = false
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvent.collect { event ->
                if (event is DetailViewModel.UiEvent.StarLimitExceeded) {
                    limitExceededEmitted = true
                }
            }
        }

        // Toggle starring (should fail due to 100-star limit)
        viewModel.toggleStarred()
        assertEquals(true, limitExceededEmitted)
        assertEquals(false, viewModel.spotDetails.value?.spot?.isStarred)
    }

    @Test
    fun artistRecognitionSettingPropagatedCorrectly() = runTest {
        val viewModel = DetailViewModel(
            -1L,
            repository,
            settingsRepository,
            aiRecognitionService,
            secretsProvider,
            wearSyncManager,
            syncManager
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.isArtistRecognitionEnabled.collect {}
        }

        // Default is true
        assertEquals(true, viewModel.isArtistRecognitionEnabled.value)

        // Toggle to false
        settingsRepository.updateArtistRecognitionEnabled(false)
        assertEquals(false, viewModel.isArtistRecognitionEnabled.value)
    }

    @Test
    fun aiAugmentationAvailabilityPropagatedCorrectly() = runTest {
        // 1. Clean/Reset shared mock states BEFORE instantiating DetailViewModel
        settingsRepository.updateArtistRecognitionEnabled(true)
        aiRecognitionService.isSupported = true

        val viewModel = DetailViewModel(
            -1L,
            repository,
            settingsRepository,
            aiRecognitionService,
            secretsProvider,
            wearSyncManager,
            syncManager
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.isAiAugmentationAvailable.collect {}
        }

        // 2. Verified initially true (enabled and service is supported)
        assertEquals(true, viewModel.isAiAugmentationAvailable.value)

        // 3. Disable in settings -> should be false
        settingsRepository.updateArtistRecognitionEnabled(false)
        assertEquals(false, viewModel.isAiAugmentationAvailable.value)

        // 4. Re-enable in settings but set service as unsupported -> should be false
        settingsRepository.updateArtistRecognitionEnabled(true)
        aiRecognitionService.isSupported = false
        // Trigger a settings change to force re-evaluation of the flow map
        settingsRepository.updateArtistRecognitionEnabled(false)
        settingsRepository.updateArtistRecognitionEnabled(true)
        assertEquals(false, viewModel.isAiAugmentationAvailable.value)
    }



    @Test
    fun identifyArtistSuccess() = runTest {
        val expectedSuggestion = AiSuggestion("Mocked Artist", "Mocked Title", listOf("stencil"))
        aiRecognitionService.identifyArtistResult = expectedSuggestion
        secretsProvider.apiKey = "valid_key"
        val viewModel = DetailViewModel(
            spotId = -1L,
            repository = repository,
            settingsRepository = settingsRepository,
            aiRecognitionService = aiRecognitionService,
            secretsProvider = secretsProvider,
            wearSyncManager = wearSyncManager,
            syncManager = syncManager
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.aiState.collect {}
        }

        viewModel.identifyArtist("some_path.png")

        assertEquals(AiState.Success(expectedSuggestion), viewModel.aiState.value)
    }

    @Test
    fun identifyArtistFailsOnException() = runTest {
        aiRecognitionService.identifyArtistException = RuntimeException("quota exceeded")
        secretsProvider.apiKey = "valid_key"
        val viewModel = DetailViewModel(
            spotId = -1L,
            repository = repository,
            settingsRepository = settingsRepository,
            aiRecognitionService = aiRecognitionService,
            secretsProvider = secretsProvider,
            wearSyncManager = wearSyncManager,
            syncManager = syncManager
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.aiState.collect {}
        }

        viewModel.identifyArtist("some_path.png")

        assertEquals(AiState.Error.QuotaExceeded, viewModel.aiState.value)
    }

    @Test
    fun identifyArtistPassesExistingArtistAndTitle() = runTest {
        val spotId = 123L
        val spot = Spot(
            id = spotId,
            latitude = 12.34,
            longitude = 56.78,
            createdAt = 1000L,
            description = "Original Description",
            tags = listOf("tagA"),
            category = "graffiti",
            status = "active",
            artists = listOf("Artist A"),
            photographer = "Photographer A"
        )
        val spotDetails = SpotDetails(spot, emptyList(), emptyList())
        repository.setSpots(listOf(spotDetails))

        secretsProvider.apiKey = "valid_key"
        val expectedSuggestion = AiSuggestion("Mocked Artist", "Mocked Title", listOf("stencil"))
        aiRecognitionService.identifyArtistResult = expectedSuggestion

        val viewModel = DetailViewModel(
            spotId = spotId,
            repository = repository,
            settingsRepository = settingsRepository,
            aiRecognitionService = aiRecognitionService,
            secretsProvider = secretsProvider,
            wearSyncManager = wearSyncManager,
            syncManager = syncManager
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.spotDetails.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.aiState.collect {}
        }

        // Let it load the spotDetails
        testScheduler.advanceUntilIdle()

        viewModel.identifyArtist("some_path.png")
        testScheduler.advanceUntilIdle()

        assertEquals("Artist A", aiRecognitionService.lastIdentifyCurrentArtist)
        assertEquals("Original Description", aiRecognitionService.lastIdentifyCurrentTitle)
        assertEquals("graffiti", aiRecognitionService.lastIdentifyCategory)
    }

    @Test
    fun identifyArtistPassesThumbnailPath() = runTest {
        val spotId = 123L
        val spot = Spot(
            id = spotId,
            latitude = 12.34,
            longitude = 56.78,
            createdAt = 1000L,
            description = "Original Description",
            tags = listOf("tagA"),
            category = "graffiti",
            status = "active",
            artists = emptyList(),
            photographer = "Photographer A"
        )
        val image = SpotImage(
            id = 1L,
            spotId = spotId,
            imagePath = "some_path.png",
            timestamp = 1000L,
            thumbnailPath = "some_thumb_path.png"
        )
        val spotDetails = SpotDetails(spot, listOf(image), emptyList())
        repository.setSpots(listOf(spotDetails))

        secretsProvider.apiKey = "valid_key"
        val expectedSuggestion = AiSuggestion("Mocked Artist", "Mocked Title", listOf("stencil"))
        aiRecognitionService.identifyArtistResult = expectedSuggestion

        val viewModel = DetailViewModel(
            spotId = spotId,
            repository = repository,
            settingsRepository = settingsRepository,
            aiRecognitionService = aiRecognitionService,
            secretsProvider = secretsProvider,
            wearSyncManager = wearSyncManager,
            syncManager = syncManager
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.spotDetails.collect {}
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.aiState.collect {}
        }

        // Let it load the spotDetails
        testScheduler.advanceUntilIdle()

        viewModel.identifyArtist("some_path.png")
        testScheduler.advanceUntilIdle()

        assertEquals("some_path.png", aiRecognitionService.lastIdentifyImagePath)
        assertEquals("some_thumb_path.png", aiRecognitionService.lastIdentifyThumbnailPath)
    }

    @Test
    fun searchWikipediaForSpotFailsWhenDescriptionIsEmpty() = runTest {
        val viewModel = DetailViewModel(
            spotId = -1L,
            repository = repository,
            settingsRepository = settingsRepository,
            aiRecognitionService = aiRecognitionService,
            secretsProvider = secretsProvider,
            wearSyncManager = wearSyncManager,
            syncManager = syncManager
        )

        viewModel.wikiSearchState.test {
            assertEquals(WikiSearchState.Idle, awaitItem())

            // Trigger Wikipedia search on spot with empty title
            viewModel.searchWikipediaForSpot()

            // Should return Error state indicating description is empty
            val state = awaitItem()
            assertTrue(state is WikiSearchState.Error)
            assertEquals(
                "No title logged. Please set a title/description first.",
                state.message
            )
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun searchWikipediaForSpotFailsWhenApiKeyIsMissing() = runTest {
        // Set up a draft spot details with a title/description
        val spotId = 125L
        val spot = Spot(
            id = spotId,
            latitude = 12.34,
            longitude = 56.78,
            createdAt = 1000L,
            description = "Some Spot Title",
            tags = emptyList(),
            category = "graffiti",
            status = "active"
        )
        val spotDetails = SpotDetails(spot, emptyList(), emptyList())
        repository.setSpots(listOf(spotDetails))

        settingsRepository.updateGeminiApiKey("")
        secretsProvider.apiKey = ""
        val viewModel = DetailViewModel(
            spotId = spotId,
            repository = repository,
            settingsRepository = settingsRepository,
            aiRecognitionService = aiRecognitionService,
            secretsProvider = secretsProvider,
            wearSyncManager = wearSyncManager,
            syncManager = syncManager
        )

        viewModel.wikiSearchState.test {
            assertEquals(WikiSearchState.Idle, awaitItem())

            // Trigger Wikipedia search
            viewModel.searchWikipediaForSpot()

            // Verification: should set error to missing key since API key is empty
            val state = awaitItem()
            assertTrue(state is WikiSearchState.Error)
            assertEquals(
                "Missing Gemini API Key. Please configure it in Settings.",
                state.message
            )
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun searchWikipediaForSpotSuccess() = runTest {
        val spotId = 126L
        val spot = Spot(
            id = spotId,
            latitude = 12.34,
            longitude = 56.78,
            createdAt = 1000L,
            description = "Some Spot Title",
            tags = emptyList(),
            category = "graffiti",
            status = "active"
        )
        val spotDetails = SpotDetails(spot, emptyList(), emptyList())
        repository.setSpots(listOf(spotDetails))

        aiRecognitionService.searchWikipediaResult = "https://en.wikipedia.org/wiki/Some_Spot_Title"
        secretsProvider.apiKey = "valid_key"
        val viewModel = DetailViewModel(
            spotId = spotId,
            repository = repository,
            settingsRepository = settingsRepository,
            aiRecognitionService = aiRecognitionService,
            secretsProvider = secretsProvider,
            wearSyncManager = wearSyncManager,
            syncManager = syncManager
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.spotDetails.collect {}
        }

        viewModel.searchWikipediaForSpot()

        assertEquals(
            WikiSearchState.Success(
                "https://en.wikipedia.org/wiki/Some_Spot_Title",
                "Some Spot Title"
            ), viewModel.wikiSearchState.value
        )
    }

    @Test
    fun resetWikiSearchStateResetsToIdle() = runTest {
        val viewModel = DetailViewModel(
            -1L,
            repository,
            settingsRepository,
            aiRecognitionService,
            secretsProvider,
            wearSyncManager = wearSyncManager,
            syncManager = syncManager
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.wikiSearchState.collect {}
        }

        // Search Wikipedia on draft spot (should fail due to empty description)
        viewModel.searchWikipediaForSpot()
        assertTrue(viewModel.wikiSearchState.value is WikiSearchState.Error)

        // Reset
        viewModel.resetWikiSearchState()
        assertEquals(WikiSearchState.Idle, viewModel.wikiSearchState.value)
    }

    @Test
    fun identifyArtist_ResponseStoppedException_setsSafetyBlocked() = runTest {
        class ResponseStoppedException : Exception("stopped")
        aiRecognitionService.identifyArtistException = ResponseStoppedException()
        val viewModel = DetailViewModel(-1L, repository, settingsRepository, aiRecognitionService, secretsProvider, wearSyncManager, syncManager)
        
        viewModel.identifyArtist("path")
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.aiState.value is AiState.Error.SafetyBlocked)
    }

    @Test
    fun identifyArtist_ServerException_setsQuotaExceeded() = runTest {
        class ServerException : Exception("quota")
        aiRecognitionService.identifyArtistException = ServerException()
        val viewModel = DetailViewModel(-1L, repository, settingsRepository, aiRecognitionService, secretsProvider, wearSyncManager, syncManager)
        
        viewModel.identifyArtist("path")
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.aiState.value is AiState.Error.QuotaExceeded)
    }

    @Test
    fun identifyArtist_InvalidAPIKeyException_setsInvalidKey() = runTest {
        class InvalidAPIKeyException : Exception("API key")
        aiRecognitionService.identifyArtistException = InvalidAPIKeyException()
        val viewModel = DetailViewModel(-1L, repository, settingsRepository, aiRecognitionService, secretsProvider, wearSyncManager, syncManager)
        
        viewModel.identifyArtist("path")
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.aiState.value is AiState.Error.InvalidKey)
    }

    @Test
    fun identifyArtist_noConnectionException_setsGenericError() = runTest {
        aiRecognitionService.identifyArtistException = Exception("Unable to resolve host")
        val viewModel = DetailViewModel(-1L, repository, settingsRepository, aiRecognitionService, secretsProvider, wearSyncManager, syncManager)
        
        viewModel.identifyArtist("path")
        testScheduler.advanceUntilIdle()

        val state = viewModel.aiState.value as AiState.Error.Generic
        assertTrue(state.message.contains("No internet connection"))
    }

    @Test
    fun draftModeUpdates_correctlyPropagatesToDraftDetails() = runTest {
        val viewModel = DetailViewModel(-1L, repository, settingsRepository, aiRecognitionService, secretsProvider, wearSyncManager, syncManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.spotDetails.collect {}
        }
        
        viewModel.updateStatus("archived")
        viewModel.updateCategory("sculpture")
        viewModel.updateArtists(listOf("Banksy"))
        viewModel.updatePhotographer("Me")
        viewModel.updateTags(listOf("street"))
        viewModel.updateLocation(10.0, 20.0)
        viewModel.updateDescription("Desc")
        viewModel.updateArtworkDate("2024")
        
        val spot = viewModel.spotDetails.value?.spot
        assertNotNull(spot)
        assertEquals("archived", spot.status)
        assertEquals("sculpture", spot.category)
        assertEquals(listOf("Banksy"), spot.artists)
        assertEquals("Me", spot.photographer)
        assertEquals(listOf("street"), spot.tags)
        assertEquals(10.0, spot.latitude)
        assertEquals(20.0, spot.longitude)
        assertEquals("Desc", spot.description)
        assertEquals("2024", spot.artworkDate)
    }

    @Test
    fun addAndDeleteImage_draftMode_worksCorrectly() = runTest {
        val viewModel = DetailViewModel(-1L, repository, settingsRepository, aiRecognitionService, secretsProvider, wearSyncManager, syncManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.spotDetails.collect {}
        }
        
        viewModel.addImage("p1", "t1", 100L)
        assertEquals(1, viewModel.spotDetails.value?.images?.size)
        
        val image = viewModel.spotDetails.value!!.images[0]
        assertEquals("p1", image.imagePath)
        
        viewModel.updateImageRating(image, 5L)
        assertEquals(5L, viewModel.spotDetails.value!!.images[0].rating)

        viewModel.deleteImage(image)
        assertTrue(viewModel.spotDetails.value!!.images.isEmpty())
    }

    @Test
    fun saveSpot_draftMode_savesMultipleImages() = runTest {
        val viewModel = DetailViewModel(-1L, repository, settingsRepository, aiRecognitionService, secretsProvider, wearSyncManager, syncManager)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.spotDetails.collect {}
        }
        
        viewModel.addImage("p1", "t1", 100L)
        viewModel.addImage("p2", "t2", 200L)
        
        var savedId: Long? = null
        viewModel.saveSpot { savedId = it }
        testScheduler.advanceUntilIdle()

        assertNotNull(savedId)
        val savedSpot = repository.getSpotById(savedId).first()
        assertNotNull(savedSpot)
        assertEquals(2, savedSpot.images.size)
    }
}

private class FakeWearSyncManager : WearSyncManager {
    override fun shareSpotToWatch(spotDetails: SpotDetails) {}
    override fun sendSpotPhoto(spotId: Long, imagePath: String) {}
}

private class FakeSyncManager : net.maiatoday.tagspotter.core.sync.SyncManager {
    override val isSyncing = kotlinx.coroutines.flow.MutableStateFlow(false)
    override suspend fun syncNow() {}
    override fun startRealtimeSync(userId: String) {}
    override fun stopRealtimeSync() {}
    override suspend fun deleteSpot(uuid: String) {}
    override suspend fun sharePack(title: String, description: String, authorName: String, spots: List<SpotDetails>): String = ""
    override suspend fun importPackByCode(code: String): net.maiatoday.tagspotter.core.model.SharedPack = error("not implemented")
    override suspend fun saveImportedPack(sharedPack: net.maiatoday.tagspotter.core.model.SharedPack) {}
}
