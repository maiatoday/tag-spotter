package net.maiatoday.tagspotter.feature.gallery

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.maiatoday.tagspotter.MainDispatcherExtension
import net.maiatoday.tagspotter.core.model.FilterCenter
import net.maiatoday.tagspotter.core.model.Spot
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.settings.FakeSettingsRepository
import net.maiatoday.tagspotter.core.database.FakeSpotRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherExtension = MainDispatcherExtension()

    private val repository = FakeSpotRepository()
    private val settingsRepository = FakeSettingsRepository()

    @Test
    fun spotsFilteredByCategoryCorrectly() = runTest {
        // Setup initial spots
        val spot1 = Spot(
            id = 1L,
            latitude = 1.0,
            longitude = 2.0,
            createdAt = 1000L,
            description = "A",
            tags = emptyList(),
            category = "graffiti",
            status = "active"
        )
        val spot2 = Spot(
            id = 2L,
            latitude = 3.0,
            longitude = 4.0,
            createdAt = 2000L,
            description = "B",
            tags = emptyList(),
            category = "sculpture",
            status = "active"
        )
        val spotDetails1 = SpotDetails(spot1, emptyList(), emptyList())
        val spotDetails2 = SpotDetails(spot2, emptyList(), emptyList())

        repository.setSpots(listOf(spotDetails1, spotDetails2))

        val viewModel = GalleryViewModel(repository, settingsRepository)

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
        viewModel.selectCategory("nature")
        assertEquals("nature", viewModel.selectedCategory.value)
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

        val viewModel = GalleryViewModel(repository, settingsRepository)

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

    @Test
    fun locationAndRadiusFilteringWorksCorrectly() = runTest {
        // Milan coordinates: 45.4642, 9.1899
        // London coordinates: 51.5074, -0.1278
        val milanSpot = Spot(
            id = 1L,
            latitude = 45.4640,
            longitude = 9.1890,
            createdAt = 1000L,
            description = "Milan Spot",
            tags = emptyList(),
            category = "graffiti",
            status = "active"
        )
        val londonSpot = Spot(
            id = 2L,
            latitude = 51.5070,
            longitude = -0.1270,
            createdAt = 2000L,
            description = "London Spot",
            tags = emptyList(),
            category = "graffiti",
            status = "active"
        )

        repository.setSpots(
            listOf(
                SpotDetails(milanSpot, emptyList(), emptyList()),
                SpotDetails(londonSpot, emptyList(), emptyList())
            )
        )

        val viewModel = GalleryViewModel(repository, settingsRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.spots.collect {}
        }

        // Initially no location filter, returns both
        assertEquals(2, viewModel.spots.value.size)

        // Apply Milan filter (radius 5km)
        viewModel.setLocationFilter(
            FilterCenter.FocusCity("Milan", 45.4642, 9.1899),
            5000.0
        )

        assertEquals(1, viewModel.spots.value.size)
        assertEquals(1L, viewModel.spots.value[0].spot.id)

        // Clear location filter
        viewModel.clearLocationFilter()
        assertEquals(2, viewModel.spots.value.size)
    }

    @Test
    fun bulkUpdateStarredAndLimitExceeded() = runTest {
        val spot1 = Spot(
            id = 1L,
            latitude = 1.0,
            longitude = 2.0,
            createdAt = 1000L,
            description = "A",
            tags = emptyList(),
            category = "graffiti",
            status = "active",
            isStarred = false
        )
        val spot2 = Spot(
            id = 2L,
            latitude = 3.0,
            longitude = 4.0,
            createdAt = 2000L,
            description = "B",
            tags = emptyList(),
            category = "sculpture",
            status = "active",
            isStarred = false
        )
        val spotDetails1 = SpotDetails(spot1, emptyList(), emptyList())
        val spotDetails2 = SpotDetails(spot2, emptyList(), emptyList())
        repository.setSpots(listOf(spotDetails1, spotDetails2))

        val viewModel = GalleryViewModel(repository, settingsRepository)

        // Collect spots StateFlow in backgroundScope
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.spots.collect {}
        }

        // Initially not starred
        assertEquals(false, viewModel.spots.value.find { it.spot.id == 1L }?.spot?.isStarred)
        assertEquals(false, viewModel.spots.value.find { it.spot.id == 2L }?.spot?.isStarred)

        // Bulk star both spots
        var completed = false
        viewModel.bulkUpdateStarred(listOf(1L, 2L), isStarred = true) {
            completed = true
        }
        assertTrue(completed)
        assertEquals(true, viewModel.spots.value.find { it.spot.id == 1L }?.spot?.isStarred)
        assertEquals(true, viewModel.spots.value.find { it.spot.id == 2L }?.spot?.isStarred)

        // Bulk unstar both spots
        completed = false
        viewModel.bulkUpdateStarred(listOf(1L, 2L), isStarred = false) {
            completed = true
        }
        assertTrue(completed)
        assertEquals(false, viewModel.spots.value.find { it.spot.id == 1L }?.spot?.isStarred)
        assertEquals(false, viewModel.spots.value.find { it.spot.id == 2L }?.spot?.isStarred)

        // Now mock 99 starred spots, and try to bulk star the 2 spots (would exceed 100 limit: 99 + 2 = 101)
        val starredSpots = (1..99).map { i ->
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
        repository.setSpots(starredSpots + spotDetails1 + spotDetails2)

        var limitExceededEmitted = false
        val collectEventsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvent.collect { event ->
                if (event is GalleryViewModel.UiEvent.StarLimitExceeded) {
                    limitExceededEmitted = true
                }
            }
        }

        // Try to bulk star both (should fail due to 100 limit)
        completed = false
        viewModel.bulkUpdateStarred(listOf(1L, 2L), isStarred = true) {
            completed = true
        }
        assertEquals(false, completed)
        assertTrue(limitExceededEmitted)
        assertEquals(false, viewModel.spots.value.find { it.spot.id == 1L }?.spot?.isStarred)
        assertEquals(false, viewModel.spots.value.find { it.spot.id == 2L }?.spot?.isStarred)
    }

    @Test
    fun spotsFilteredByStarredOnlyCorrectly() = runTest {
        val spot1 = Spot(
            id = 1L,
            latitude = 1.0,
            longitude = 2.0,
            createdAt = 1000L,
            description = "A",
            tags = emptyList(),
            category = "graffiti",
            status = "active",
            isStarred = true
        )
        val spot2 = Spot(
            id = 2L,
            latitude = 3.0,
            longitude = 4.0,
            createdAt = 2000L,
            description = "B",
            tags = emptyList(),
            category = "sculpture",
            status = "active",
            isStarred = false
        )
        val spotDetails1 = SpotDetails(spot1, emptyList(), emptyList())
        val spotDetails2 = SpotDetails(spot2, emptyList(), emptyList())

        repository.setSpots(listOf(spotDetails1, spotDetails2))

        val viewModel = GalleryViewModel(repository, settingsRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.spots.collect {}
        }

        // Initially showStarredOnly is false, so returns both
        assertEquals(false, viewModel.showStarredOnly.value)
        assertEquals(2, viewModel.spots.value.size)

        // Toggle showStarredOnly to true, should only return spot1 (starred)
        viewModel.toggleShowStarredOnly()
        assertEquals(true, viewModel.showStarredOnly.value)
        assertEquals(1, viewModel.spots.value.size)
        assertEquals(1L, viewModel.spots.value[0].spot.id)

        // Toggle showStarredOnly back to false, should return both
        viewModel.toggleShowStarredOnly()
        assertEquals(false, viewModel.showStarredOnly.value)
        assertEquals(2, viewModel.spots.value.size)
    }

    @Test
    fun spotsFilteredByEmojiSearchQueryCorrectly() = runTest {
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

        val viewModel = GalleryViewModel(repository, settingsRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.spots.collect {}
        }

        // Search by graffiti emoji 🎨
        viewModel.setSearchQuery("🎨")
        assertEquals(1, viewModel.spots.value.size)
        assertEquals(1L, viewModel.spots.value[0].spot.id)

        // Search by sculpture emoji 🗿
        viewModel.setSearchQuery("🗿")
        assertEquals(1, viewModel.spots.value.size)
        assertEquals(2L, viewModel.spots.value[0].spot.id)
    }
}