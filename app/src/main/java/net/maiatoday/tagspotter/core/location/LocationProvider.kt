package net.maiatoday.tagspotter.core.location

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val isFallback: Boolean
)

interface LocationProvider {
    suspend fun getCurrentLocation(): LocationData?
}
