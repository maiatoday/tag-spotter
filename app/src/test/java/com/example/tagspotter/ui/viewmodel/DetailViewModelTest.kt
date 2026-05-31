package com.example.tagspotter.ui.viewmodel

import com.example.tagspotter.MainDispatcherRule
import com.example.tagspotter.data.FakeSettingsRepository
import com.example.tagspotter.data.FakeSpotRepository
import com.example.tagspotter.data.Spot
import com.example.tagspotter.data.SpotDetails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

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

        // Update location
        viewModel.updateLocation(43.21, 87.65)
        assertEquals(43.21, viewModel.spotDetails.value?.spot?.latitude)
        assertEquals(87.65, viewModel.spotDetails.value?.spot?.longitude)

        // Add note
        viewModel.addNote("Nice spot", 2000L)
        val notes = viewModel.spotDetails.value?.notes
        assertEquals(1, notes?.size)
        assertEquals("Nice spot", notes?.first()?.noteText)
        assertEquals(2000L, notes?.first()?.timestamp)

        // Add image
        viewModel.addImage("/path/to/image.png", 3000L)
        val images = viewModel.spotDetails.value?.images
        assertEquals(1, images?.size)
        assertEquals("/path/to/image.png", images?.first()?.imagePath)
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
}
