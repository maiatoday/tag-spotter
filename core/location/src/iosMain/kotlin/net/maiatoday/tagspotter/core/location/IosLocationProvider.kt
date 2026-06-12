package net.maiatoday.tagspotter.core.location

import platform.CoreLocation.CLLocationManager
import kotlinx.cinterop.useContents
import kotlinx.cinterop.ExperimentalForeignApi

class IosLocationProvider : LocationProvider {
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun getCurrentLocation(): LocationData? {
        val locationManager = CLLocationManager()
        val location = locationManager.location
        if (location != null) {
            val coordinate = location.coordinate
            return LocationData(
                latitude = coordinate.useContents { latitude },
                longitude = coordinate.useContents { longitude },
                isFallback = true
            )
        }
        return null
    }
}
