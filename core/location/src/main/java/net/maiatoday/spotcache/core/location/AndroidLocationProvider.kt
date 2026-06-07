package net.maiatoday.spotcache.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class AndroidLocationProvider(private val context: Context) : LocationProvider {
    override suspend fun getCurrentLocation(): LocationData? {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return null

        return try {
            val loc = LocationHelper.getCurrentLocation(context) ?: return null
            LocationData(
                latitude = loc.latitude,
                longitude = loc.longitude,
                isFallback = loc.isFallback
            )
        } catch (_: SecurityException) {
            null
        }
    }
}
