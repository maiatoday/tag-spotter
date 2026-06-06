package net.maiatoday.tagspotter.core.location

import android.content.Context

class AndroidLocationProvider(private val context: Context) : LocationProvider {
    override suspend fun getCurrentLocation(): LocationData? {
        val loc = LocationHelper.getCurrentLocation(context) ?: return null
        return LocationData(
            latitude = loc.latitude,
            longitude = loc.longitude,
            isFallback = loc.isFallback
        )
    }
}
