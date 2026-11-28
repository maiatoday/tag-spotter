package net.maiatoday.tagspotter.core.location

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import net.maiatoday.tagspotter.core.model.LocationUtils
import net.maiatoday.tagspotter.core.settings.SettingsRepository

class JvmLocationProvider(
    private val settingsRepository: SettingsRepository? = null
) : LocationProvider {
    override suspend fun getCurrentLocation(): LocationData {
        val homeCity = try {
            settingsRepository?.homeCity?.first()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }?.takeIf { it.isNotBlank() } ?: LocationUtils.DEFAULT_CITY
        val (lat, lon) = LocationUtils.resolveCityCoordinates(homeCity)
        return LocationData(
            latitude = lat,
            longitude = lon,
            isFallback = true
        )
    }
}
