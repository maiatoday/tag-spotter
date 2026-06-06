package net.maiatoday.tagspotter.feature.main

import android.graphics.Bitmap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.maiatoday.tagspotter.MainDispatcherRule
import net.maiatoday.tagspotter.core.location.LocationData
import net.maiatoday.tagspotter.core.location.LocationProvider
import net.maiatoday.tagspotter.core.photo.PhotoMetadata
import net.maiatoday.tagspotter.core.photo.PhotoProcessor
import net.maiatoday.tagspotter.core.photo.TempFileDetails
import net.maiatoday.tagspotter.core.settings.FakeSettingsRepository
import net.maiatoday.tagspotter.core.database.FakeSpotRepository
import org.junit.Assert
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
        val viewModel = MainViewModel(
            locationProvider,
            photoProcessor,
            settingsRepository,
            spotRepository,
            UnconfinedTestDispatcher(testScheduler)
        )

        Assert.assertFalse(viewModel.uiState.value.hasLocationPermission)

        viewModel.updateLocationPermission(true)
        Assert.assertTrue(viewModel.uiState.value.hasLocationPermission)

        viewModel.updateLocationPermission(false)
        Assert.assertFalse(viewModel.uiState.value.hasLocationPermission)
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

        Assert.assertNull(viewModel.uiState.value.tempPhotoUri)
        Assert.assertNull(viewModel.uiState.value.tempPhotoFilePath)

        val expectedUri = "temp_uri"
        photoProcessor.tempCameraFileResult = TempFileDetails(expectedUri, "temp_path")

        val returnedUri = viewModel.prepareCameraCapture()

        Assert.assertEquals(expectedUri, returnedUri)
        Assert.assertEquals(expectedUri, viewModel.uiState.value.tempPhotoUri)
        Assert.assertEquals("temp_path", viewModel.uiState.value.tempPhotoFilePath)
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
        val eventsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(eventsList)
        }

        viewModel.handleCameraCaptureSuccess()

        // Verify that loading was toggled and temp file was deleted
        Assert.assertEquals("temp_path", photoProcessor.deleteFileCalledWith)
        Assert.assertEquals("temp_path", photoProcessor.saveImageCalledWith)
        Assert.assertFalse(viewModel.uiState.value.isLoading)

        // Verify emitted event
        Assert.assertEquals(1, eventsList.size)
        val event = eventsList[0] as MainEvent.PhotoProcessed
        Assert.assertEquals("public_uri", event.imagePath)
        Assert.assertEquals("thumb_path", event.thumbnailPath)
        Assert.assertEquals(45.4642, event.latitude, 0.0001)
        Assert.assertEquals(9.1900, event.longitude, 0.0001)
        Assert.assertFalse(event.isFallback)
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
        val eventsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(eventsList)
        }

        viewModel.handlePhotoPicked("some_uri")

        Assert.assertFalse(viewModel.uiState.value.isLoading)

        // Verify emitted event
        Assert.assertEquals(1, eventsList.size)
        val event = eventsList[0] as MainEvent.PhotoProcessed
        Assert.assertEquals("some_uri", event.imagePath)
        Assert.assertEquals("thumb_path", event.thumbnailPath)
        Assert.assertEquals(45.4642, event.latitude, 0.0001)
        Assert.assertEquals(9.1900, event.longitude, 0.0001)
        Assert.assertFalse(event.isFallback)
        Assert.assertEquals(123456789L, event.captureTime)
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
        val eventsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(eventsList)
        }

        viewModel.handlePhotoPicked("some_uri")

        // Verify fallback coordinates
        Assert.assertEquals(1, eventsList.size)
        val event = eventsList[0] as MainEvent.PhotoProcessed
        Assert.assertEquals(45.4642, event.latitude, 0.0001)
        Assert.assertEquals(9.1900, event.longitude, 0.0001)
        Assert.assertTrue(event.isFallback)
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

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.showTestData.collect {}
        }

        Assert.assertFalse(viewModel.showTestData.value)

        viewModel.updateShowTestData(true)
        Assert.assertTrue(viewModel.showTestData.value)
    }

    // Fakes for testing
    private class FakeLocationProvider(var locationToReturn: LocationData? = null) :
        LocationProvider {
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