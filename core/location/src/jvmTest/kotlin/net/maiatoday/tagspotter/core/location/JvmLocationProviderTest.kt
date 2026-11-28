package net.maiatoday.tagspotter.core.location

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import net.maiatoday.tagspotter.core.model.LocationUtils
import net.maiatoday.tagspotter.core.settings.FakeSettingsRepository
import net.maiatoday.tagspotter.core.settings.SettingsRepository
import net.maiatoday.tagspotter.core.settings.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class JvmLocationProviderTest {

    @Test
    fun defaultsToBerlinWhenSettingsRepositoryIsNull() = runTest {
        val provider = JvmLocationProvider(settingsRepository = null)
        val location = provider.getCurrentLocation()
        val expected = LocationUtils.CITIES.getValue("Berlin")

        assertEquals(expected.first, location.latitude)
        assertEquals(expected.second, location.longitude)
        assertTrue(location.isFallback)
    }

    @Test
    fun usesConfiguredHomeCityFromSettings() = runTest {
        val settings = FakeSettingsRepository(initialHomeCity = "Berlin")
        val provider = JvmLocationProvider(settingsRepository = settings)
        val location = provider.getCurrentLocation()
        val expected = LocationUtils.CITIES.getValue("Berlin")

        assertEquals(expected.first, location.latitude)
        assertEquals(expected.second, location.longitude)
        assertTrue(location.isFallback)
    }

    @Test
    fun usesCustomHomeCityCoordinatesFromSettings() = runTest {
        val settings = FakeSettingsRepository(initialHomeCity = "Custom: 48.1351, 11.5820")
        val provider = JvmLocationProvider(settingsRepository = settings)
        val location = provider.getCurrentLocation()

        assertEquals(48.1351, location.latitude)
        assertEquals(11.5820, location.longitude)
        assertTrue(location.isFallback)
    }

    @Test
    fun fallsBackToBerlinForInvalidCustomHomeCity() = runTest {
        val settings = FakeSettingsRepository(initialHomeCity = "Custom: invalid")
        val provider = JvmLocationProvider(settingsRepository = settings)
        val location = provider.getCurrentLocation()
        val expected = LocationUtils.CITIES.getValue("Berlin")

        assertEquals(expected.first, location.latitude)
        assertEquals(expected.second, location.longitude)
        assertTrue(location.isFallback)
    }

    @Test
    fun propagatesCancellationFromSettingsRead() = runTest {
        val cancellingSettings = object : SettingsRepository {
            override val themeMode = MutableStateFlow(ThemeMode.SYSTEM)
            override fun setThemeMode(mode: ThemeMode) {}
            override val photographerName: Flow<String> = MutableStateFlow("")
            override suspend fun updatePhotographerName(name: String) {}
            override val homeCity: Flow<String> = flow { throw CancellationException("cancelled") }
            override suspend fun updateHomeCity(city: String) {}
            override val showTestData: Flow<Boolean> = MutableStateFlow(false)
            override suspend fun updateShowTestData(show: Boolean) {}
            override val showPublicSpots: Flow<Boolean> = MutableStateFlow(true)
            override suspend fun updateShowPublicSpots(show: Boolean) {}
            override val darkMapEnabled: Flow<Boolean> = MutableStateFlow(false)
            override suspend fun updateDarkMapEnabled(enabled: Boolean) {}
            override val artistRecognitionEnabled: Flow<Boolean> = MutableStateFlow(false)
            override suspend fun updateArtistRecognitionEnabled(enabled: Boolean) {}
            override val geminiApiKey: Flow<String> = MutableStateFlow("")
            override suspend fun updateGeminiApiKey(key: String) {}
        }
        val provider = JvmLocationProvider(settingsRepository = cancellingSettings)

        assertFailsWith<CancellationException> {
            provider.getCurrentLocation()
        }
    }
}
