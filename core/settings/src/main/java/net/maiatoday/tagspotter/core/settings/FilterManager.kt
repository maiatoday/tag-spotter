package net.maiatoday.tagspotter.core.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.maiatoday.tagspotter.core.model.FilterCenter

class FilterManager {
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedSource = MutableStateFlow("All")
    val selectedSource = _selectedSource.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _showStarredOnly = MutableStateFlow(false)
    val showStarredOnly = _showStarredOnly.asStateFlow()

    private val _activeFilterCenter = MutableStateFlow<FilterCenter?>(null)
    val activeFilterCenter = _activeFilterCenter.asStateFlow()

    private val _activeRadiusMeters = MutableStateFlow(5000.0)
    val activeRadiusMeters = _activeRadiusMeters.asStateFlow()

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun selectSource(source: String) {
        _selectedSource.value = source
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setShowStarredOnly(show: Boolean) {
        _showStarredOnly.value = show
    }

    fun toggleShowStarredOnly() {
        _showStarredOnly.value = !_showStarredOnly.value
    }

    fun setLocationFilter(center: FilterCenter?, radiusMeters: Double) {
        _activeFilterCenter.value = center
        _activeRadiusMeters.value = radiusMeters
    }

    fun clearLocationFilter() {
        _activeFilterCenter.value = null
    }

    fun clearAll() {
        _selectedCategory.value = "All"
        _selectedSource.value = "All"
        _searchQuery.value = ""
        _showStarredOnly.value = false
        _activeFilterCenter.value = null
    }
}
