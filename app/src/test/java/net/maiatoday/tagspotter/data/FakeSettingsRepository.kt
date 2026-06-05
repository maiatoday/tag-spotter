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

    private val _notificationsEnabled = MutableStateFlow(false)
    override val notificationsEnabled: Flow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _darkMapEnabled = MutableStateFlow(false)
    override val darkMapEnabled: Flow<Boolean> = _darkMapEnabled.asStateFlow()

    private val _artistRecognitionEnabled = MutableStateFlow(true)
    override val artistRecognitionEnabled: Flow<Boolean> = _artistRecognitionEnabled.asStateFlow()

    private val _geminiApiKey = MutableStateFlow("")
    override val geminiApiKey: Flow<String> = _geminiApiKey.asStateFlow()

    override suspend fun updatePhotographerName(name: String) {
        _photographerName.value = name
    }

    override suspend fun updateHomeCity(city: String) {
        _homeCity.value = city
    }

    override suspend fun updateShowTestData(show: Boolean) {
        _showTestData.value = show
    }

    override suspend fun updateNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }

    override suspend fun updateDarkMapEnabled(enabled: Boolean) {
        _darkMapEnabled.value = enabled
    }

    override suspend fun updateArtistRecognitionEnabled(enabled: Boolean) {
        _artistRecognitionEnabled.value = enabled
    }

    override suspend fun updateGeminiApiKey(key: String) {
        _geminiApiKey.value = key
    }
}
