package net.maiatoday.tagspotter.ui.viewmodel

import android.graphics.Bitmap
import net.maiatoday.tagspotter.MainDispatcherRule
import net.maiatoday.tagspotter.data.FakeSettingsRepository
import net.maiatoday.tagspotter.data.FakeSpotRepository
import net.maiatoday.tagspotter.data.Spot
import net.maiatoday.tagspotter.data.SpotDetails
import net.maiatoday.tagspotter.data.SpotImage
import net.maiatoday.tagspotter.domain.AiRecognitionService
import net.maiatoday.tagspotter.domain.AiSuggestion
import net.maiatoday.tagspotter.domain.PhotoMetadata
import net.maiatoday.tagspotter.domain.PhotoProcessor
import net.maiatoday.tagspotter.domain.TempFileDetails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class FakePhotoProcessor : PhotoProcessor {
    override suspend fun saveImageToPublicGallery(filePath: String): String? = null
    override suspend fun createThumbnailFromFile(filePath: String): String? = null
    override suspend fun createThumbnailFromUri(uriString: String): String? = null
    override suspend fun extractMetadataFromUri(uriString: String): PhotoMetadata? = null
    override fun createTempCameraFile(): TempFileDetails = TempFileDetails("", "")
    override fun deleteFile(filePath: String): Boolean = false
    override suspend fun decodeScaledBitmap(imagePath: String, maxDimension: Int): Bitmap? = null
}

class FakeAiRecognitionService : AiRecognitionService {
    var identifyArtistResult: AiSuggestion? = null
    var identifyArtistException: Exception? = null
    var searchWikipediaResult: String? = null
    var searchWikipediaException: Exception? = null

    override suspend fun identifyArtist(imagePath: String, apiKey: String, category: String): AiSuggestion? {
        identifyArtistException?.let { throw it }
        return identifyArtistResult
    }

    override suspend fun searchWikipediaForSpot(title: String, apiKey: String): String? {
        searchWikipediaException?.let { throw it }
        return searchWikipediaResult
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSpotRepository()
    private val settingsRepository = FakeSettingsRepository("Initial Photographer")
    private val aiRecognitionService = FakeAiRecognitionService()
    private val photoProcessor = FakePhotoProcessor()

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

        val viewModel = DetailViewModel(spotId, repository, settingsRepository, aiRecognitionService, photoProcessor)

        // Collect StateFlows in backgroundScope to trigger WhileSubscribed updates
        val collectJobDetails = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.spotDetails.collect {}
        }
        val collectJobPhotographer = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.defaultPhotographer.collect {}
        }
        val collectJobRecentTags = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.recentCustomTags.collect {}
        }

        // Verify initial state
        assertEquals("Initial Photographer", viewModel.defaultPhotographer.value)
        val initialLoadedDetails = viewModel.spotDetails.value
        assertNotNull(initialLoadedDetails)
        assertEquals("Original Description", initialLoadedDetails?.spot?.description)
        assertEquals("active", initialLoadedDetails?.spot?.status)
        assertEquals(listOf("Artist A"), initialLoadedDetails?.spot?.artists)
        assertEquals("Photographer A", initialLoadedDetails?.spot?.photographer)

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
        val image1 = SpotImage(id = 1L, spotId = spotId, imagePath = "/path/1.png", thumbnailPath = "/path/1_thumb.png", timestamp = 1000L, isMain = true)
        val image2 = SpotImage(id = 2L, spotId = spotId, imagePath = "/path/2.png", thumbnailPath = "/path/2_thumb.png", timestamp = 1100L, isMain = false)
        val spotDetails = SpotDetails(spot, listOf(image1, image2), emptyList())
        repository.setSpots(listOf(spotDetails))

        val viewModel = DetailViewModel(spotId, repository, settingsRepository, aiRecognitionService, photoProcessor)

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

        val viewModel = DetailViewModel(spotId, repository, settingsRepository, aiRecognitionService, photoProcessor)

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
        val collectEventsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
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
        val viewModel = DetailViewModel(-1L, repository, settingsRepository, aiRecognitionService, photoProcessor)

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
    fun identifyArtistFailsWhenApiKeyIsMissing() = runTest {
        settingsRepository.updateGeminiApiKey("")
        
        val viewModel = DetailViewModel(
            spotId = -1L,
            repository = repository,
            settingsRepository = settingsRepository,
            aiRecognitionService = aiRecognitionService,
            photoProcessor = photoProcessor,
            buildConfigApiKey = ""
        )
        
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.aiState.collect {}
        }
        
        assertEquals(AiState.Idle, viewModel.aiState.value)
        
        // Trigger identification with a dummy path
        viewModel.identifyArtist("some_path.png")
        
        // Verification: should set error to MissingKey since API Key is empty
        assertEquals(AiState.Error.MissingKey, viewModel.aiState.value)
    }

    @Test
    fun identifyArtistSuccess() = runTest {
        val expectedSuggestion = AiSuggestion("Mocked Artist", "Mocked Title", listOf("stencil"))
        aiRecognitionService.identifyArtistResult = expectedSuggestion

        val viewModel = DetailViewModel(
            spotId = -1L,
            repository = repository,
            settingsRepository = settingsRepository,
            aiRecognitionService = aiRecognitionService,
            photoProcessor = photoProcessor,
            buildConfigApiKey = "valid_key"
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

        val viewModel = DetailViewModel(
            spotId = -1L,
            repository = repository,
            settingsRepository = settingsRepository,
            aiRecognitionService = aiRecognitionService,
            photoProcessor = photoProcessor,
            buildConfigApiKey = "valid_key"
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.aiState.collect {}
        }

        viewModel.identifyArtist("some_path.png")

        assertEquals(AiState.Error.QuotaExceeded, viewModel.aiState.value)
    }

    @Test
    fun searchWikipediaForSpotFailsWhenDescriptionIsEmpty() = runTest {
        val viewModel = DetailViewModel(
            spotId = -1L,
            repository = repository,
            settingsRepository = settingsRepository,
            aiRecognitionService = aiRecognitionService,
            photoProcessor = photoProcessor
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.wikiSearchState.collect {}
        }

        assertEquals(WikiSearchState.Idle, viewModel.wikiSearchState.value)

        // Trigger Wikipedia search on spot with empty title
        viewModel.searchWikipediaForSpot()

        // Should return Error state indicating description is empty
        val state = viewModel.wikiSearchState.value
        assert(state is WikiSearchState.Error)
        assertEquals("No title logged. Please set a title/description first.", (state as WikiSearchState.Error).message)
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

        val viewModel = DetailViewModel(
            spotId = spotId,
            repository = repository,
            settingsRepository = settingsRepository,
            aiRecognitionService = aiRecognitionService,
            photoProcessor = photoProcessor,
            buildConfigApiKey = ""
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.spotDetails.collect {}
            viewModel.wikiSearchState.collect {}
        }

        assertEquals(WikiSearchState.Idle, viewModel.wikiSearchState.value)

        // Trigger Wikipedia search
        viewModel.searchWikipediaForSpot()

        // Verification: should set error to missing key since API key is empty
        val state = viewModel.wikiSearchState.value
        assert(state is WikiSearchState.Error)
        assertEquals("Missing Gemini API Key. Please configure it in Settings.", (state as WikiSearchState.Error).message)
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

        val viewModel = DetailViewModel(
            spotId = spotId,
            repository = repository,
            settingsRepository = settingsRepository,
            aiRecognitionService = aiRecognitionService,
            photoProcessor = photoProcessor,
            buildConfigApiKey = "valid_key"
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.spotDetails.collect {}
            viewModel.wikiSearchState.collect {}
        }

        viewModel.searchWikipediaForSpot()

        assertEquals(WikiSearchState.Success("https://en.wikipedia.org/wiki/Some_Spot_Title", "Some Spot Title"), viewModel.wikiSearchState.value)
    }

    @Test
    fun resetWikiSearchStateResetsToIdle() = runTest {
        val viewModel = DetailViewModel(-1L, repository, settingsRepository, aiRecognitionService, photoProcessor)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.wikiSearchState.collect {}
        }

        // Search Wikipedia on draft spot (should fail due to empty description)
        viewModel.searchWikipediaForSpot()
        assert(viewModel.wikiSearchState.value is WikiSearchState.Error)

        // Reset
        viewModel.resetWikiSearchState()
        assertEquals(WikiSearchState.Idle, viewModel.wikiSearchState.value)
    }
}
