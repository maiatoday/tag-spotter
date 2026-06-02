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
import kotlinx.coroutines.launch

class GalleryViewModel(private val repository: SpotRepository) : ViewModel() {

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedSource = MutableStateFlow("All")
    val selectedSource: StateFlow<String> = _selectedSource.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val spots: StateFlow<List<SpotDetails>> = combine(
        _selectedCategory,
        _selectedSource,
        _searchQuery
    ) { category, source, query ->
        Triple(category, source, query)
    }.flatMapLatest { (category, source, query) ->
        repository.getSpotsByCategory(category).map { list ->
            // First apply source filtering
            val sourceFiltered = when (source) {
                "My Spots" -> list.filter { !it.spot.isImported }
                "Imported" -> list.filter { it.spot.isImported }
                else -> list
            }
            // Then apply search query filtering
            if (query.isBlank()) {
                sourceFiltered
            } else {
                val q = query.trim().lowercase()
                sourceFiltered.filter { detail ->
                    val spot = detail.spot
                    val matchTags = spot.tags.any { it.lowercase().contains(q) }
                    val matchArtists = spot.artists.any { it.lowercase().contains(q) }
                    val matchPhotographer = spot.photographer.lowercase().contains(q)
                    matchTags || matchArtists || matchPhotographer
                }
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

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TagSpotterApplication
                GalleryViewModel(app.repository)
            }
        }
    }
}
