package net.maiatoday.tagspotter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.maiatoday.tagspotter.data.SettingsRepository
import net.maiatoday.tagspotter.data.SpotRepository
import net.maiatoday.tagspotter.domain.LocationProvider
import net.maiatoday.tagspotter.domain.PhotoProcessor

data class MainUiState(
    val isLoading: Boolean = false,
    val tempPhotoUri: String? = null,
    val tempPhotoFilePath: String? = null,
    val hasLocationPermission: Boolean = false
)

sealed interface MainEvent {
    data class PhotoProcessed(
        val imagePath: String,
        val thumbnailPath: String,
        val latitude: Double,
        val longitude: Double,
        val isFallback: Boolean,
        val category: String,
        val captureTime: Long?
    ) : MainEvent
    data class ShowError(val message: String) : MainEvent
}

class MainViewModel(
    private val locationProvider: LocationProvider,
    private val photoProcessor: PhotoProcessor,
    private val settingsRepository: SettingsRepository,
    private val spotRepository: SpotRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MainEvent>()
    val events: SharedFlow<MainEvent> = _events.asSharedFlow()

    val showTestData: StateFlow<Boolean> = settingsRepository.showTestData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun updateLocationPermission(granted: Boolean) {
        _uiState.update { it.copy(hasLocationPermission = granted) }
    }

    fun prepareCameraCapture(): String? {
        return try {
            val tempDetails = photoProcessor.createTempCameraFile()
            _uiState.update {
                it.copy(
                    tempPhotoUri = tempDetails.uriString,
                    tempPhotoFilePath = tempDetails.fileAbsolutePath
                )
            }
            tempDetails.uriString
        } catch (e: Exception) {
            e.printStackTrace()
            viewModelScope.launch {
                _events.emit(MainEvent.ShowError("Failed to prepare camera file."))
            }
            null
        }
    }

    fun handleCameraCaptureSuccess() {
        val tempPath = _uiState.value.tempPhotoFilePath ?: return
        val hasPermission = _uiState.value.hasLocationPermission

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                var lat = 0.0
                var lng = 0.0
                var isFallback = true

                if (hasPermission) {
                    val loc = locationProvider.getCurrentLocation()
                    if (loc != null) {
                        lat = loc.latitude
                        lng = loc.longitude
                        isFallback = loc.isFallback
                    }
                }

                // Process in IO Dispatcher
                val result = withContext(ioDispatcher) {
                    val publicUri = photoProcessor.saveImageToPublicGallery(tempPath)
                    val thumbnail = photoProcessor.createThumbnailFromFile(tempPath)
                    // Clean up temporary cache file
                    photoProcessor.deleteFile(tempPath)
                    if (publicUri != null && thumbnail != null) {
                        publicUri to thumbnail
                    } else {
                        null
                    }
                }

                _uiState.update { it.copy(isLoading = false) }

                if (result != null) {
                    val (publicUri, thumbnail) = result
                    _events.emit(
                        MainEvent.PhotoProcessed(
                            imagePath = publicUri,
                            thumbnailPath = thumbnail,
                            latitude = lat,
                            longitude = lng,
                            isFallback = isFallback,
                            category = "All",
                            captureTime = null
                        )
                    )
                } else {
                    _events.emit(MainEvent.ShowError("Error saving captured photo."))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(MainEvent.ShowError("Failed to process captured photo."))
            }
        }
    }

    fun handlePhotoPicked(uriString: String) {
        val hasPermission = _uiState.value.hasLocationPermission

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                var lat = 0.0
                var lng = 0.0
                var isFallback = true
                var captureTime: Long? = null

                // Process metadata and thumbnail on IO Dispatcher
                val result = withContext(ioDispatcher) {
                    val metadata = photoProcessor.extractMetadataFromUri(uriString)
                    val thumbnail = photoProcessor.createThumbnailFromUri(uriString)
                    metadata to thumbnail
                }

                val metadata = result.first
                val thumbnail = result.second

                if (metadata != null) {
                    if (metadata.latitude != null && metadata.longitude != null) {
                        lat = metadata.latitude
                        lng = metadata.longitude
                        isFallback = false
                    } else if (hasPermission) {
                        val loc = locationProvider.getCurrentLocation()
                        if (loc != null) {
                            lat = loc.latitude
                            lng = loc.longitude
                            isFallback = loc.isFallback
                        }
                    }
                    captureTime = metadata.timestamp
                } else if (hasPermission) {
                    val loc = locationProvider.getCurrentLocation()
                    if (loc != null) {
                        lat = loc.latitude
                        lng = loc.longitude
                        isFallback = loc.isFallback
                    }
                }

                _uiState.update { it.copy(isLoading = false) }

                if (thumbnail != null) {
                    _events.emit(
                        MainEvent.PhotoProcessed(
                            imagePath = uriString,
                            thumbnailPath = thumbnail,
                            latitude = lat,
                            longitude = lng,
                            isFallback = isFallback,
                            category = "All",
                            captureTime = captureTime
                        )
                    )
                } else {
                    _events.emit(MainEvent.ShowError("Error processing gallery image."))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(MainEvent.ShowError("Failed to process gallery photo."))
            }
        }
    }

    fun updateShowTestData(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowTestData(show)
            if (show) {
                spotRepository.loadTestData()
            } else {
                spotRepository.unloadTestData()
            }
        }
    }

    companion object {
        fun provideFactory(
            locationProvider: LocationProvider,
            photoProcessor: PhotoProcessor,
            settingsRepository: SettingsRepository,
            spotRepository: SpotRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MainViewModel(
                    locationProvider,
                    photoProcessor,
                    settingsRepository,
                    spotRepository
                )
            }
        }
    }
}
