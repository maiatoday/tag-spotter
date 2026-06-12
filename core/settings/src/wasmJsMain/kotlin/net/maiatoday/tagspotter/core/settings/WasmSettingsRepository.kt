package net.maiatoday.tagspotter.core.settings

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class WasmSettingsRepository(
    private val secureStorage: SecureStorage,
    private val settings: Settings
) : SettingsRepository {
    private val _photographerName = MutableStateFlow(settings.getString("photographer_name", ""))
    override val photographerName: Flow<String> = _photographerName.asStateFlow()

    private val _homeCity = MutableStateFlow(settings.getString("home_city", "Milan"))
    override val homeCity: Flow<String> = _homeCity.asStateFlow()

    private val _showTestData = MutableStateFlow(settings.getBoolean("show_test_data", false))
    override val showTestData: Flow<Boolean> = _showTestData.asStateFlow()

    private val _darkMapEnabled = MutableStateFlow(settings.getBoolean("dark_map_enabled", false))
    override val darkMapEnabled: Flow<Boolean> = _darkMapEnabled.asStateFlow()

    private val _artistRecognitionEnabled = MutableStateFlow(settings.getBoolean("artist_recognition_enabled", true))
    override val artistRecognitionEnabled: Flow<Boolean> = _artistRecognitionEnabled.asStateFlow()

    private val _geminiApiKey = MutableStateFlow(secureStorage.getString("gemini_api_key", ""))
    override val geminiApiKey: Flow<String> = _geminiApiKey.asStateFlow()

    override suspend fun updatePhotographerName(name: String) {
        settings.putString("photographer_name", name)
        _photographerName.value = name
    }

    override suspend fun updateHomeCity(city: String) {
        settings.putString("home_city", city)
        _homeCity.value = city
    }

    override suspend fun updateShowTestData(show: Boolean) {
        settings.putBoolean("show_test_data", show)
        _showTestData.value = show
    }

    override suspend fun updateDarkMapEnabled(enabled: Boolean) {
        settings.putBoolean("dark_map_enabled", enabled)
        _darkMapEnabled.value = enabled
    }

    override suspend fun updateArtistRecognitionEnabled(enabled: Boolean) {
        settings.putBoolean("artist_recognition_enabled", enabled)
        _artistRecognitionEnabled.value = enabled
    }

    override suspend fun updateGeminiApiKey(key: String) {
        secureStorage.putString("gemini_api_key", key)
        _geminiApiKey.value = key
    }
}
