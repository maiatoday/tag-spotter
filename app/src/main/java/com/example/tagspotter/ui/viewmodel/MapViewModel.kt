package com.example.tagspotter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tagspotter.TagSpotterApplication
import com.example.tagspotter.data.SettingsRepository
import com.example.tagspotter.data.SpotDetails
import com.example.tagspotter.data.SpotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import org.osmdroid.util.GeoPoint

class MapViewModel(
    private val repository: SpotRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

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

    private fun resolveCityCoordinate(homeCityName: String): GeoPoint {
        if (homeCityName.startsWith("Custom:")) {
            try {
                val coords = homeCityName.removePrefix("Custom:").trim().split(",")
                if (coords.size == 2) {
                    val lat = coords[0].trim().toDouble()
                    val lng = coords[1].trim().toDouble()
                    return GeoPoint(lat, lng)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return CITIES[homeCityName] ?: CITIES["Milan"]!!
    }

    val initialMapCenter: StateFlow<GeoPoint?> = repository.getAllSpots()
        .combine(settingsRepository.homeCity) { allSpots, homeCityName ->
            if (allSpots.isNotEmpty()) {
                val groups = allSpots.groupBy { spotDetails ->
                    Pair(
                        Math.round(spotDetails.spot.latitude * 10) / 10.0,
                        Math.round(spotDetails.spot.longitude * 10) / 10.0
                    )
                }
                val mostPopulatedGroup = groups.maxByOrNull { it.value.size }
                if (mostPopulatedGroup != null) {
                    val avgLat = mostPopulatedGroup.value.map { it.spot.latitude }.average()
                    val avgLng = mostPopulatedGroup.value.map { it.spot.longitude }.average()
                    GeoPoint(avgLat, avgLng)
                } else {
                    resolveCityCoordinate(homeCityName)
                }
            } else {
                resolveCityCoordinate(homeCityName)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    companion object {
        val CITIES = mapOf(
            "Milan" to GeoPoint(45.4642, 9.1900),
            "London" to GeoPoint(51.5074, -0.1278),
            "New York" to GeoPoint(40.7128, -74.0060),
            "Paris" to GeoPoint(48.8566, 2.3522),
            "Tokyo" to GeoPoint(35.6762, 139.6503),
            "Berlin" to GeoPoint(52.5200, 13.4050),
            "Rome" to GeoPoint(41.9028, 12.4964),
            "San Francisco" to GeoPoint(37.7749, -122.4194),
            "Sydney" to GeoPoint(-33.8688, 151.2093)
        )

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TagSpotterApplication
                MapViewModel(app.repository, app.settingsRepository)
            }
        }
    }
}
