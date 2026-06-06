package net.maiatoday.tagspotter.ui.viewmodel

import android.graphics.Bitmap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.maiatoday.tagspotter.MainDispatcherRule
import net.maiatoday.tagspotter.data.FakeSettingsRepository
import net.maiatoday.tagspotter.data.FakeSpotRepository
import net.maiatoday.tagspotter.domain.LocationData
import net.maiatoday.tagspotter.domain.LocationProvider
import net.maiatoday.tagspotter.domain.PhotoMetadata
import net.maiatoday.tagspotter.domain.PhotoProcessor
import net.maiatoday.tagspotter.domain.TempFileDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settingsRepository = FakeSettingsRepository("Initial Photographer", "Milan")
    private val spotRepository = FakeSpotRepository()
    private val locationProvider = FakeLocationProvider()
    private val photoProcessor = FakePhotoProcessor()

    @Test
    fun updateLocationPermission_updatesState() = runTest {
        val viewModel = MainViewModel(locationProvider, photoProcessor, settingsRepository, spotRepository, UnconfinedTestDispatcher(testScheduler))

        assertFalse(viewModel.uiState.value.hasLocationPermission)

        viewModel.updateLocationPermission(true)
        assertTrue(viewModel.uiState.value.hasLocationPermission)

        viewModel.updateLocationPermission(false)
        assertFalse(viewModel.uiState.value.hasLocationPermission)
    }

    @Test
    fun prepareCameraCapture_setsTempPhotoDetails() = runTest {
        val viewModel = MainViewModel(locationProvider, photoProcessor, settingsRepository, spotRepository, UnconfinedTestDispatcher(testScheduler))

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
        val viewModel = MainViewModel(locationProvider, photoProcessor, settingsRepository, spotRepository, UnconfinedTestDispatcher(testScheduler))

        // Set temp file details
        photoProcessor.tempCameraFileResult = TempFileDetails("temp_uri", "temp_path")
        viewModel.prepareCameraCapture()

        locationProvider.locationToReturn = LocationData(45.4642, 9.1900, false)
        viewModel.updateLocationPermission(true)

        val eventsList = mutableListOf<MainEvent>()
        val eventsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
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
        val viewModel = MainViewModel(locationProvider, photoProcessor, settingsRepository, spotRepository, UnconfinedTestDispatcher(testScheduler))

        photoProcessor.metadataResult = PhotoMetadata(45.4642, 9.1900, 123456789L)

        val eventsList = mutableListOf<MainEvent>()
        val eventsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
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
        val viewModel = MainViewModel(locationProvider, photoProcessor, settingsRepository, spotRepository, UnconfinedTestDispatcher(testScheduler))

        photoProcessor.metadataResult = PhotoMetadata(null, null, null)
        locationProvider.locationToReturn = LocationData(45.4642, 9.1900, true)
        viewModel.updateLocationPermission(true)

        val eventsList = mutableListOf<MainEvent>()
        val eventsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
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
        val viewModel = MainViewModel(locationProvider, photoProcessor, settingsRepository, spotRepository, UnconfinedTestDispatcher(testScheduler))

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.showTestData.collect {}
        }

        assertFalse(viewModel.showTestData.value)

        viewModel.updateShowTestData(true)
        assertTrue(viewModel.showTestData.value)
    }

    // Fakes for testing
    private class FakeLocationProvider(var locationToReturn: LocationData? = null) : LocationProvider {
        override suspend fun getCurrentLocation(): LocationData? = locationToReturn
    }

    private class FakePhotoProcessor : PhotoProcessor {
        var saveToPublicResult: String? = "public_uri"
        var createThumbFileResult: String? = "thumb_path"
        var createThumbUriResult: String? = "thumb_path"
        var metadataResult: PhotoMetadata? = PhotoMetadata(12.34, 56.78, 123456789L)
        var deleteFileResult: Boolean = true
        var tempCameraFileResult = TempFileDetails("temp_uri", "temp_path")

        var deleteFileCalledWith: String? = null
        var saveImageCalledWith: String? = null

        override suspend fun saveImageToPublicGallery(filePath: String): String? {
            saveImageCalledWith = filePath
            return saveToPublicResult
        }

        override suspend fun createThumbnailFromFile(filePath: String): String? {
            return createThumbFileResult
        }

        override suspend fun createThumbnailFromUri(uriString: String): String? {
            return createThumbUriResult
        }

        override suspend fun extractMetadataFromUri(uriString: String): PhotoMetadata? {
            return metadataResult
        }

        override fun createTempCameraFile(): TempFileDetails {
            return tempCameraFileResult
        }

        override fun deleteFile(filePath: String): Boolean {
            deleteFileCalledWith = filePath
            return deleteFileResult
        }

        override suspend fun decodeScaledBitmap(imagePath: String, maxDimension: Int): Bitmap? {
            return null
        }
    }
}
