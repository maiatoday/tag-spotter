package net.maiatoday.tagspotter.feature.gallery

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import app.cash.turbine.test
import net.maiatoday.tagspotter.core.location.FakeLocationProvider
import net.maiatoday.tagspotter.core.model.FilterCenter
import net.maiatoday.tagspotter.core.model.Spot
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.settings.FakeSettingsRepository
import net.maiatoday.tagspotter.core.database.FakeSpotRepository
import net.maiatoday.tagspotter.core.settings.FilterManager
import net.maiatoday.tagspotter.core.photo.FakePhotoProcessor
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.maiatoday.tagspotter.core.sync.SyncManager


@OptIn(ExperimentalCoroutinesApi::class)
class GalleryViewModelTest {

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
    private val settingsRepository = FakeSettingsRepository()
    private val filterManager = FilterManager()
    private val locationProvider = FakeLocationProvider()
    private val photoProcessor = FakePhotoProcessor()
    private val syncManager = FakeSyncManager()

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

        val viewModel = GalleryViewModel(repository, filterManager, settingsRepository, locationProvider, photoProcessor, syncManager)

        // Collect spots in backgroundScope to trigger WhileSubscribed StateFlow updates
        backgroundScope.launch(testDispatcher) {
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

        val viewModel = GalleryViewModel(repository, filterManager, settingsRepository, locationProvider, photoProcessor, syncManager)

        backgroundScope.launch(testDispatcher) {
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

        val viewModel = GalleryViewModel(repository, filterManager, settingsRepository, locationProvider, photoProcessor, syncManager)

        backgroundScope.launch(testDispatcher) {
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

        val viewModel = GalleryViewModel(repository, filterManager, settingsRepository, locationProvider, photoProcessor, syncManager)

        // Collect spots StateFlow in backgroundScope
        backgroundScope.launch(testDispatcher) {
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

        viewModel.uiEvent.test {
            // Try to bulk star both (should fail due to 100 limit)
            completed = false
            viewModel.bulkUpdateStarred(listOf(1L, 2L), isStarred = true) {
                completed = true
            }
            assertEquals(false, completed)
            
            // Verify limit exceeded emitted
            val event = awaitItem()
            assertTrue(event is GalleryViewModel.UiEvent.StarLimitExceeded)
            
            cancelAndConsumeRemainingEvents()
        }
        
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

        val viewModel = GalleryViewModel(repository, filterManager, settingsRepository, locationProvider, photoProcessor, syncManager)

        backgroundScope.launch(testDispatcher) {
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

        val viewModel = GalleryViewModel(repository, filterManager, settingsRepository, locationProvider, photoProcessor, syncManager)

        backgroundScope.launch(testDispatcher) {
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

    @Test
    fun syncNow_callsSyncManager() = runTest {
        var syncCalled = false
        val customSyncManager = object : FakeSyncManager() {
            override suspend fun syncNow() {
                syncCalled = true
            }
        }
        val viewModel = GalleryViewModel(repository, filterManager, settingsRepository, locationProvider, photoProcessor, customSyncManager)
        viewModel.syncNow()
        assertTrue(syncCalled)
    }

    @Test
    fun sharePack_successCallsOnSuccessWithCode() = runTest {
        val spot1 = Spot(id = 1L, latitude = 1.0, longitude = 2.0, createdAt = 1000L, description = "A", tags = emptyList(), category = "graffiti", status = "active")
        val spotDetails1 = SpotDetails(spot1, emptyList(), emptyList())
        repository.setSpots(listOf(spotDetails1))

        var sharedTitle = ""
        val customSyncManager = object : FakeSyncManager() {
            override suspend fun sharePack(title: String, description: String, authorName: String, spots: List<SpotDetails>): String {
                sharedTitle = title
                return "PACK123"
            }
        }
        val viewModel = GalleryViewModel(repository, filterManager, settingsRepository, locationProvider, photoProcessor, customSyncManager)
        
        backgroundScope.launch(testDispatcher) {
            viewModel.spots.collect {}
        }

        var returnedCode: String? = null
        viewModel.sharePack("My Pack", "Desc", "Author", listOf(1L), onSuccess = { returnedCode = it }, onError = {})
        
        assertEquals("PACK123", returnedCode)
        assertEquals("My Pack", sharedTitle)
    }

    @Test
    fun importPackByCode_successCallsOnSuccess() = runTest {
        val expectedPack = net.maiatoday.tagspotter.core.model.SharedPack("packId", "title", "author", "desc", emptyList())
        val customSyncManager = object : FakeSyncManager() {
            override suspend fun importPackByCode(code: String): net.maiatoday.tagspotter.core.model.SharedPack {
                return expectedPack
            }
        }
        val viewModel = GalleryViewModel(repository, filterManager, settingsRepository, locationProvider, photoProcessor, customSyncManager)
        
        var returnedPack: net.maiatoday.tagspotter.core.model.SharedPack? = null
        viewModel.importPackByCode("code", onSuccess = { returnedPack = it }, onError = {})
        
        assertEquals(expectedPack, returnedPack)
    }

    @Test
    fun saveImportedPack_savesToRepository() = runTest {
        val sharedPack = net.maiatoday.tagspotter.core.model.SharedPack("packId", "title", "author", "desc", emptyList())
        var saveCalled = false
        val customSyncManager = object : FakeSyncManager() {
            override suspend fun saveImportedPack(pack: net.maiatoday.tagspotter.core.model.SharedPack) {
                saveCalled = true
            }
        }
        val viewModel = GalleryViewModel(repository, filterManager, settingsRepository, locationProvider, photoProcessor, customSyncManager)
        
        backgroundScope.launch(testDispatcher) {
            viewModel.loadedPacks.collect {}
        }

        var successCalled = false
        viewModel.saveImportedPack(sharedPack, onSuccess = { successCalled = true }, onError = {})
        
        assertTrue(saveCalled)
        assertTrue(successCalled)
        assertEquals(1, viewModel.loadedPacks.value.size)
        assertEquals("packId", viewModel.loadedPacks.value[0].packId)
    }

    @Test
    fun deleteSpots_removesFromRepository() = runTest {
        val spot1 = Spot(id = 1L, latitude = 1.0, longitude = 2.0, createdAt = 1000L, description = "A", tags = emptyList(), category = "graffiti", status = "active")
        val spotDetails1 = SpotDetails(spot1, emptyList(), emptyList())
        repository.setSpots(listOf(spotDetails1))

        val viewModel = GalleryViewModel(repository, filterManager, settingsRepository, locationProvider, photoProcessor, syncManager)
        backgroundScope.launch(testDispatcher) {
            viewModel.spots.collect {}
        }

        assertEquals(1, viewModel.spots.value.size)

        var completed = false
        viewModel.deleteSpots(listOf(1L), onCompleted = { completed = true })
        
        assertTrue(completed)
        assertTrue(viewModel.spots.value.isEmpty())
    }

    @Test
    fun unloadPack_deletesFromRepository() = runTest {
        val viewModel = GalleryViewModel(repository, filterManager, settingsRepository, locationProvider, photoProcessor, syncManager)
        
        backgroundScope.launch(testDispatcher) {
            viewModel.loadedPacks.collect {}
        }

        val loadedPack = net.maiatoday.tagspotter.core.model.LoadedPack("packId", "title", "author", "desc", 0L, 0L)
        repository.saveLoadedPack(loadedPack)
        assertEquals(1, viewModel.loadedPacks.value.size)
        
        var completed = false
        viewModel.unloadPack("packId", onCompleted = { completed = true })
        
        assertTrue(completed)
        assertTrue(viewModel.loadedPacks.value.isEmpty())
    }

    @Test
    fun setLocationFilter_GPS_resolvesCurrentLocation() = runTest {
        locationProvider.locationToReturn = net.maiatoday.tagspotter.core.location.LocationData(45.1111, 9.2222, false)
        val viewModel = GalleryViewModel(repository, filterManager, settingsRepository, locationProvider, photoProcessor, syncManager)
        
        viewModel.setLocationFilter(FilterCenter.GPS(0.0, 0.0), 1000.0)
        
        val center = viewModel.activeFilterCenter.value as FilterCenter.GPS
        assertEquals(45.1111, center.latitude, 0.0001)
        assertEquals(9.2222, center.longitude, 0.0001)
    }

    @Test
    fun sourceFilteringWithTurbine() = runTest {
        val spot1 = Spot(id = 1L, latitude = 1.0, longitude = 2.0, createdAt = 1000L, description = "A", tags = emptyList(), category = "graffiti", status = "active", isImported = false)
        val spot2 = Spot(id = 2L, latitude = 3.0, longitude = 4.0, createdAt = 2000L, description = "B", tags = emptyList(), category = "sculpture", status = "active", isImported = true)
        repository.setSpots(listOf(SpotDetails(spot1, emptyList(), emptyList()), SpotDetails(spot2, emptyList(), emptyList())))

        val viewModel = GalleryViewModel(repository, filterManager, settingsRepository, locationProvider, photoProcessor, syncManager)

        viewModel.spots.test {
            assertEquals(2, awaitItem().size)

            viewModel.selectSource("My Spots")
            val mySpots = awaitItem()
            assertEquals(1, mySpots.size)
            assertEquals(1L, mySpots[0].spot.id)

            viewModel.selectSource("Imported")
            val importedSpots = awaitItem()
            assertEquals(1, importedSpots.size)
            assertEquals(2L, importedSpots[0].spot.id)

            viewModel.selectSource("All")
            assertEquals(2, awaitItem().size)

            cancelAndConsumeRemainingEvents()
        }
    }
}

open class FakeSyncManager : SyncManager {
    override val isSyncing = kotlinx.coroutines.flow.MutableStateFlow(false)
    open override suspend fun syncNow() {}
    override fun startRealtimeSync(userId: String) {}
    override fun stopRealtimeSync() {}
    open override suspend fun deleteSpot(uuid: String) {}
    open override suspend fun sharePack(title: String, description: String, authorName: String, spots: List<SpotDetails>): String = ""
    open override suspend fun importPackByCode(code: String): net.maiatoday.tagspotter.core.model.SharedPack = error("not implemented")
    open override suspend fun saveImportedPack(sharedPack: net.maiatoday.tagspotter.core.model.SharedPack) {}
}

