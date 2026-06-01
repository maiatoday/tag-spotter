package com.example.tagspotter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tagspotter.data.SettingsRepository
import com.example.tagspotter.data.SpotRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    companion object {
        fun provideFactory(
            settingsRepository: SettingsRepository,
            spotRepository: SpotRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(settingsRepository, spotRepository)
            }
        }
    }
}
