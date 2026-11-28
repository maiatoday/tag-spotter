package net.maiatoday.tagspotter.feature.map

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
import kotlinx.coroutines.launch
import net.maiatoday.tagspotter.core.settings.SettingsRepository
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.model.SpotOrigin
import net.maiatoday.tagspotter.core.database.SpotRepository
import net.maiatoday.tagspotter.core.model.FilterCenter
import net.maiatoday.tagspotter.core.model.LocationUtils
import net.maiatoday.tagspotter.core.settings.FilterManager
import net.maiatoday.tagspotter.core.sync.AuthService
import net.maiatoday.tagspotter.core.sync.SyncManager
import net.maiatoday.tagspotter.feature.gallery.EmojiSearchMap
import kotlin.math.roundToInt

class MapViewModel(
    private val repository: SpotRepository,
    private val filterManager: FilterManager,
    private val settingsRepository: SettingsRepository,
    private val authService: AuthService,
    private val syncManager: SyncManager
) : ViewModel() {

    val homeCity: StateFlow<String> = settingsRepository.homeCity
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Berlin"
        )

    val darkMapEnabled: StateFlow<Boolean> = settingsRepository.darkMapEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _selectedSpot = MutableStateFlow<SpotDetails?>(null)
    val selectedSpot: StateFlow<SpotDetails?> = _selectedSpot.asStateFlow()

    val selectedCategory: StateFlow<String> = filterManager.selectedCategory
    val activeFilterCenter: StateFlow<FilterCenter?> = filterManager.activeFilterCenter
    val activeRadiusMeters: StateFlow<Double> = filterManager.activeRadiusMeters
    val selectedSource: StateFlow<String> = filterManager.selectedSource
    val showStarredOnly: StateFlow<Boolean> = filterManager.showStarredOnly
    val searchQuery: StateFlow<String> = filterManager.searchQuery

    val showPublicSpots: StateFlow<Boolean> = settingsRepository.showPublicSpots
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val currentUserUid: StateFlow<String?> = authService.currentUserFlow
        .map { it?.uid }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        viewModelScope.launch {
            combine(settingsRepository.showPublicSpots, authService.currentUserFlow.map { it?.uid }) { showPublic, uid ->
                showPublic to uid
            }.collect { (showPublic, uid) ->
                if (!showPublic && _selectedSpot.value?.spot?.isOtherUsersPublic(uid) == true) {
                    _selectedSpot.value = null
                }
            }
        }
    }

    private data class MapFilterState(
        val category: String,
        val source: String,
        val query: String,
        val filterCenter: FilterCenter?,
        val radiusMeters: Double,
        val showStarredOnly: Boolean,
        val showPublicSpots: Boolean,
        val currentUserUid: String?
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val spots: StateFlow<List<SpotDetails>> = combine(
        combine(
            filterManager.selectedCategory,
            filterManager.selectedSource,
            filterManager.searchQuery,
            filterManager.activeFilterCenter,
            filterManager.activeRadiusMeters
        ) { category, source, query, filterCenter, radiusMeters ->
            MapFilterState(
                category = category,
                source = source,
                query = query,
                filterCenter = filterCenter,
                radiusMeters = radiusMeters,
                showStarredOnly = false,
                showPublicSpots = true,
                currentUserUid = null
            )
        },
        filterManager.showStarredOnly,
        settingsRepository.showPublicSpots,
        authService.currentUserFlow.map { it?.uid }
    ) { state, showStarredOnly, showPublicSpots, currentUserUid ->
        state.copy(
            showStarredOnly = showStarredOnly,
            showPublicSpots = showPublicSpots,
            currentUserUid = currentUserUid
        )
    }.flatMapLatest { state ->
        repository.getSpotsByCategory(state.category).map { list ->
            val visibilityFiltered = if (state.showPublicSpots) {
                list
            } else {
                list.filter { !it.spot.isOtherUsersPublic(state.currentUserUid) }
            }
            val sourceFiltered = when (state.source) {
                "My Spots" -> visibilityFiltered.filter { it.spot.isEditableBy(state.currentUserUid) }
                "Public" -> visibilityFiltered.filter { it.spot.isOtherUsersPublic(state.currentUserUid) }
                "Imported" -> visibilityFiltered.filter { it.spot.effectiveOrigin == SpotOrigin.IMPORTED }
                else -> visibilityFiltered
            }
            val starredFiltered = if (state.showStarredOnly) {
                sourceFiltered.filter { it.spot.isStarred }
            } else {
                sourceFiltered
            }
            val queryFiltered = if (state.query.isBlank()) {
                starredFiltered
            } else {
                val q = state.query.trim().lowercase()
                val emojiKeywords = EmojiSearchMap.getKeywordsForEmoji(q)
                starredFiltered.filter { detail ->
                    val spot = detail.spot
                    val matchTags = spot.tags.any { tag ->
                        tag.lowercase().contains(q) || emojiKeywords.any { tag.lowercase().contains(it) }
                    }
                    val matchCategory = spot.category.lowercase().contains(q) || emojiKeywords.any { spot.category.lowercase().contains(it) }
                    val matchArtists = spot.artists.any { artist ->
                        artist.lowercase().contains(q) || emojiKeywords.any { artist.lowercase().contains(it) }
                    }
                    val matchPhotographer = spot.photographer.lowercase().contains(q) || emojiKeywords.any { spot.photographer.lowercase().contains(it) }
                    matchTags || matchCategory || matchArtists || matchPhotographer
                }
            }
            val center = state.filterCenter
            if (center != null) {
                queryFiltered.filter { detail ->
                    val distance = LocationUtils.calculateDistance(
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

    fun selectCategory(category: String) {
        filterManager.selectCategory(category)
    }

    fun selectSpot(spot: SpotDetails?) {
        _selectedSpot.value = spot
    }

    fun selectSource(source: String) {
        filterManager.selectSource(source)
    }

    fun setSearchQuery(query: String) {
        filterManager.setSearchQuery(query)
    }

    fun setShowStarredOnly(show: Boolean) {
        filterManager.setShowStarredOnly(show)
    }

    fun toggleShowStarredOnly() {
        filterManager.toggleShowStarredOnly()
    }

    fun setShowPublicSpots(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateShowPublicSpots(show)
            if (show) {
                try {
                    syncManager.syncPublicSpots()
                } catch (_: Exception) {
                    // Ignore sync errors on toggle
                }
            } else if (filterManager.selectedSource.value == "Public") {
                filterManager.selectSource("All")
            }
        }
    }

    fun setLocationFilter(center: FilterCenter?, radiusMeters: Double) {
        filterManager.setLocationFilter(center, radiusMeters)
    }

    fun clearLocationFilter() {
        filterManager.clearLocationFilter()
    }

    private fun resolveCityCoordinate(homeCityName: String): MapPoint {
        if (homeCityName.startsWith("Custom:")) {
            try {
                val coords = homeCityName.removePrefix("Custom:").trim().split(",")
                if (coords.size == 2) {
                    val lat = coords[0].trim().toDouble()
                    val lng = coords[1].trim().toDouble()
                    return MapPoint(lat, lng)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return CITIES[homeCityName] ?: CITIES.getValue(LocationUtils.DEFAULT_CITY)
    }

    val initialMapCenter: StateFlow<MapPoint?> = combine(
        repository.getAllSpots(),
        settingsRepository.homeCity,
        settingsRepository.showPublicSpots,
        authService.currentUserFlow.map { it?.uid }
    ) { allSpots, homeCityName, showPublic, uid ->
        val visibleSpots = if (showPublic) {
            allSpots
        } else {
            allSpots.filterNot { it.spot.isOtherUsersPublic(uid) }
        }
        if (visibleSpots.isNotEmpty()) {
            val groups = visibleSpots.groupBy { spotDetails ->
                Pair(
                    (spotDetails.spot.latitude * 10).roundToInt() / 10.0,
                    (spotDetails.spot.longitude * 10).roundToInt() / 10.0
                )
            }
            val mostPopulatedGroup = groups.maxByOrNull { it.value.size }
            if (mostPopulatedGroup != null) {
                val avgLat = mostPopulatedGroup.value.map { it.spot.latitude }.average()
                val avgLng = mostPopulatedGroup.value.map { it.spot.longitude }.average()
                MapPoint(avgLat, avgLng)
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
            MapPoint(coords.first, coords.second)
        }
    }
}
