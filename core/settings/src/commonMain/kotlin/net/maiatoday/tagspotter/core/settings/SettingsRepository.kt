package net.maiatoday.tagspotter.core.settings

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val photographerName: Flow<String>
    suspend fun updatePhotographerName(name: String)
    val homeCity: Flow<String>
    suspend fun updateHomeCity(city: String)
    val showTestData: Flow<Boolean>
    suspend fun updateShowTestData(show: Boolean)

    val darkMapEnabled: Flow<Boolean>
    suspend fun updateDarkMapEnabled(enabled: Boolean)
    val artistRecognitionEnabled: Flow<Boolean>
    suspend fun updateArtistRecognitionEnabled(enabled: Boolean)
    val geminiApiKey: Flow<String>
    suspend fun updateGeminiApiKey(key: String)
}
