package net.maiatoday.tagspotter.core.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.datastore.core.IOException

class DataStoreSettingsRepository(
    private val secureStorage: SecureStorage,
    private val dataStore: DataStore<Preferences>,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : SettingsRepository {

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    init {
        scope.launch {
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }
                .collect { preferences ->
                    val modeStr = preferences[THEME_MODE]
                    if (modeStr != null) {
                        runCatching { ThemeMode.valueOf(modeStr) }.getOrNull()?.let { savedMode ->
                            _themeMode.value = savedMode
                        }
                    }
                }
        }
    }

    override fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        scope.launch {
            dataStore.edit { preferences ->
                preferences[THEME_MODE] = mode.name
            }
        }
    }
    
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
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PHOTOGRAPHER_NAME = stringPreferencesKey("photographer_name")
        val HOME_CITY = stringPreferencesKey("home_city")
        val SHOW_TEST_DATA = booleanPreferencesKey("show_test_data")
        val SHOW_PUBLIC_SPOTS = booleanPreferencesKey("show_public_spots")

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
            preferences[HOME_CITY] ?: "Berlin"
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

    override val showPublicSpots: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[SHOW_PUBLIC_SPOTS] ?: true
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

    override suspend fun updateShowPublicSpots(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_PUBLIC_SPOTS] = show
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
