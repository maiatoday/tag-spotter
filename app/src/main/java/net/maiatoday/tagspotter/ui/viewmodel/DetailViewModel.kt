package net.maiatoday.tagspotter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import net.maiatoday.tagspotter.data.SettingsRepository
import net.maiatoday.tagspotter.data.Spot
import net.maiatoday.tagspotter.data.SpotImage
import net.maiatoday.tagspotter.data.SpotDetails
import net.maiatoday.tagspotter.data.SpotRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class DetailViewModel(
    private val spotId: Long,
    private val repository: SpotRepository,
    settingsRepository: SettingsRepository,
    draftImagePath: String? = null,
    draftThumbnailPath: String? = null,
    draftLatitude: Double? = null,
    draftLongitude: Double? = null,
    draftCategory: String? = null,
    draftCaptureTime: Long? = null
) : ViewModel() {

    sealed interface UiEvent {
        data object StarLimitExceeded : UiEvent
    }

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _draftDetails = MutableStateFlow<SpotDetails?>(
        if (spotId == -1L) {
            SpotDetails(
                spot = Spot(
                    latitude = draftLatitude ?: 0.0,
                    longitude = draftLongitude ?: 0.0,
                    createdAt = draftCaptureTime ?: System.currentTimeMillis(),
                    description = "",
                    tags = emptyList(),
                    category = draftCategory ?: "graffiti",
                    status = "active",
                    artists = emptyList(),
                    photographer = ""
                ),
                images = if (draftImagePath != null && draftThumbnailPath != null) {
                    listOf(
                        SpotImage(
                            spotId = 0,
                            imagePath = draftImagePath,
                            thumbnailPath = draftThumbnailPath,
                            timestamp = draftCaptureTime ?: System.currentTimeMillis()
                        )
                    )
                } else emptyList(),
                notes = emptyList()
            )
        } else null
    )

    val spotDetails: StateFlow<SpotDetails?> = if (spotId != -1L) {
        repository.getSpotById(spotId)
    } else {
        _draftDetails
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = if (spotId == -1L) _draftDetails.value else null
    )

    val defaultPhotographer: StateFlow<String> = settingsRepository.photographerName
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    private val predefinedTags = setOf("mural", "stencil", "throwup", "pasteup", "sticker")
    val recentCustomTags: StateFlow<List<String>> = repository.getRecentCustomTags(predefinedTags)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Initialize photographer from settings repository if in draft mode
        if (spotId == -1L) {
            viewModelScope.launch {
                settingsRepository.photographerName.collect { name ->
                    _draftDetails.value = _draftDetails.value?.let {
                        if (it.spot.photographer.isEmpty()) {
                            it.copy(spot = it.spot.copy(photographer = name))
                        } else {
                            it
                        }
                    }
                }
            }
        }
    }

    fun toggleStarred() {
        val details = spotDetails.value ?: return
        val currentStarred = details.spot.isStarred
        viewModelScope.launch {
            if (!currentStarred) {
                val count = repository.getStarredSpotsCount()
                if (count >= 100) {
                    _uiEvent.emit(UiEvent.StarLimitExceeded)
                    return@launch
                }
            }
            if (spotId != -1L) {
                repository.updateSpotStarred(spotId, !currentStarred)
            } else {
                _draftDetails.value = details.copy(spot = details.spot.copy(isStarred = !currentStarred))
            }
        }
    }

    fun updateStatus(status: String) {
        if (spotId != -1L) {
            viewModelScope.launch {
                repository.updateSpotStatus(spotId, status)
            }
        } else {
            _draftDetails.value = _draftDetails.value?.let {
                it.copy(spot = it.spot.copy(status = status))
            }
        }
    }

    fun updateCategory(category: String) {
        if (spotId != -1L) {
            viewModelScope.launch {
                repository.updateSpotCategory(spotId, category)
            }
        } else {
            _draftDetails.value = _draftDetails.value?.let {
                it.copy(spot = it.spot.copy(category = category))
            }
        }
    }

    fun updateArtists(artists: List<String>) {
        if (spotId != -1L) {
            viewModelScope.launch {
                repository.updateSpotArtists(spotId, artists)
            }
        } else {
            _draftDetails.value = _draftDetails.value?.let {
                it.copy(spot = it.spot.copy(artists = artists))
            }
        }
    }

    fun updatePhotographer(photographer: String) {
        if (spotId != -1L) {
            viewModelScope.launch {
                repository.updateSpotPhotographer(spotId, photographer)
            }
        } else {
            _draftDetails.value = _draftDetails.value?.let {
                it.copy(spot = it.spot.copy(photographer = photographer))
            }
        }
    }

    fun updateTags(tags: List<String>) {
        if (spotId != -1L) {
            viewModelScope.launch {
                repository.updateSpotTags(spotId, tags)
            }
        } else {
            _draftDetails.value = _draftDetails.value?.let {
                it.copy(spot = it.spot.copy(tags = tags))
            }
        }
    }

    fun updateLocation(latitude: Double, longitude: Double) {
        if (spotId != -1L) {
            viewModelScope.launch {
                repository.updateSpotLocation(spotId, latitude, longitude)
            }
        } else {
            _draftDetails.value = _draftDetails.value?.let {
                it.copy(spot = it.spot.copy(latitude = latitude, longitude = longitude))
            }
        }
    }

    fun updateDescription(description: String) {
        if (spotId != -1L) {
            viewModelScope.launch {
                repository.updateSpotDescription(spotId, description)
            }
        } else {
            _draftDetails.value = _draftDetails.value?.let {
                it.copy(spot = it.spot.copy(description = description))
            }
        }
    }

    fun addNote(noteText: String, timestamp: Long) {
        if (spotId != -1L) {
            viewModelScope.launch {
                repository.addNoteToSpot(spotId, noteText, timestamp)
            }
        }
    }

    fun addImage(imagePath: String, thumbnailPath: String, timestamp: Long) {
        if (spotId != -1L) {
            viewModelScope.launch {
                repository.addImageToSpot(spotId, imagePath, thumbnailPath, timestamp)
            }
        } else {
            val details = _draftDetails.value ?: return
            val newImage = SpotImage(
                id = 0L,
                spotId = 0L,
                imagePath = imagePath,
                thumbnailPath = thumbnailPath,
                timestamp = timestamp
            )
            _draftDetails.value = details.copy(images = details.images + newImage)
        }
    }

    fun deleteSpot(spotDetails: SpotDetails, onDeleted: () -> Unit) {
        if (spotId != -1L) {
            viewModelScope.launch {
                repository.deleteSpot(spotDetails)
                onDeleted()
            }
        }
    }

    fun saveSpot(onSaved: (Long) -> Unit) {
        val details = _draftDetails.value ?: return
        val firstImage = details.images.firstOrNull() ?: return
        viewModelScope.launch {
            val newSpotId = repository.saveSpot(details.spot, firstImage.imagePath, firstImage.thumbnailPath)
            
            if (details.images.size > 1) {
                details.images.drop(1).forEach { extraImage ->
                    repository.addImageToSpot(newSpotId, extraImage.imagePath, extraImage.thumbnailPath, extraImage.timestamp)
                }
            }
            
            if (details.spot.isStarred) {
                repository.updateSpotStarred(newSpotId, true)
            }
            onSaved(newSpotId)
        }
    }

    companion object {
        fun provideFactory(
            spotId: Long,
            repository: SpotRepository,
            settingsRepository: SettingsRepository,
            draftImagePath: String? = null,
            draftThumbnailPath: String? = null,
            draftLatitude: Double? = null,
            draftLongitude: Double? = null,
            draftCategory: String? = null,
            draftCaptureTime: Long? = null
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DetailViewModel(
                    spotId,
                    repository,
                    settingsRepository,
                    draftImagePath,
                    draftThumbnailPath,
                    draftLatitude,
                    draftLongitude,
                    draftCategory,
                    draftCaptureTime
                )
            }
        }
    }
}
