package net.maiatoday.tagspotter.core.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSettingsRepositoryTest {

    private val fakeSecureStorage = FakeSecureStorage()

    private fun createRepository(): DataStoreSettingsRepository {
        return DataStoreSettingsRepository(fakeSecureStorage, createTestDatastore())
    }

    @Test
    fun defaultValuesAreReturnedCorrectly() = runTest {
        val repository = createRepository()

        assertEquals("", repository.photographerName.first())
        assertEquals("Milan", repository.homeCity.first())
        assertFalse(repository.showTestData.first())

        assertFalse(repository.darkMapEnabled.first())
        assertTrue(repository.artistRecognitionEnabled.first())
    }

    @Test
    fun updatePreferencesWorksCorrectly() = runTest {
        val repository = createRepository()

        repository.updatePhotographerName("Alice")
        assertEquals("Alice", repository.photographerName.first())

        repository.updateHomeCity("Rome")
        assertEquals("Rome", repository.homeCity.first())

        repository.updateShowTestData(true)
        assertTrue(repository.showTestData.first())

        repository.updateDarkMapEnabled(true)
        assertTrue(repository.darkMapEnabled.first())

        repository.updateArtistRecognitionEnabled(false)
        assertFalse(repository.artistRecognitionEnabled.first())
    }

    @Test
    fun geminiApiKeyUsesSecurePreferences() = runTest {
        fakeSecureStorage.putString("gemini_api_key", "secure_key_123")

        val repository = createRepository()

        assertEquals("secure_key_123", repository.geminiApiKey.first())

        repository.updateGeminiApiKey("new_secure_key")
        assertEquals("new_secure_key", repository.geminiApiKey.first())
        assertEquals("new_secure_key", fakeSecureStorage.getString("gemini_api_key", ""))
    }

    class FakeSecureStorage : SecureStorage {
        val map = mutableMapOf<String, String>()
        override fun getString(key: String, defaultValue: String): String = map[key] ?: defaultValue
        override fun putString(key: String, value: String) {
            map[key] = value
        }
    }
}

expect fun createTestDatastore(): DataStore<Preferences>
