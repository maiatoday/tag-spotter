package net.maiatoday.tagspotter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import net.maiatoday.tagspotter.TagSpotterApplication
import net.maiatoday.tagspotter.data.SpotDetails
import net.maiatoday.tagspotter.data.SpotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class GalleryViewModel(
    private val repository: SpotRepository,
    private val settingsRepository: net.maiatoday.tagspotter.data.SettingsRepository
) : ViewModel() {

    val homeCity: StateFlow<String> = settingsRepository.homeCity
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Milan"
        )

    sealed interface UiEvent {
        data object StarLimitExceeded : UiEvent
    }

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedSource = MutableStateFlow("All")
    val selectedSource: StateFlow<String> = _selectedSource.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeFilterCenter = MutableStateFlow<net.maiatoday.tagspotter.utils.FilterCenter?>(null)
    val activeFilterCenter: StateFlow<net.maiatoday.tagspotter.utils.FilterCenter?> = _activeFilterCenter.asStateFlow()

    private val _activeRadiusMeters = MutableStateFlow(5000.0) // default 5km
    val activeRadiusMeters: StateFlow<Double> = _activeRadiusMeters.asStateFlow()

    private data class FilterState(
        val category: String,
        val source: String,
        val query: String,
        val filterCenter: net.maiatoday.tagspotter.utils.FilterCenter?,
        val radiusMeters: Double
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val spots: StateFlow<List<SpotDetails>> = combine(
        _selectedCategory,
        _selectedSource,
        _searchQuery,
        _activeFilterCenter,
        _activeRadiusMeters
    ) { category, source, query, filterCenter, radiusMeters ->
        FilterState(category, source, query, filterCenter, radiusMeters)
    }.flatMapLatest { state ->
        repository.getSpotsByCategory(state.category).map { list ->
            // First apply source filtering
            val sourceFiltered = when (state.source) {
                "My Spots" -> list.filter { !it.spot.isImported }
                "Imported" -> list.filter { it.spot.isImported }
                else -> list
            }
            // Then apply search query filtering
            val queryFiltered = if (state.query.isBlank()) {
                sourceFiltered
            } else {
                val q = state.query.trim().lowercase()
                sourceFiltered.filter { detail ->
                    val spot = detail.spot
                    val matchTags = spot.tags.any { it.lowercase().contains(q) }
                    val matchArtists = spot.artists.any { it.lowercase().contains(q) }
                    val matchPhotographer = spot.photographer.lowercase().contains(q)
                    matchTags || matchArtists || matchPhotographer
                }
            }
            // Then apply location & radius filtering if active
            val center = state.filterCenter
            if (center != null) {
                queryFiltered.filter { detail ->
                    val distance = net.maiatoday.tagspotter.utils.LocationUtils.calculateDistance(
                        center.latitude,
                        center.longitude,
                        detail.spot.latitude,
                        detail.spot.longitude
                    )
                    distance <= state.radiusMeters
                }
            } else {
                queryFiltered
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setLocationFilter(center: net.maiatoday.tagspotter.utils.FilterCenter?, radiusMeters: Double) {
        _activeFilterCenter.value = center
        _activeRadiusMeters.value = radiusMeters
    }

    fun clearLocationFilter() {
        _activeFilterCenter.value = null
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun selectSource(source: String) {
        _selectedSource.value = source
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteSpots(ids: List<Long>, onCompleted: () -> Unit) {
        viewModelScope.launch {
            val allSpots = spots.value
            ids.forEach { id ->
                allSpots.find { it.spot.id == id }?.let { detail ->
                    repository.deleteSpot(detail)
                }
            }
            onCompleted()
        }
    }

    fun bulkUpdateStarred(ids: List<Long>, isStarred: Boolean, onCompleted: () -> Unit) {
        viewModelScope.launch {
            if (isStarred) {
                val currentStarredCount = repository.getStarredSpotsCount()
                val spotsList = spots.value
                val newStarsCount = ids.count { id ->
                    val spotDetails = spotsList.find { it.spot.id == id }
                    spotDetails != null && !spotDetails.spot.isStarred
                }
                if (currentStarredCount + newStarsCount > 100) {
                    _uiEvent.emit(UiEvent.StarLimitExceeded)
                    return@launch
                }
            }
            ids.forEach { id ->
                repository.updateSpotStarred(id, isStarred)
            }
            onCompleted()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TagSpotterApplication
                GalleryViewModel(app.repository, app.settingsRepository)
            }
        }
    }
}
