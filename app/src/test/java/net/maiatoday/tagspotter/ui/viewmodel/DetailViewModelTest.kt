package net.maiatoday.tagspotter.ui.viewmodel

import net.maiatoday.tagspotter.MainDispatcherRule
import net.maiatoday.tagspotter.data.FakeSettingsRepository
import net.maiatoday.tagspotter.data.FakeSpotRepository
import net.maiatoday.tagspotter.data.Spot
import net.maiatoday.tagspotter.data.SpotDetails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

import net.maiatoday.tagspotter.ui.viewmodel.AiState
import net.maiatoday.tagspotter.ui.viewmodel.AiSuggestion

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSpotRepository()
    private val settingsRepository = FakeSettingsRepository("Initial Photographer")

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

        val viewModel = DetailViewModel(spotId, repository, settingsRepository)

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

        val viewModel = DetailViewModel(spotId, repository, settingsRepository)

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
        val viewModel = DetailViewModel(-1L, repository, settingsRepository)

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
}
