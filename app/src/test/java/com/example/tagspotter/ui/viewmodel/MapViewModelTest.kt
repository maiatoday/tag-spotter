package com.example.tagspotter.ui.viewmodel

import com.example.tagspotter.MainDispatcherRule
import com.example.tagspotter.data.FakeSpotRepository
import com.example.tagspotter.data.Spot
import com.example.tagspotter.data.SpotDetails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSpotRepository()

    @Test
    fun mapPinsAndSelectionWorkCorrectly() = runTest {
        val spot1 = Spot(id = 1L, latitude = 1.0, longitude = 2.0, createdAt = 1000L, description = "A", tags = emptyList(), category = "graffiti", status = "active")
        val spot2 = Spot(id = 2L, latitude = 3.0, longitude = 4.0, createdAt = 2000L, description = "B", tags = emptyList(), category = "sculpture", status = "active")
        val spotDetails1 = SpotDetails(spot1, emptyList(), emptyList())
        val spotDetails2 = SpotDetails(spot2, emptyList(), emptyList())

        repository.setSpots(listOf(spotDetails1, spotDetails2))

        val viewModel = MapViewModel(repository)

        // Collect spots in backgroundScope to trigger WhileSubscribed StateFlow updates
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.spots.collect {}
        }

        // Verify loaded spots pins (default category is "All")
        assertEquals(2, viewModel.spots.value.size)

        // Verify initial selection is null
        assertNull(viewModel.selectedSpot.value)

        // Select a spot
        viewModel.selectSpot(spotDetails1)
        assertEquals(spotDetails1, viewModel.selectedSpot.value)

        // Select category "sculpture" and verify filtered pins
        viewModel.selectCategory("sculpture")
        assertEquals(1, viewModel.spots.value.size)
        assertEquals(2L, viewModel.spots.value[0].spot.id)

        // Deselect spot
        viewModel.selectSpot(null)
        assertNull(viewModel.selectedSpot.value)
    }
}
