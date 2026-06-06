package net.maiatoday.tagspotter.domain

interface GeofenceService {
    fun registerGeofence(id: Long, latitude: Double, longitude: Double, onResult: (Boolean) -> Unit = {})
    fun unregisterGeofence(id: Long)
}
