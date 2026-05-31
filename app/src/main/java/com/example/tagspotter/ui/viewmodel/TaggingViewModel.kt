package com.example.tagspotter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tagspotter.TagSpotterApplication
import com.example.tagspotter.data.SettingsRepository
import com.example.tagspotter.data.Spot
import com.example.tagspotter.data.SpotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaggingViewModel(
    private val repository: SpotRepository,
    private val settingsRepository: SettingsRepository,
    initialLat: Double,
    initialLng: Double,
    initialCategory: String
) : ViewModel() {

    private val _currentLat = MutableStateFlow(initialLat)
    val currentLat: StateFlow<Double> = _currentLat.asStateFlow()

    private val _currentLng = MutableStateFlow(initialLng)
    val currentLng: StateFlow<Double> = _currentLng.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _selectedCategory = MutableStateFlow(initialCategory)
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _photographer = MutableStateFlow("")
    val photographer: StateFlow<String> = _photographer.asStateFlow()

    private val _selectedArtists = MutableStateFlow<List<String>>(emptyList())
    val selectedArtists: StateFlow<List<String>> = _selectedArtists.asStateFlow()

    private val _selectedTags = MutableStateFlow<List<String>>(emptyList())
    val selectedTags: StateFlow<List<String>> = _selectedTags.asStateFlow()

    private val _artistInput = MutableStateFlow("")
    val artistInput: StateFlow<String> = _artistInput.asStateFlow()

    private val _customTagInput = MutableStateFlow("")
    val customTagInput: StateFlow<String> = _customTagInput.asStateFlow()

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
        // Initialize photographer from settings repository
        viewModelScope.launch {
            settingsRepository.photographerName.collect { name ->
                if (_photographer.value.isEmpty()) {
                    _photographer.value = name
                }
            }
        }
    }

    fun updateCoordinates(lat: Double, lng: Double) {
        _currentLat.value = lat
        _currentLng.value = lng
    }

    fun updateDescription(desc: String) {
        _description.value = desc
    }

    fun updateCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updatePhotographer(name: String) {
        _photographer.value = name
    }

    fun updateArtistInput(input: String) {
        _artistInput.value = input
    }

    fun updateCustomTagInput(input: String) {
        _customTagInput.value = input
    }

    fun addArtist(artist: String) {
        val cleaned = artist.trim()
        if (cleaned.isNotEmpty()) {
            val current = _selectedArtists.value
            if (!current.contains(cleaned)) {
                _selectedArtists.value = current + cleaned
            }
            _artistInput.value = ""
        }
    }

    fun removeArtist(artist: String) {
        _selectedArtists.value = _selectedArtists.value - artist
    }

    fun addTag(tag: String) {
        val cleaned = tag.trim().lowercase()
        if (cleaned.isNotEmpty()) {
            val current = _selectedTags.value
            if (!current.contains(cleaned)) {
                _selectedTags.value = current + cleaned
            }
        }
    }

    fun removeTag(tag: String) {
        _selectedTags.value = _selectedTags.value - tag
    }

    fun saveSpot(imagePath: String, onSaved: () -> Unit) {
        viewModelScope.launch {
            val spot = Spot(
                latitude = _currentLat.value,
                longitude = _currentLng.value,
                createdAt = System.currentTimeMillis(),
                description = _description.value.trim(),
                tags = _selectedTags.value,
                category = _selectedCategory.value,
                status = "active",
                artists = _selectedArtists.value,
                photographer = _photographer.value.trim()
            )
            repository.saveSpot(spot, imagePath)
            onSaved()
        }
    }

    companion object {
        fun provideFactory(
            repository: SpotRepository,
            settingsRepository: SettingsRepository,
            initialLat: Double,
            initialLng: Double,
            initialCategory: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                TaggingViewModel(repository, settingsRepository, initialLat, initialLng, initialCategory)
            }
        }
    }
}
