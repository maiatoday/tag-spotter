package net.maiatoday.tagspotter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import net.maiatoday.tagspotter.TagSpotterApplication
import net.maiatoday.tagspotter.data.SettingsRepository
import net.maiatoday.tagspotter.data.SpotDetails
import net.maiatoday.tagspotter.data.SpotRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DetailViewModel(
    private val spotId: Long,
    private val repository: SpotRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val spotDetails: StateFlow<SpotDetails?> = repository.getSpotById(spotId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val defaultPhotographer: StateFlow<String> = settingsRepository.photographerName
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    fun updateStatus(status: String) {
        viewModelScope.launch {
            repository.updateSpotStatus(spotId, status)
        }
    }

    fun updateArtists(artists: List<String>) {
        viewModelScope.launch {
            repository.updateSpotArtists(spotId, artists)
        }
    }

    fun updatePhotographer(photographer: String) {
        viewModelScope.launch {
            repository.updateSpotPhotographer(spotId, photographer)
        }
    }

    fun updateLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            repository.updateSpotLocation(spotId, latitude, longitude)
        }
    }

    fun updateDescription(description: String) {
        viewModelScope.launch {
            repository.updateSpotDescription(spotId, description)
        }
    }

    fun addNote(noteText: String, timestamp: Long) {
        viewModelScope.launch {
            repository.addNoteToSpot(spotId, noteText, timestamp)
        }
    }

    fun addImage(imagePath: String, thumbnailPath: String, timestamp: Long) {
        viewModelScope.launch {
            repository.addImageToSpot(spotId, imagePath, thumbnailPath, timestamp)
        }
    }

    fun deleteSpot(spotDetails: SpotDetails, onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteSpot(spotDetails)
            onDeleted()
        }
    }

    companion object {
        fun provideFactory(
            spotId: Long,
            repository: SpotRepository,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DetailViewModel(spotId, repository, settingsRepository)
            }
        }
    }
}
