package net.maiatoday.tagspotter.feature.map

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.maiatoday.tagspotter.MainDispatcherRule
import net.maiatoday.tagspotter.core.model.Spot
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.settings.FakeSettingsRepository
import net.maiatoday.tagspotter.core.database.FakeSpotRepository
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSpotRepository()
    private val settingsRepository = FakeSettingsRepository()

    @Test
    fun mapPinsAndSelectionWorkCorrectly() = runTest {
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

        val viewModel = MapViewModel(repository, settingsRepository)

        // Collect spots in backgroundScope to trigger WhileSubscribed StateFlow updates
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.spots.collect {}
        }

        // Verify loaded spots pins (default category is "All")
        Assert.assertEquals(2, viewModel.spots.value.size)

        // Verify initial selection is null
        Assert.assertNull(viewModel.selectedSpot.value)

        // Select a spot
        viewModel.selectSpot(spotDetails1)
        Assert.assertEquals(spotDetails1, viewModel.selectedSpot.value)

        // Select category "sculpture" and verify filtered pins
        viewModel.selectCategory("sculpture")
        Assert.assertEquals(1, viewModel.spots.value.size)
        Assert.assertEquals(2L, viewModel.spots.value[0].spot.id)

        // Deselect spot
        viewModel.selectSpot(null)
        Assert.assertNull(viewModel.selectedSpot.value)
    }

    @Test
    fun initialMapCenterResolvesCorrectly() = runTest {
        // 1. Empty spots, default home city "Milan"
        val settingsRepo = FakeSettingsRepository(initialHomeCity = "Milan")
        val viewModel = MapViewModel(repository, settingsRepo)

        // Collect initialMapCenter to trigger StateFlow updates
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.initialMapCenter.collect {}
        }

        // Milan coords: (45.4642, 9.1900)
        val milanCenter = viewModel.initialMapCenter.value
        Assert.assertNotNull(milanCenter)
        Assert.assertEquals(45.4642, milanCenter!!.latitude, 0.0001)
        Assert.assertEquals(9.1900, milanCenter.longitude, 0.0001)

        // 2. Change home city to "London" with no spots
        settingsRepo.updateHomeCity("London")
        val londonCenter = viewModel.initialMapCenter.value
        Assert.assertNotNull(londonCenter)
        Assert.assertEquals(51.5074, londonCenter!!.latitude, 0.0001)
        Assert.assertEquals(-0.1278, londonCenter.longitude, 0.0001)

        // 3. Add spots in Paris (48.8566, 2.3522) and San Francisco (37.7749, -122.4194)
        // Let's add 2 spots in Paris and 1 in SF
        val paris1 = Spot(
            id = 1L,
            latitude = 48.8560,
            longitude = 2.3520,
            createdAt = 1000L,
            description = "Paris 1",
            tags = emptyList(),
            category = "graffiti",
            status = "active"
        )
        val paris2 = Spot(
            id = 2L,
            latitude = 48.8570,
            longitude = 2.3530,
            createdAt = 2000L,
            description = "Paris 2",
            tags = emptyList(),
            category = "sculpture",
            status = "active"
        )
        val sf1 = Spot(
            id = 3L,
            latitude = 37.7749,
            longitude = -122.4194,
            createdAt = 3000L,
            description = "SF 1",
            tags = emptyList(),
            category = "nature",
            status = "active"
        )

        repository.setSpots(
            listOf(
                SpotDetails(paris1, emptyList(), emptyList()),
                SpotDetails(paris2, emptyList(), emptyList()),
                SpotDetails(sf1, emptyList(), emptyList())
            )
        )

        // Initial center should now be the average of the Paris cluster (since it has 2 spots vs SF's 1)
        val avgParisLat = (48.8560 + 48.8570) / 2.0
        val avgParisLng = (2.3520 + 2.3530) / 2.0

        val parisClusterCenter = viewModel.initialMapCenter.value
        Assert.assertNotNull(parisClusterCenter)
        Assert.assertEquals(avgParisLat, parisClusterCenter!!.latitude, 0.0001)
        Assert.assertEquals(avgParisLng, parisClusterCenter.longitude, 0.0001)

        // 4. Clear spots and check Berlin
        repository.setSpots(emptyList())
        settingsRepo.updateHomeCity("Berlin")
        val berlinCenter = viewModel.initialMapCenter.value
        Assert.assertNotNull(berlinCenter)
        Assert.assertEquals(52.5200, berlinCenter!!.latitude, 0.0001)
        Assert.assertEquals(13.4050, berlinCenter.longitude, 0.0001)

        // 5. Check Custom Coordinates
        settingsRepo.updateHomeCity("Custom: 12.3456, -78.9012")
        val customCenter = viewModel.initialMapCenter.value
        Assert.assertNotNull(customCenter)
        Assert.assertEquals(12.3456, customCenter!!.latitude, 0.0001)
        Assert.assertEquals(-78.9012, customCenter.longitude, 0.0001)

        // 6. Check bad Custom Coordinates fallback (should default to Milan)
        settingsRepo.updateHomeCity("Custom: invalid, coordinates")
        val fallbackCenter = viewModel.initialMapCenter.value
        Assert.assertNotNull(fallbackCenter)
        Assert.assertEquals(45.4642, fallbackCenter!!.latitude, 0.0001)
        Assert.assertEquals(9.1900, fallbackCenter.longitude, 0.0001)
    }
}