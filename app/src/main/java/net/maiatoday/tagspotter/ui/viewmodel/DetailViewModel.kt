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
import net.maiatoday.tagspotter.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DetailViewModel(
    private val spotId: Long,
    private val repository: SpotRepository,
    private val settingsRepository: SettingsRepository,
    draftImagePath: String? = null,
    draftThumbnailPath: String? = null,
    draftLatitude: Double? = null,
    draftLongitude: Double? = null,
    draftCategory: String? = null,
    draftCaptureTime: Long? = null,
    private val buildConfigApiKey: String = BuildConfig.GEMINI_API_KEY
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

    private suspend fun decodeScaledBitmap(context: Context?, imagePath: String, maxDimension: Int): Bitmap? = withContext(Dispatchers.IO) {
        try {
            if (context == null || (!imagePath.startsWith("content://") && !imagePath.startsWith("file://"))) {
                val file = File(imagePath)
                if (!file.exists()) return@withContext null
                
                // Get dimensions
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(imagePath, options)
                
                val srcWidth = options.outWidth
                val srcHeight = options.outHeight
                if (srcWidth <= 0 || srcHeight <= 0) return@withContext null
                
                // Calculate sample size (power of 2)
                var inSampleSize = 1
                val maxSrcDim = maxOf(srcWidth, srcHeight)
                while (maxSrcDim / (inSampleSize * 2) >= maxDimension) {
                    inSampleSize *= 2
                }
                
                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                }
                BitmapFactory.decodeFile(imagePath, decodeOptions)
            } else {
                val uri = Uri.parse(imagePath)
                // Get dimensions
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, options)
                }
                
                val srcWidth = options.outWidth
                val srcHeight = options.outHeight
                if (srcWidth <= 0 || srcHeight <= 0) return@withContext null
                
                // Calculate sample size (power of 2)
                var inSampleSize = 1
                val maxSrcDim = maxOf(srcWidth, srcHeight)
                while (maxSrcDim / (inSampleSize * 2) >= maxDimension) {
                    inSampleSize *= 2
                }
                
                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                }
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, decodeOptions)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun identifyArtist(imagePath: String, context: Context? = null) {
        val appContext = context?.applicationContext
        viewModelScope.launch {
            _aiState.value = AiState.Identifying
            try {
                // 1. Resolve API Key
                var apiKey = buildConfigApiKey
                if (apiKey.isEmpty()) {
                    apiKey = settingsRepository.geminiApiKey.first()
                }
                if (apiKey.isEmpty()) {
                    _aiState.value = AiState.Error.MissingKey
                    return@launch
                }
                
                // 2. Load and downscale image
                val bitmap = decodeScaledBitmap(appContext, imagePath, 1024)
                if (bitmap == null) {
                    _aiState.value = AiState.Error.Generic("Failed to load image.")
                    return@launch
                }
                
                // 3. Initialize Gemini
                val model = GenerativeModel(
                    modelName = "gemini-2.5-flash",
                    apiKey = apiKey
                )
                
                val category = spotDetails.value?.spot?.category ?: "graffiti"
                val artistRoleDescription = when (category) {
                    "sculpture" -> "sculptor, artist, designer, or creator"
                    "architecture" -> "architect, designer, or builder"
                    "nature" -> "landscape artist, gardener, designer, or photographer"
                    "public_place" -> "artist, sculptor, architect, designer, or creator"
                    else -> "street art artist, graffiti writer, crew, or painter"
                }

                val prompt = """
                    Analyze this image of a spot in the category: "$category".
                    Identify the $artistRoleDescription (if known), suggest a title for the art/spot, and suggest tags (from: mural, stencil, throwup, pasteup, sticker, or others appropriate for this category).
                    ${if (category == "nature") "Specifically, since the category is \"nature\", for the \"title\" field try to identify the specific plant, flower, tree species, or geological/natural feature visible in the image." else ""}
                    Return the response in strict JSON format using exactly these keys:
                    {
                      "artist": "Name or null",
                      "title": "Suggested Title or null",
                      "tags": ["tag1", "tag2"]
                    }
                    If you do not know the artist/creator/architect, set the "artist" field to null. If you cannot suggest a title, set the "title" field to null.
                    Do not add markdown formatting or backticks around the JSON. Return only the raw JSON.
                """.trimIndent()
                
                val response = model.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )
                
                val responseText = response.text ?: ""
                if (responseText.isEmpty()) {
                    _aiState.value = AiState.Error.Generic("Empty response from AI model.")
                    return@launch
                }
                
                // Clean the JSON string (in case markdown backticks were returned)
                val cleanJson = if (responseText.contains("```")) {
                    responseText
                        .substringAfter("```json")
                        .substringAfter("```")
                        .substringBefore("```")
                        .trim()
                } else {
                    responseText.trim()
                }
                
                // Parse suggestion
                val suggestion = try {
                    Json.decodeFromString<AiSuggestion>(cleanJson)
                } catch (e: Exception) {
                    e.printStackTrace()
                    _aiState.value = AiState.Error.Generic("Failed to parse AI response.")
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

@Serializable
data class AiSuggestion(
    val artist: String? = null,
    val title: String? = null,
    val tags: List<String> = emptyList()
)

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

