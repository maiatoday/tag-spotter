package net.maiatoday.tagspotter.feature.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.maiatoday.tagspotter.core.database.SpotRepository
import net.maiatoday.tagspotter.core.model.FilterCenter
import net.maiatoday.tagspotter.core.model.LocationUtils
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.settings.FilterManager
import net.maiatoday.tagspotter.core.settings.SettingsRepository

class GalleryViewModel(
    private val repository: SpotRepository,
    private val filterManager: FilterManager,
    settingsRepository: SettingsRepository
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

    val selectedCategory: StateFlow<String> = filterManager.selectedCategory
    val selectedSource: StateFlow<String> = filterManager.selectedSource
    val searchQuery: StateFlow<String> = filterManager.searchQuery
    val showStarredOnly: StateFlow<Boolean> = filterManager.showStarredOnly
    val activeFilterCenter: StateFlow<FilterCenter?> = filterManager.activeFilterCenter
    val activeRadiusMeters: StateFlow<Double> = filterManager.activeRadiusMeters

    private data class FilterState(
        val category: String,
        val source: String,
        val query: String,
        val filterCenter: FilterCenter?,
        val radiusMeters: Double,
        val showStarredOnly: Boolean
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
            FilterState(category, source, query, filterCenter, radiusMeters, false)
        },
        filterManager.showStarredOnly
    ) { state, showStarredOnly ->
        state.copy(showStarredOnly = showStarredOnly)
    }.flatMapLatest { state ->
        repository.getSpotsByCategory(state.category).map { list ->
            // First apply source filtering
            val sourceFiltered = when (state.source) {
                "My Spots" -> list.filter { !it.spot.isImported }
                "Imported" -> list.filter { it.spot.isImported }
                else -> list
            }
            // Apply starred filtering
            val starredFiltered = if (state.showStarredOnly) {
                sourceFiltered.filter { it.spot.isStarred }
            } else {
                sourceFiltered
            }
            // Then apply search query filtering
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
            // Then apply location & radius filtering if active
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

    fun toggleShowStarredOnly() {
        filterManager.toggleShowStarredOnly()
    }

    fun setShowStarredOnly(show: Boolean) {
        filterManager.setShowStarredOnly(show)
    }

    fun setLocationFilter(center: FilterCenter?, radiusMeters: Double) {
        filterManager.setLocationFilter(center, radiusMeters)
    }

    fun clearLocationFilter() {
        filterManager.clearLocationFilter()
    }

    fun selectCategory(category: String) {
        filterManager.selectCategory(category)
    }

    fun selectSource(source: String) {
        filterManager.selectSource(source)
    }

    fun setSearchQuery(query: String) {
        filterManager.setSearchQuery(query)
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
}
