package net.maiatoday.tagspotter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.maiatoday.tagspotter.domain.SecretsProvider
import net.maiatoday.tagspotter.data.SettingsRepository
import net.maiatoday.tagspotter.data.Spot
import net.maiatoday.tagspotter.data.SpotDetails
import net.maiatoday.tagspotter.data.SpotImage
import net.maiatoday.tagspotter.data.SpotRepository
import net.maiatoday.tagspotter.domain.AiRecognitionService
import net.maiatoday.tagspotter.domain.AiSuggestion

class DetailViewModel(
    private val spotId: Long,
    private val repository: SpotRepository,
    private val settingsRepository: SettingsRepository,
    private val aiRecognitionService: AiRecognitionService,
    private val secretsProvider: SecretsProvider,
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

    private val _draftDetails = MutableStateFlow(
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

    val isArtistRecognitionEnabled: StateFlow<Boolean> = settingsRepository.artistRecognitionEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    private val _aiState = MutableStateFlow<AiState>(AiState.Idle)
    val aiState: StateFlow<AiState> = _aiState.asStateFlow()

    fun resetAiState() {
        _aiState.value = AiState.Idle
    }

    private val _wikiSearchState = MutableStateFlow<WikiSearchState>(WikiSearchState.Idle)
    val wikiSearchState: StateFlow<WikiSearchState> = _wikiSearchState.asStateFlow()

    fun resetWikiSearchState() {
        _wikiSearchState.value = WikiSearchState.Idle
    }

    fun identifyArtist(imagePath: String) {
        viewModelScope.launch {
            _aiState.value = AiState.Identifying
            try {
                // 1. Resolve API Key
                var apiKey = secretsProvider.getGeminiApiKey()
                if (apiKey.isEmpty()) {
                    apiKey = settingsRepository.geminiApiKey.first()
                }
                if (apiKey.isEmpty()) {
                    _aiState.value = AiState.Error.MissingKey
                    return@launch
                }
                
                val category = spotDetails.value?.spot?.category ?: "graffiti"
                
                val suggestion = aiRecognitionService.identifyArtist(imagePath, apiKey, category)
                if (suggestion == null) {
                    _aiState.value = AiState.Error.Generic("Failed to load image.")
                    return@launch
                }
                
                _aiState.value = AiState.Success(suggestion)
                
            } catch (e: Exception) {
                e.printStackTrace()
                val message = e.message ?: ""
                val className = e.javaClass.simpleName
                if (className.contains("ResponseStoppedException", ignoreCase = true) || message.contains("stopped", ignoreCase = true)) {
                    _aiState.value = AiState.Error.SafetyBlocked
                } else if (className.contains("ServerException", ignoreCase = true) || message.contains("quota", ignoreCase = true) || message.contains("429", ignoreCase = true)) {
                    _aiState.value = AiState.Error.QuotaExceeded
                } else if (className.contains("InvalidAPIKeyException", ignoreCase = true) || message.contains("API key", ignoreCase = true) || message.contains("403", ignoreCase = true)) {
                    _aiState.value = AiState.Error.InvalidKey
                } else if (message.contains("Unable to resolve host", ignoreCase = true) || message.contains("connect", ignoreCase = true)) {
                    _aiState.value = AiState.Error.Generic("No internet connection. Please connect and try again.")
                } else {
                    _aiState.value = AiState.Error.Generic(message.ifEmpty { "An unexpected error occurred." })
                }
            }
        }
    }

    fun searchWikipediaForSpot() {
        val title = spotDetails.value?.spot?.description ?: ""
        if (title.isBlank()) {
            _wikiSearchState.value = WikiSearchState.Error("No title logged. Please set a title/description first.")
            return
        }

        viewModelScope.launch {
            _wikiSearchState.value = WikiSearchState.Searching
            try {
                // 1. Resolve API Key
                var apiKey = secretsProvider.getGeminiApiKey()
                if (apiKey.isEmpty()) {
                    apiKey = settingsRepository.geminiApiKey.first()
                }
                if (apiKey.isEmpty()) {
                    _wikiSearchState.value = WikiSearchState.Error("Missing Gemini API Key. Please configure it in Settings.")
                    return@launch
                }

                val url = aiRecognitionService.searchWikipediaForSpot(title, apiKey)
                if (!url.isNullOrBlank()) {
                    _wikiSearchState.value = WikiSearchState.Success(url, title)
                } else {
                    _wikiSearchState.value = WikiSearchState.NotFound
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _wikiSearchState.value = WikiSearchState.Error(e.message ?: "An unexpected error occurred.")
            }
        }
    }

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

    fun updateArtworkDate(artworkDate: String) {
        if (spotId != -1L) {
            viewModelScope.launch {
                repository.updateSpotArtworkDate(spotId, artworkDate)
            }
        } else {
            _draftDetails.value = _draftDetails.value?.let {
                it.copy(spot = it.spot.copy(artworkDate = artworkDate))
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

    fun deleteNote(noteId: Long) {
        if (spotId != -1L) {
            viewModelScope.launch {
                repository.deleteNote(noteId)
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

    fun setMainImage(imageId: Long) {
        if (spotId != -1L) {
            viewModelScope.launch {
                repository.setMainImage(spotId, imageId)
            }
        }
    }

    fun deleteImage(image: SpotImage) {
        if (spotId != -1L) {
            viewModelScope.launch {
                repository.deleteImage(image)
            }
        } else {
            val details = _draftDetails.value ?: return
            val updatedImages = details.images.filter { it.imagePath != image.imagePath }
            _draftDetails.value = details.copy(images = updatedImages)
        }
    }

    fun updateImageRating(image: SpotImage, rating: Int) {
        if (spotId != -1L) {
            viewModelScope.launch {
                repository.updateImageRating(image.id, rating)
            }
        } else {
            val details = _draftDetails.value ?: return
            val updatedImages = details.images.map {
                if (it.imagePath == image.imagePath) {
                    it.copy(rating = rating)
                } else {
                    it
                }
            }
            _draftDetails.value = details.copy(images = updatedImages)
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

}

sealed interface AiState {
    object Idle : AiState
    object Identifying : AiState
    data class Success(val suggestion: AiSuggestion) : AiState
    sealed interface Error : AiState {
        object MissingKey : Error
        object InvalidKey : Error
        object QuotaExceeded : Error
        object SafetyBlocked : Error
        data class Generic(val message: String) : Error
    }
}

sealed interface WikiSearchState {
    object Idle : WikiSearchState
    object Searching : WikiSearchState
    data class Success(val url: String, val title: String) : WikiSearchState
    object NotFound : WikiSearchState
    data class Error(val message: String) : WikiSearchState
}
