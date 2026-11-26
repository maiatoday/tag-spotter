package net.maiatoday.tagspotter.core.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

interface SettingsRepository {
    val themeMode: StateFlow<ThemeMode>
    fun setThemeMode(mode: ThemeMode)

    val photographerName: Flow<String>
    suspend fun updatePhotographerName(name: String)
    val homeCity: Flow<String>
    suspend fun updateHomeCity(city: String)
    val showTestData: Flow<Boolean>
    suspend fun updateShowTestData(show: Boolean)
    val showPublicSpots: Flow<Boolean>
    suspend fun updateShowPublicSpots(show: Boolean)

    val darkMapEnabled: Flow<Boolean>
    suspend fun updateDarkMapEnabled(enabled: Boolean)
    val artistRecognitionEnabled: Flow<Boolean>
    suspend fun updateArtistRecognitionEnabled(enabled: Boolean)
    val geminiApiKey: Flow<String>
    suspend fun updateGeminiApiKey(key: String)
}
