package net.maiatoday.tagspotter.data.service

import android.content.Context
import net.maiatoday.tagspotter.data.Spot
import net.maiatoday.tagspotter.domain.GeofenceService
import net.maiatoday.tagspotter.utils.GeofenceManager

class AndroidGeofenceService(private val context: Context) : GeofenceService {
    private val geofenceManager = GeofenceManager(context)

    override fun registerGeofence(id: Long, latitude: Double, longitude: Double, onResult: (Boolean) -> Unit) {
        val dummySpot = Spot(
            id = id,
            latitude = latitude,
            longitude = longitude,
            createdAt = 0L,
            description = "",
            tags = emptyList(),
            category = "",
            status = ""
        )
        geofenceManager.registerGeofence(dummySpot, onResult)
    }

    override fun unregisterGeofence(id: Long) {
        geofenceManager.unregisterGeofence(id)
    }
}
