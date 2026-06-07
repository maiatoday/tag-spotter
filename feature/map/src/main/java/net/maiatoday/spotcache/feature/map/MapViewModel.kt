package net.maiatoday.spotcache.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.maiatoday.spotcache.core.settings.SettingsRepository
import net.maiatoday.spotcache.core.model.SpotDetails
import net.maiatoday.spotcache.core.database.SpotRepository
import net.maiatoday.spotcache.core.model.FilterCenter
import net.maiatoday.spotcache.core.model.LocationUtils
import org.osmdroid.util.GeoPoint
import kotlin.math.roundToInt

class MapViewModel(
    private val repository: SpotRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val homeCity: StateFlow<String> = settingsRepository.homeCity
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Milan"
        )

    val darkMapEnabled: StateFlow<Boolean> = settingsRepository.darkMapEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedSpot = MutableStateFlow<SpotDetails?>(null)
    val selectedSpot: StateFlow<SpotDetails?> = _selectedSpot.asStateFlow()

    private val _activeFilterCenter = MutableStateFlow<FilterCenter?>(null)
    val activeFilterCenter: StateFlow<FilterCenter?> = _activeFilterCenter.asStateFlow()

    private val _activeRadiusMeters = MutableStateFlow(5000.0) // default 5km
    val activeRadiusMeters: StateFlow<Double> = _activeRadiusMeters.asStateFlow()

    private data class MapFilterState(
        val category: String,
        val filterCenter: FilterCenter?,
        val radiusMeters: Double
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val spots: StateFlow<List<SpotDetails>> = combine(
        _selectedCategory,
        _activeFilterCenter,
        _activeRadiusMeters
    ) { category, filterCenter, radiusMeters ->
        MapFilterState(category, filterCenter, radiusMeters)
    }.flatMapLatest { state ->
        repository.getSpotsByCategory(state.category).map { list ->
            val center = state.filterCenter
            if (center != null) {
                list.filter { detail ->
                    val distance = LocationUtils.calculateDistance(
                        center.latitude,
                        center.longitude,
                        detail.spot.latitude,
                        detail.spot.longitude
                    )
                    distance <= state.radiusMeters
                }
            } else {
                list
            }
        }
    }.stateIn(
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

    fun setLocationFilter(center: FilterCenter?, radiusMeters: Double) {
        _activeFilterCenter.value = center
        _activeRadiusMeters.value = radiusMeters
    }

    fun clearLocationFilter() {
        _activeFilterCenter.value = null
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
                        (spotDetails.spot.latitude * 10).roundToInt() / 10.0,
                        (spotDetails.spot.longitude * 10).roundToInt() / 10.0
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
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    companion object {
        val CITIES = LocationUtils.CITIES.mapValues { (_, coords) ->
            GeoPoint(coords.first, coords.second)
        }
    }
}
