package net.maiatoday.tagspotter.core.settings

import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSettingsRepositoryTest {

    @TempDir
    lateinit var tempFolder: File

    private val fakeContext = FakeContext()
    private val fakeSecureStorage = FakeSecureStorage()

    private fun createRepository(): DataStoreSettingsRepository {
        val testDataStore = PreferenceDataStoreFactory.create(
            produceFile = { File(tempFolder, "test_settings.preferences_pb") }
        )
        return DataStoreSettingsRepository(fakeContext, fakeSecureStorage, testDataStore)
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
        // Set up secure api key in preferences prior to repo instantiation
        fakeSecureStorage.putString("gemini_api_key", "secure_key_123")

        val repository = createRepository()

        // Verify it reads the key correctly
        assertEquals("secure_key_123", repository.geminiApiKey.first())

        // Verify updating it changes the flow and writes to secure preferences
        repository.updateGeminiApiKey("new_secure_key")
        assertEquals("new_secure_key", repository.geminiApiKey.first())
        assertEquals("new_secure_key", fakeSecureStorage.getString("gemini_api_key", ""))
    }

    class FakeContext : ContextWrapper(null) {
        private val preferencesMap = mutableMapOf<String, FakeSharedPreferences>()

        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
            return preferencesMap.getOrPut(name) { FakeSharedPreferences() }
        }
    }

    class FakeSharedPreferences : SharedPreferences {
        val map = mutableMapOf<String, Any?>()

        override fun getAll(): Map<String, *> = map

        override fun getString(key: String, defValue: String?): String? {
            return map[key] as? String ?: defValue
        }

        override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? {
            return (map[key] as? Set<*>)?.filterIsInstance<String>()?.toSet() ?: defValues
        }

        override fun getInt(key: String, defValue: Int): Int {
            return map[key] as? Int ?: defValue
        }

        override fun getLong(key: String, defValue: Long): Long {
            return map[key] as? Long ?: defValue
        }

        override fun getFloat(key: String, defValue: Float): Float {
            return map[key] as? Float ?: defValue
        }

        override fun getBoolean(key: String, defValue: Boolean): Boolean {
            return map[key] as? Boolean ?: defValue
        }

        override fun contains(key: String): Boolean = map.containsKey(key)

        override fun edit(): SharedPreferences.Editor = FakeEditor(this)

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        class FakeEditor(private val parent: FakeSharedPreferences) : SharedPreferences.Editor {
            private val tempMap = mutableMapOf<String, Any?>()

            override fun putString(key: String, value: String?): SharedPreferences.Editor {
                tempMap[key] = value
                return this
            }

            override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor {
                tempMap[key] = values
                return this
            }

            override fun putInt(key: String, value: Int): SharedPreferences.Editor {
                tempMap[key] = value
                return this
            }

            override fun putLong(key: String, value: Long): SharedPreferences.Editor {
                tempMap[key] = value
                return this
            }

            override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
                tempMap[key] = value
                return this
            }

            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
                tempMap[key] = value
                return this
            }

            override fun remove(key: String): SharedPreferences.Editor {
                tempMap.remove(key)
                return this
            }

            override fun clear(): SharedPreferences.Editor {
                tempMap.clear()
                return this
            }

            override fun commit(): Boolean {
                parent.map.putAll(tempMap)
                return true
            }

            override fun apply() {
                parent.map.putAll(tempMap)
            }
        }
    }

    class FakeSecureStorage : SecureStorage {
        val map = mutableMapOf<String, String>()
        override fun getString(key: String, defaultValue: String): String = map[key] ?: defaultValue
        override fun putString(key: String, value: String) {
            map[key] = value
        }
    }
}