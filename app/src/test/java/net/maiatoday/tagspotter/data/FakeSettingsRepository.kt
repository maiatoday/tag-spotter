package net.maiatoday.tagspotter.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSettingsRepository(initialName: String = "", initialHomeCity: String = "Milan") : SettingsRepository {
    private val _photographerName = MutableStateFlow(initialName)
    override val photographerName: Flow<String> = _photographerName.asStateFlow()

    private val _homeCity = MutableStateFlow(initialHomeCity)
    override val homeCity: Flow<String> = _homeCity.asStateFlow()

    private val _showTestData = MutableStateFlow(false)
    override val showTestData: Flow<Boolean> = _showTestData.asStateFlow()

    override suspend fun updatePhotographerName(name: String) {
        _photographerName.value = name
    }

    override suspend fun updateHomeCity(city: String) {
        _homeCity.value = city
    }

    override suspend fun updateShowTestData(show: Boolean) {
        _showTestData.value = show
    }
}
