package net.maiatoday.tagspotter.feature.main

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.maiatoday.tagspotter.MainDispatcherExtension
import net.maiatoday.tagspotter.core.database.FakeSpotRepository
import net.maiatoday.tagspotter.core.location.FakeLocationProvider
import net.maiatoday.tagspotter.core.location.LocationData
import net.maiatoday.tagspotter.core.photo.FakePhotoProcessor
import net.maiatoday.tagspotter.core.photo.PhotoMetadata
import net.maiatoday.tagspotter.core.photo.TempFileDetails
import net.maiatoday.tagspotter.core.settings.FakeSettingsRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import app.cash.turbine.test

import net.maiatoday.tagspotter.core.sync.SyncManager
import net.maiatoday.tagspotter.core.sync.AuthService
import net.maiatoday.tagspotter.core.sync.FirebaseUserWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAuthService : AuthService {
    private val _currentUserFlow = MutableStateFlow<FirebaseUserWrapper?>(null)
    override val currentUserFlow = _currentUserFlow.asStateFlow()
    override val isGoogleSignInSupported: Boolean = true
    override suspend fun signInWithGoogle(idToken: String?): Result<Unit> = Result.success(Unit)
    override suspend fun signInWithEmailAndPassword(email: String, password: String): Result<Unit> = Result.success(Unit)
    override suspend fun signUpWithEmailAndPassword(email: String, password: String): Result<Unit> = Result.success(Unit)
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = Result.success(Unit)
    override suspend fun signOut() {
        _currentUserFlow.value = null
    }

    fun setUser(user: FirebaseUserWrapper?) {
        _currentUserFlow.value = user
    }
}

class FakeSyncManager : SyncManager {
    private val _isSyncing = MutableStateFlow(false)
    override val isSyncing = _isSyncing.asStateFlow()
    var startRealtimeSyncUser: String? = null
    var stopRealtimeSyncCalled = false

    override suspend fun syncNow() {}
    override fun startRealtimeSync(userId: String) {
        startRealtimeSyncUser = userId
    }
    override fun stopRealtimeSync() {
        stopRealtimeSyncCalled = true
    }
    override suspend fun deleteSpot(uuid: String) {}
    override suspend fun sharePack(title: String, description: String, authorName: String, spots: List<net.maiatoday.tagspotter.core.model.SpotDetails>): String = "CODE"
    override suspend fun importPackByCode(code: String): net.maiatoday.tagspotter.core.model.SharedPack = error("Not implemented")
    override suspend fun saveImportedPack(pack: net.maiatoday.tagspotter.core.model.SharedPack) {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherExtension = MainDispatcherExtension()

    private val settingsRepository = FakeSettingsRepository("Initial Photographer", "Milan")
    private val spotRepository = FakeSpotRepository()
    private val locationProvider = FakeLocationProvider()
    private val photoProcessor = FakePhotoProcessor()
    private val authService = FakeAuthService()
    private val syncManager = FakeSyncManager()

    @Test
    fun updateLocationPermission_updatesState() = runTest {
        val viewModel = MainViewModel(
            locationProvider,
            photoProcessor,
            settingsRepository,
            spotRepository,
            authService,
            syncManager,
            UnconfinedTestDispatcher(testScheduler)
        )

        assertFalse(viewModel.uiState.value.hasLocationPermission)

        viewModel.updateLocationPermission(true)
        assertTrue(viewModel.uiState.value.hasLocationPermission)

        viewModel.updateLocationPermission(false)
        assertFalse(viewModel.uiState.value.hasLocationPermission)
    }

    @Test
    fun prepareCameraCapture_setsTempPhotoDetails() = runTest {
        val viewModel = MainViewModel(
            locationProvider,
            photoProcessor,
            settingsRepository,
            spotRepository,
            authService,
            syncManager,
            UnconfinedTestDispatcher(testScheduler)
        )

        assertNull(viewModel.uiState.value.tempPhotoUri)
        assertNull(viewModel.uiState.value.tempPhotoFilePath)

        val expectedUri = "temp_uri"
        photoProcessor.tempCameraFileResult = TempFileDetails(expectedUri, "temp_path")

        val returnedUri = viewModel.prepareCameraCapture()

        assertEquals(expectedUri, returnedUri)
        assertEquals(expectedUri, viewModel.uiState.value.tempPhotoUri)
        assertEquals("temp_path", viewModel.uiState.value.tempPhotoFilePath)
    }

    @Test
    fun handleCameraCaptureSuccess_savesPhotoAndEmitsProcessedEvent() = runTest {
        val viewModel = MainViewModel(
            locationProvider,
            photoProcessor,
            settingsRepository,
            spotRepository,
            authService,
            syncManager,
            UnconfinedTestDispatcher(testScheduler)
        )

        // Set temp file details
        photoProcessor.tempCameraFileResult = TempFileDetails("temp_uri", "temp_path")
        viewModel.prepareCameraCapture()

        locationProvider.locationToReturn = LocationData(45.4642, 9.1900, false)
        viewModel.updateLocationPermission(true)

        viewModel.events.test {
            viewModel.handleCameraCaptureSuccess()

            // Verify that loading was toggled and temp file was deleted
            assertEquals("temp_path", photoProcessor.deleteFileCalledWith)
            assertEquals("temp_path", photoProcessor.saveImageCalledWith)
            assertFalse(viewModel.uiState.value.isLoading)

            // Verify emitted event
            val event = awaitItem() as MainEvent.PhotoProcessed
            assertEquals("public_uri", event.imagePath)
            assertEquals("thumb_path", event.thumbnailPath)
            assertEquals(45.4642, event.latitude, 0.0001)
            assertEquals(9.1900, event.longitude, 0.0001)
            assertFalse(event.isFallback)
            
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun handlePhotoPicked_extractsExifAndEmitsProcessedEvent() = runTest {
        val viewModel = MainViewModel(
            locationProvider,
            photoProcessor,
            settingsRepository,
            spotRepository,
            authService,
            syncManager,
            UnconfinedTestDispatcher(testScheduler)
        )

        photoProcessor.metadataResult = PhotoMetadata(45.4642, 9.1900, 123456789L)

        viewModel.events.test {
            viewModel.handlePhotoPicked("some_uri")

            assertFalse(viewModel.uiState.value.isLoading)

            // Verify emitted event
            val event = awaitItem() as MainEvent.PhotoProcessed
            assertEquals("some_uri", event.imagePath)
            assertEquals("thumb_path", event.thumbnailPath)
            assertEquals(45.4642, event.latitude, 0.0001)
            assertEquals(9.1900, event.longitude, 0.0001)
            assertFalse(event.isFallback)
            assertEquals(123456789L, event.captureTime)
            
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun handlePhotoPicked_withoutExif_usesLocationFallback() = runTest {
        val viewModel = MainViewModel(
            locationProvider,
            photoProcessor,
            settingsRepository,
            spotRepository,
            authService,
            syncManager,
            UnconfinedTestDispatcher(testScheduler)
        )

        photoProcessor.metadataResult = PhotoMetadata(null, null, null)
        locationProvider.locationToReturn = LocationData(45.4642, 9.1900, true)
        viewModel.updateLocationPermission(true)

        viewModel.events.test {
            viewModel.handlePhotoPicked("some_uri")

            // Verify fallback coordinates
            val event = awaitItem() as MainEvent.PhotoProcessed
            assertEquals(45.4642, event.latitude, 0.0001)
            assertEquals(9.1900, event.longitude, 0.0001)
            assertTrue(event.isFallback)
            
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun updateShowTestData_updatesRepository() = runTest {
        val viewModel = MainViewModel(
            locationProvider,
            photoProcessor,
            settingsRepository,
            spotRepository,
            authService,
            syncManager,
            UnconfinedTestDispatcher(testScheduler)
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.showTestData.collect {}
        }

        assertFalse(viewModel.showTestData.value)

        viewModel.updateShowTestData(true)
        assertTrue(viewModel.showTestData.value)
        assertTrue(spotRepository.loadTestDataCalled)

        viewModel.updateShowTestData(false)
        assertFalse(viewModel.showTestData.value)
        assertTrue(spotRepository.unloadTestDataCalled)
    }

    @Test
    fun prepareCameraCapture_onException_emitsErrorEvent() = runTest {
        val viewModel = MainViewModel(
            locationProvider,
            photoProcessor,
            settingsRepository,
            spotRepository,
            authService,
            syncManager,
            UnconfinedTestDispatcher(testScheduler)
        )
        photoProcessor.tempCameraFileException = Exception("Camera fail")

        viewModel.events.test {
            val uri = viewModel.prepareCameraCapture()
            assertNull(uri)
            val event = awaitItem() as MainEvent.ShowError
            assertEquals("Failed to prepare camera file.", event.message)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun writePhotoBytes_callsPhotoProcessor() = runTest {
        val viewModel = MainViewModel(
            locationProvider,
            photoProcessor,
            settingsRepository,
            spotRepository,
            authService,
            syncManager,
            UnconfinedTestDispatcher(testScheduler)
        )
        val bytes = byteArrayOf(1, 2, 3)
        var called = false
        photoProcessor.writeBytesToFileResult = true
        
        val success = viewModel.writePhotoBytes(bytes, "path")
        assertTrue(success)
        assertEquals("path", photoProcessor.writeBytesPath)
        assertTrue(photoProcessor.writeBytesCalled)
    }

    @Test
    fun handleCameraCaptureSuccess_onResultNull_emitsError() = runTest {
        val viewModel = MainViewModel(
            locationProvider,
            photoProcessor,
            settingsRepository,
            spotRepository,
            authService,
            syncManager,
            UnconfinedTestDispatcher(testScheduler)
        )
        photoProcessor.tempCameraFileResult = TempFileDetails("temp_uri", "temp_path")
        viewModel.prepareCameraCapture()
        
        // Make public uri return null to trigger save failure
        photoProcessor.saveToPublicResult = null

        viewModel.events.test {
            viewModel.handleCameraCaptureSuccess()
            val event = awaitItem() as MainEvent.ShowError
            assertEquals("Error saving captured photo.", event.message)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun handleCameraCaptureSuccess_onException_emitsError() = runTest {
        val viewModel = MainViewModel(
            locationProvider,
            photoProcessor,
            settingsRepository,
            spotRepository,
            authService,
            syncManager,
            UnconfinedTestDispatcher(testScheduler)
        )
        photoProcessor.tempCameraFileResult = TempFileDetails("temp_uri", "temp_path")
        viewModel.prepareCameraCapture()
        photoProcessor.saveImageException = Exception("Disk full")

        viewModel.events.test {
            viewModel.handleCameraCaptureSuccess()
            val event = awaitItem() as MainEvent.ShowError
            assertEquals("Failed to process captured photo.", event.message)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun handlePhotoPicked_onException_emitsError() = runTest {
        val viewModel = MainViewModel(
            locationProvider,
            photoProcessor,
            settingsRepository,
            spotRepository,
            authService,
            syncManager,
            UnconfinedTestDispatcher(testScheduler)
        )
        photoProcessor.extractMetadataException = Exception("Metadata fail")

        viewModel.events.test {
            viewModel.handlePhotoPicked("some_uri")
            val event = awaitItem() as MainEvent.ShowError
            assertEquals("Failed to process gallery photo.", event.message)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun importPack_onSuccess_callsOnSuccess() = runTest {
        val viewModel = MainViewModel(
            locationProvider,
            photoProcessor,
            settingsRepository,
            spotRepository,
            authService,
            syncManager,
            UnconfinedTestDispatcher(testScheduler)
        )
        spotRepository.importPackCountToReturn = 5
        
        var successCount: Int? = null
        viewModel.importPack("pack.zip", "files", "cache", onSuccess = { successCount = it }, onError = {})
        testScheduler.advanceUntilIdle()
        
        assertEquals(5, successCount)
        assertEquals("pack.zip", spotRepository.importedPackFilePath)
    }

    @Test
    fun importPack_onError_callsOnError() = runTest {
        val viewModel = MainViewModel(
            locationProvider,
            photoProcessor,
            settingsRepository,
            spotRepository,
            authService,
            syncManager,
            UnconfinedTestDispatcher(testScheduler)
        )
        spotRepository.importPackException = Exception("Import failed")
        
        var caught: Throwable? = null
        viewModel.importPack("pack.zip", "files", "cache", onSuccess = {}, onError = { caught = it })
        testScheduler.advanceUntilIdle()
        
        assertNotNull(caught)
        assertEquals("Import failed", caught?.message)
    }

    @Test
    fun currentUserFlow_triggersRealtimeSync() = runTest {
        authService.setUser(FirebaseUserWrapper("user_123", "user@test.com", "Test User"))
        val viewModel = MainViewModel(
            locationProvider,
            photoProcessor,
            settingsRepository,
            spotRepository,
            authService,
            syncManager,
            UnconfinedTestDispatcher(testScheduler)
        )
        testScheduler.advanceUntilIdle()

        assertEquals("user_123", spotRepository.activeUid)
        assertEquals("user_123", syncManager.startRealtimeSyncUser)

        authService.signOut()
        testScheduler.advanceUntilIdle()

        assertNull(spotRepository.activeUid)
        assertTrue(syncManager.stopRealtimeSyncCalled)
    }
}