package net.maiatoday.tagspotter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.maiatoday.tagspotter.data.SettingsRepository
import net.maiatoday.tagspotter.data.SpotRepository

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val spotRepository: SpotRepository
) : ViewModel() {

    val photographerName: StateFlow<String> = settingsRepository.photographerName
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    val homeCity: StateFlow<String> = settingsRepository.homeCity
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Milan"
        )

    val showTestData: StateFlow<Boolean> = settingsRepository.showTestData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val notificationsEnabled: StateFlow<Boolean> = settingsRepository.notificationsEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val darkMapEnabled: StateFlow<Boolean> = settingsRepository.darkMapEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun updatePhotographerName(name: String) {
        viewModelScope.launch {
            settingsRepository.updatePhotographerName(name)
        }
    }

    fun updateHomeCity(city: String) {
        viewModelScope.launch {
            settingsRepository.updateHomeCity(city)
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

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateNotificationsEnabled(enabled)
        }
    }

    val artistRecognitionEnabled: StateFlow<Boolean> = settingsRepository.artistRecognitionEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val geminiApiKey: StateFlow<String> = settingsRepository.geminiApiKey
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    fun updateDarkMapEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDarkMapEnabled(enabled)
        }
    }

    fun updateArtistRecognitionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateArtistRecognitionEnabled(enabled)
        }
    }

    fun updateGeminiApiKey(key: String) {
        viewModelScope.launch {
            settingsRepository.updateGeminiApiKey(key)
        }
    }

}
