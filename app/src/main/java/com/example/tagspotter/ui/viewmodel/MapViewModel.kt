package com.example.tagspotter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tagspotter.TagSpotterApplication
import com.example.tagspotter.data.SpotDetails
import com.example.tagspotter.data.SpotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class MapViewModel(private val repository: SpotRepository) : ViewModel() {

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedSpot = MutableStateFlow<SpotDetails?>(null)
    val selectedSpot: StateFlow<SpotDetails?> = _selectedSpot.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val spots: StateFlow<List<SpotDetails>> = _selectedCategory
        .flatMapLatest { category ->
            repository.getSpotsByCategory(category)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun selectSpot(spot: SpotDetails?) {
        _selectedSpot.value = spot
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TagSpotterApplication
                MapViewModel(app.repository)
            }
        }
    }
}
