package net.maiatoday.tagspotter.data

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface SettingsRepository {
    val photographerName: Flow<String>
    suspend fun updatePhotographerName(name: String)
    val homeCity: Flow<String>
    suspend fun updateHomeCity(city: String)
    val showTestData: Flow<Boolean>
    suspend fun updateShowTestData(show: Boolean)
    val notificationsEnabled: Flow<Boolean>
    suspend fun updateNotificationsEnabled(enabled: Boolean)
    val darkMapEnabled: Flow<Boolean>
    suspend fun updateDarkMapEnabled(enabled: Boolean)
    val artistRecognitionEnabled: Flow<Boolean>
    suspend fun updateArtistRecognitionEnabled(enabled: Boolean)
    val geminiApiKey: Flow<String>
    suspend fun updateGeminiApiKey(key: String)
}

class SharedPreferencesSettingsRepository(context: Context) : SettingsRepository {
    private val sharedPreferences = context.getSharedPreferences("tag_spotter_settings", Context.MODE_PRIVATE)
    
    private val securePreferences = try {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "secure_tag_spotter_settings",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        e.printStackTrace()
        sharedPreferences
    }

    private val _photographerName = MutableStateFlow(getSavedName())
    override val photographerName: Flow<String> = _photographerName.asStateFlow()

    private val _homeCity = MutableStateFlow(getSavedHomeCity())
    override val homeCity: Flow<String> = _homeCity.asStateFlow()

    private val _showTestData = MutableStateFlow(getSavedShowTestData())
    override val showTestData: Flow<Boolean> = _showTestData.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(getSavedNotificationsEnabled())
    override val notificationsEnabled: Flow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _darkMapEnabled = MutableStateFlow(getSavedDarkMapEnabled())
    override val darkMapEnabled: Flow<Boolean> = _darkMapEnabled.asStateFlow()

    private val _artistRecognitionEnabled = MutableStateFlow(getSavedArtistRecognitionEnabled())
    override val artistRecognitionEnabled: Flow<Boolean> = _artistRecognitionEnabled.asStateFlow()

    private val _geminiApiKey = MutableStateFlow(getSavedGeminiApiKey())
    override val geminiApiKey: Flow<String> = _geminiApiKey.asStateFlow()

    private fun getSavedName(): String {
        return sharedPreferences.getString("photographer_name", "") ?: ""
    }

    private fun getSavedHomeCity(): String {
        return sharedPreferences.getString("home_city", "Milan") ?: "Milan"
    }

    private fun getSavedShowTestData(): Boolean {
        return sharedPreferences.getBoolean("show_test_data", false)
    }

    private fun getSavedNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean("notifications_enabled", false)
    }

    private fun getSavedDarkMapEnabled(): Boolean {
        return sharedPreferences.getBoolean("dark_map_enabled", false)
    }

    private fun getSavedArtistRecognitionEnabled(): Boolean {
        return sharedPreferences.getBoolean("artist_recognition_enabled", true)
    }

    private fun getSavedGeminiApiKey(): String {
        return securePreferences.getString("gemini_api_key", "") ?: ""
    }

    override suspend fun updatePhotographerName(name: String) {
        sharedPreferences.edit { putString("photographer_name", name) }
        _photographerName.value = name
    }

    override suspend fun updateHomeCity(city: String) {
        sharedPreferences.edit { putString("home_city", city) }
        _homeCity.value = city
    }

    override suspend fun updateShowTestData(show: Boolean) {
        sharedPreferences.edit { putBoolean("show_test_data", show) }
        _showTestData.value = show
    }

    override suspend fun updateNotificationsEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean("notifications_enabled", enabled) }
        _notificationsEnabled.value = enabled
    }

    override suspend fun updateDarkMapEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean("dark_map_enabled", enabled) }
        _darkMapEnabled.value = enabled
    }

    override suspend fun updateArtistRecognitionEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean("artist_recognition_enabled", enabled) }
        _artistRecognitionEnabled.value = enabled
    }

    override suspend fun updateGeminiApiKey(key: String) {
        securePreferences.edit { putString("gemini_api_key", key) }
        _geminiApiKey.value = key
    }
}
