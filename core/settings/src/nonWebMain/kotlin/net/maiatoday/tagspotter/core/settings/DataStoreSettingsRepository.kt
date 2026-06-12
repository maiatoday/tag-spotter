package net.maiatoday.tagspotter.core.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import androidx.datastore.core.IOException

class DataStoreSettingsRepository(
    private val secureStorage: SecureStorage,
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {
    
    private val _geminiApiKey = MutableStateFlow(getSavedGeminiApiKey())
    override val geminiApiKey: Flow<String> = _geminiApiKey.asStateFlow()

    private fun getSavedGeminiApiKey(): String {
        return secureStorage.getString("gemini_api_key", "")
    }

    override suspend fun updateGeminiApiKey(key: String) {
        secureStorage.putString("gemini_api_key", key)
        _geminiApiKey.value = key
    }

    companion object {
        val PHOTOGRAPHER_NAME = stringPreferencesKey("photographer_name")
        val HOME_CITY = stringPreferencesKey("home_city")
        val SHOW_TEST_DATA = booleanPreferencesKey("show_test_data")

        val DARK_MAP_ENABLED = booleanPreferencesKey("dark_map_enabled")
        val ARTIST_RECOGNITION_ENABLED = booleanPreferencesKey("artist_recognition_enabled")
    }

    override val photographerName: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PHOTOGRAPHER_NAME] ?: ""
        }

    override val homeCity: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[HOME_CITY] ?: "Milan"
        }

    override val showTestData: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[SHOW_TEST_DATA] ?: false
        }

    override val darkMapEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[DARK_MAP_ENABLED] ?: false
        }

    override val artistRecognitionEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[ARTIST_RECOGNITION_ENABLED] ?: true
        }

    override suspend fun updatePhotographerName(name: String) {
        dataStore.edit { preferences ->
            preferences[PHOTOGRAPHER_NAME] = name
        }
    }

    override suspend fun updateHomeCity(city: String) {
        dataStore.edit { preferences ->
            preferences[HOME_CITY] = city
        }
    }

    override suspend fun updateShowTestData(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_TEST_DATA] = show
        }
    }

    override suspend fun updateDarkMapEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DARK_MAP_ENABLED] = enabled
        }
    }

    override suspend fun updateArtistRecognitionEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[ARTIST_RECOGNITION_ENABLED] = enabled
        }
    }
}
