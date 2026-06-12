package net.maiatoday.tagspotter.core.location

class JvmLocationProvider : LocationProvider {
    override suspend fun getCurrentLocation(): LocationData? {
        return LocationData(
            latitude = 48.1351, // Munich
            longitude = 11.5820,
            isFallback = false
        )
    }
}
