package net.maiatoday.tagspotter.core.location

import android.content.Context
import net.maiatoday.tagspotter.core.model.Spot

class AndroidGeofenceService(context: Context) : GeofenceService {
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
