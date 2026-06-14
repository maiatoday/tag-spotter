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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherExtension = MainDispatcherExtension()

    private val settingsRepository = FakeSettingsRepository("Initial Photographer", "Milan")
    private val spotRepository = FakeSpotRepository()
    private val locationProvider = FakeLocationProvider()
    private val photoProcessor = FakePhotoProcessor()

    @Test
    fun updateLocationPermission_updatesState() = runTest {
        val viewModel = MainViewModel(
            locationProvider,
            photoProcessor,
            settingsRepository,
            spotRepository,
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
            UnconfinedTestDispatcher(testScheduler)
        )

        // Set temp file details
        photoProcessor.tempCameraFileResult = TempFileDetails("temp_uri", "temp_path")
        viewModel.prepareCameraCapture()

        locationProvider.locationToReturn = LocationData(45.4642, 9.1900, false)
        viewModel.updateLocationPermission(true)

        val eventsList = mutableListOf<MainEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(eventsList)
        }

        viewModel.handleCameraCaptureSuccess()

        // Verify that loading was toggled and temp file was deleted
        assertEquals("temp_path", photoProcessor.deleteFileCalledWith)
        assertEquals("temp_path", photoProcessor.saveImageCalledWith)
        assertFalse(viewModel.uiState.value.isLoading)

        // Verify emitted event
        assertEquals(1, eventsList.size)
        val event = eventsList[0] as MainEvent.PhotoProcessed
        assertEquals("public_uri", event.imagePath)
        assertEquals("thumb_path", event.thumbnailPath)
        assertEquals(45.4642, event.latitude, 0.0001)
        assertEquals(9.1900, event.longitude, 0.0001)
        assertFalse(event.isFallback)
    }

    @Test
    fun handlePhotoPicked_extractsExifAndEmitsProcessedEvent() = runTest {
        val viewModel = MainViewModel(
            locationProvider,
            photoProcessor,
            settingsRepository,
            spotRepository,
            UnconfinedTestDispatcher(testScheduler)
        )

        photoProcessor.metadataResult = PhotoMetadata(45.4642, 9.1900, 123456789L)

        val eventsList = mutableListOf<MainEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(eventsList)
        }

        viewModel.handlePhotoPicked("some_uri")

        assertFalse(viewModel.uiState.value.isLoading)

        // Verify emitted event
        assertEquals(1, eventsList.size)
        val event = eventsList[0] as MainEvent.PhotoProcessed
        assertEquals("some_uri", event.imagePath)
        assertEquals("thumb_path", event.thumbnailPath)
        assertEquals(45.4642, event.latitude, 0.0001)
        assertEquals(9.1900, event.longitude, 0.0001)
        assertFalse(event.isFallback)
        assertEquals(123456789L, event.captureTime)
    }

    @Test
    fun handlePhotoPicked_withoutExif_usesLocationFallback() = runTest {
        val viewModel = MainViewModel(
            locationProvider,
            photoProcessor,
            settingsRepository,
            spotRepository,
            UnconfinedTestDispatcher(testScheduler)
        )

        photoProcessor.metadataResult = PhotoMetadata(null, null, null)
        locationProvider.locationToReturn = LocationData(45.4642, 9.1900, true)
        viewModel.updateLocationPermission(true)

        val eventsList = mutableListOf<MainEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(eventsList)
        }

        viewModel.handlePhotoPicked("some_uri")

        // Verify fallback coordinates
        assertEquals(1, eventsList.size)
        val event = eventsList[0] as MainEvent.PhotoProcessed
        assertEquals(45.4642, event.latitude, 0.0001)
        assertEquals(9.1900, event.longitude, 0.0001)
        assertTrue(event.isFallback)
    }

    @Test
    fun updateShowTestData_updatesRepository() = runTest {
        val viewModel = MainViewModel(
            locationProvider,
            photoProcessor,
            settingsRepository,
            spotRepository,
            UnconfinedTestDispatcher(testScheduler)
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.showTestData.collect {}
        }

        assertFalse(viewModel.showTestData.value)

        viewModel.updateShowTestData(true)
        assertTrue(viewModel.showTestData.value)
    }
}