package net.maiatoday.tagspotter.ui.viewmodel

import net.maiatoday.tagspotter.MainDispatcherRule
import net.maiatoday.tagspotter.data.FakeSpotRepository
import net.maiatoday.tagspotter.data.Spot
import net.maiatoday.tagspotter.data.SpotDetails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSpotRepository()

    @Test
    fun spotsFilteredByCategoryCorrectly() = runTest {
        // Setup initial spots
        val spot1 = Spot(id = 1L, latitude = 1.0, longitude = 2.0, createdAt = 1000L, description = "A", tags = emptyList(), category = "graffiti", status = "active")
        val spot2 = Spot(id = 2L, latitude = 3.0, longitude = 4.0, createdAt = 2000L, description = "B", tags = emptyList(), category = "sculpture", status = "active")
        val spotDetails1 = SpotDetails(spot1, emptyList(), emptyList())
        val spotDetails2 = SpotDetails(spot2, emptyList(), emptyList())
        
        repository.setSpots(listOf(spotDetails1, spotDetails2))

        val viewModel = GalleryViewModel(repository)

        // Collect spots in backgroundScope to trigger WhileSubscribed StateFlow updates
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.spots.collect {}
        }

        // Default category is "All"
        assertEquals("All", viewModel.selectedCategory.value)
        var spots = viewModel.spots.value
        assertEquals(2, spots.size)

        // Select "graffiti"
        viewModel.selectCategory("graffiti")
        assertEquals("graffiti", viewModel.selectedCategory.value)

        spots = viewModel.spots.value
        assertEquals(1, spots.size)
        assertEquals(1L, spots[0].spot.id)

        // Select "sculpture"
        viewModel.selectCategory("sculpture")
        assertEquals("sculpture", viewModel.selectedCategory.value)
        spots = viewModel.spots.value
        assertEquals(1, spots.size)
        assertEquals(2L, spots[0].spot.id)

        // Select non-existing category
        viewModel.selectCategory("tree")
        assertEquals("tree", viewModel.selectedCategory.value)
        spots = viewModel.spots.value
        assertTrue(spots.isEmpty())
    }

    @Test
    fun spotsFilteredBySearchQueryCorrectly() = runTest {
        val spot1 = Spot(
            id = 1L, latitude = 1.0, longitude = 2.0, createdAt = 1000L,
            description = "A", tags = listOf("milan", "stencil"), category = "graffiti",
            status = "active", artists = listOf("Mr. Brainwash"), photographer = "Alice"
        )
        val spot2 = Spot(
            id = 2L, latitude = 3.0, longitude = 4.0, createdAt = 2000L,
            description = "B", tags = listOf("london"), category = "sculpture",
            status = "active", artists = listOf("Famous Sculptor"), photographer = "Bob"
        )
        val spotDetails1 = SpotDetails(spot1, emptyList(), emptyList())
        val spotDetails2 = SpotDetails(spot2, emptyList(), emptyList())
        repository.setSpots(listOf(spotDetails1, spotDetails2))

        val viewModel = GalleryViewModel(repository)

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.spots.collect {}
        }

        // Initially no query, returns both
        assertEquals("", viewModel.searchQuery.value)
        assertEquals(2, viewModel.spots.value.size)

        // Search by tag
        viewModel.setSearchQuery("milan")
        assertEquals(1, viewModel.spots.value.size)
        assertEquals(1L, viewModel.spots.value[0].spot.id)

        // Search by artist
        viewModel.setSearchQuery("Famous")
        assertEquals(1, viewModel.spots.value.size)
        assertEquals(2L, viewModel.spots.value[0].spot.id)

        // Search by photographer
        viewModel.setSearchQuery("Alice")
        assertEquals(1, viewModel.spots.value.size)
        assertEquals(1L, viewModel.spots.value[0].spot.id)

        // Non-matching search
        viewModel.setSearchQuery("xyz")
        assertTrue(viewModel.spots.value.isEmpty())
    }
}
