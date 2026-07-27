package net.maiatoday.tagspotter.feature.map

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.maiatoday.tagspotter.core.database.FakeSpotRepository
import net.maiatoday.tagspotter.core.model.FilterCenter
import net.maiatoday.tagspotter.core.model.Spot
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.settings.FakeSettingsRepository
import net.maiatoday.tagspotter.core.settings.FilterManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

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
    private val filterManager = FilterManager()
    private val settingsRepository = FakeSettingsRepository("Test Photographer", "Milan")

    @Test
    fun testSelectedSpotFlowWithTurbine() = runTest {
        val viewModel = MapViewModel(repository, filterManager, settingsRepository)

        viewModel.selectedSpot.test {
            // Initial item is null
            assertNull(awaitItem())

            val spot1 = Spot(1L, 45.0, 9.0, 1000L, "Milan Spot", emptyList(), "graffiti", "active")
            val spotDetails1 = SpotDetails(spot1, emptyList(), emptyList())

            viewModel.selectSpot(spotDetails1)
            assertEquals(spotDetails1, awaitItem())

            viewModel.selectSpot(null)
            assertNull(awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun testReactiveSpotsFilteringWithTurbine() = runTest {
        val spot1 = Spot(1L, 45.0, 9.0, 1000L, "Graffiti Spot", emptyList(), "graffiti", "active", isImported = false)
        val spot2 = Spot(2L, 51.0, -0.1, 2000L, "Sculpture Spot", emptyList(), "sculpture", "active", isImported = true)
        repository.setSpots(listOf(SpotDetails(spot1, emptyList(), emptyList()), SpotDetails(spot2, emptyList(), emptyList())))

        val viewModel = MapViewModel(repository, filterManager, settingsRepository)

        viewModel.spots.test {
            // 1. Initial emission contains both spots
            val initialList = awaitItem()
            assertEquals(2, initialList.size)

            // 2. Filter by source "My Spots"
            viewModel.selectSource("My Spots")
            val mySpotsList = awaitItem()
            assertEquals(1, mySpotsList.size)
            assertEquals(1L, mySpotsList[0].spot.id)

            // 3. Filter by source "Imported"
            viewModel.selectSource("Imported")
            val importedList = awaitItem()
            assertEquals(1, importedList.size)
            assertEquals(2L, importedList[0].spot.id)

            // 4. Reset source filter
            viewModel.selectSource("All")
            assertEquals(2, awaitItem().size)

            // 5. Filter by category "sculpture"
            viewModel.selectCategory("sculpture")
            val sculptureList = awaitItem()
            assertEquals(1, sculptureList.size)
            assertEquals(2L, sculptureList[0].spot.id)

            // 6. Search query "Graffiti"
            viewModel.selectCategory("All")
            awaitItem() // Reset category emission
            viewModel.setSearchQuery("Graffiti")
            val searchList = awaitItem()
            assertEquals(1, searchList.size)
            assertEquals(1L, searchList[0].spot.id)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun testLocationAndRadiusFilterWithTurbine() = runTest {
        val milanSpot = Spot(1L, 45.4640, 9.1890, 1000L, "Milan Spot", emptyList(), "graffiti", "active")
        val londonSpot = Spot(2L, 51.5070, -0.1270, 2000L, "London Spot", emptyList(), "graffiti", "active")
        repository.setSpots(listOf(SpotDetails(milanSpot, emptyList(), emptyList()), SpotDetails(londonSpot, emptyList(), emptyList())))

        val viewModel = MapViewModel(repository, filterManager, settingsRepository)

        viewModel.spots.test {
            assertEquals(2, awaitItem().size)

            // Apply location filter (Milan, 5km radius)
            viewModel.setLocationFilter(FilterCenter.FocusCity("Milan", 45.4642, 9.1899), 5000.0)
            val filtered = awaitItem()
            assertEquals(1, filtered.size)
            assertEquals(1L, filtered[0].spot.id)

            // Clear location filter
            viewModel.clearLocationFilter()
            assertEquals(2, awaitItem().size)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun testInitialMapCenterFlowWithTurbine() = runTest {
        val viewModel = MapViewModel(repository, filterManager, settingsRepository)

        viewModel.initialMapCenter.test {
            val center = awaitItem()
            assertNotNull(center)
            assertEquals(45.4642, center.latitude, 0.01)
            assertEquals(9.1899, center.longitude, 0.01)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun testDarkMapEnabledFlowWithTurbine() = runTest {
        val viewModel = MapViewModel(repository, filterManager, settingsRepository)

        viewModel.darkMapEnabled.test {
            assertFalse(awaitItem())

            settingsRepository.updateDarkMapEnabled(true)
            assertTrue(awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }
}
