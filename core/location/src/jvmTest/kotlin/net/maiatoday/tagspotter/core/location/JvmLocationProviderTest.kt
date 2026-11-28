package net.maiatoday.tagspotter.core.location

import kotlinx.coroutines.test.runTest
import net.maiatoday.tagspotter.core.model.LocationUtils
import net.maiatoday.tagspotter.core.settings.FakeSettingsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
