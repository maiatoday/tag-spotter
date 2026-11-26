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
import net.maiatoday.tagspotter.core.sync.AuthService
import net.maiatoday.tagspotter.core.sync.FirebaseUserWrapper
import net.maiatoday.tagspotter.core.sync.SyncManager
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
    private val authService = FakeMapAuthService()
    private val syncManager = FakeMapSyncManager()

    @Test
    fun testSelectedSpotFlowWithTurbine() = runTest {
        val viewModel = MapViewModel(repository, filterManager, settingsRepository, authService, syncManager)

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

        val viewModel = MapViewModel(repository, filterManager, settingsRepository, authService, syncManager)

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

        val viewModel = MapViewModel(repository, filterManager, settingsRepository, authService, syncManager)

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
        val viewModel = MapViewModel(repository, filterManager, settingsRepository, authService, syncManager)

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
        val viewModel = MapViewModel(repository, filterManager, settingsRepository, authService, syncManager)

        viewModel.darkMapEnabled.test {
            assertFalse(awaitItem())

            settingsRepository.updateDarkMapEnabled(true)
            assertTrue(awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun testShowPublicSpotsToggleAndFourSourceFilters() = runTest {
        authService.userFlow.value = FirebaseUserWrapper("viewer-uid", "viewer@test.com", "Viewer")

        val ownSpot = Spot(
            id = 1L, latitude = 45.0, longitude = 9.0, createdAt = 1000L,
            description = "Own", tags = emptyList(), category = "graffiti", status = "active",
            ownerUid = "viewer-uid", isPublic = true, isImported = false
        )
        val otherPublicSpot = Spot(
            id = 2L, latitude = 45.1, longitude = 9.1, createdAt = 2000L,
            description = "Other Public", tags = emptyList(), category = "graffiti", status = "active",
            ownerUid = "author-other", isPublic = true, isImported = false
        )
        val importedSpot = Spot(
            id = 3L, latitude = 45.2, longitude = 9.2, createdAt = 3000L,
            description = "Imported", tags = emptyList(), category = "graffiti", status = "active",
            ownerUid = "pack-author", isPublic = false, isImported = true, parentPackId = "PACK01"
        )
        repository.setSpots(
            listOf(
                SpotDetails(ownSpot, emptyList(), emptyList()),
                SpotDetails(otherPublicSpot, emptyList(), emptyList()),
                SpotDetails(importedSpot, emptyList(), emptyList())
            )
        )

        val viewModel = MapViewModel(repository, filterManager, settingsRepository, authService, syncManager)

        viewModel.spots.test {
            // Default showPublicSpots = true -> All returns all 3
            assertEquals(setOf(1L, 2L, 3L), awaitItem().map { it.spot.id }.toSet())

            viewModel.selectSource("My Spots")
            assertEquals(listOf(1L), awaitItem().map { it.spot.id })

            viewModel.selectSource("Public")
            assertEquals(listOf(2L), awaitItem().map { it.spot.id })

            viewModel.selectSource("Imported")
            assertEquals(listOf(3L), awaitItem().map { it.spot.id })

            // Select Public then toggle showPublicSpots OFF -> resets source to All and hides spot 2
            viewModel.selectSource("Public")
            assertEquals(listOf(2L), awaitItem().map { it.spot.id })

            viewModel.setShowPublicSpots(false)
            assertEquals("All", viewModel.selectedSource.value)
            assertEquals(setOf(1L, 3L), expectMostRecentItem().map { it.spot.id }.toSet())

            // Toggle back ON -> triggers syncPublicSpots and shows all 3 again
            viewModel.setShowPublicSpots(true)
            assertTrue(syncManager.syncPublicSpotsCalled)
            assertEquals(setOf(1L, 2L, 3L), expectMostRecentItem().map { it.spot.id }.toSet())

            cancelAndConsumeRemainingEvents()
        }
    }
}

private class FakeMapAuthService(initialUser: FirebaseUserWrapper? = null) : AuthService {
    val userFlow = kotlinx.coroutines.flow.MutableStateFlow(initialUser)
    override val currentUserFlow = userFlow
    override val isGoogleSignInSupported: Boolean = false
    override suspend fun signInWithGoogle(idToken: String?): Result<Unit> = Result.success(Unit)
    override suspend fun signInWithEmailAndPassword(email: String, password: String): Result<Unit> = Result.success(Unit)
    override suspend fun signUpWithEmailAndPassword(email: String, password: String): Result<Unit> = Result.success(Unit)
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = Result.success(Unit)
    override suspend fun signOut() { userFlow.value = null }
}

private class FakeMapSyncManager : SyncManager {
    override val isSyncing = kotlinx.coroutines.flow.MutableStateFlow(false)
    var syncPublicSpotsCalled = false
    override suspend fun syncNow() {}
    override suspend fun syncPublicSpots() { syncPublicSpotsCalled = true }
    override fun startRealtimeSync(userId: String) {}
    override fun stopRealtimeSync() {}
    override suspend fun deleteSpot(uuid: String) {}
    override suspend fun sharePack(
        title: String,
        description: String,
        authorName: String,
        spots: List<SpotDetails>
    ): String = ""
    override suspend fun importPackByCode(code: String): net.maiatoday.tagspotter.core.model.SharedPack = error("not implemented")
    override suspend fun saveImportedPack(sharedPack: net.maiatoday.tagspotter.core.model.SharedPack) {}
}
