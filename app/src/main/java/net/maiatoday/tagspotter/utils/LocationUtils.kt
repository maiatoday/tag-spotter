package net.maiatoday.tagspotter.utils

import kotlin.math.*

@kotlinx.serialization.Serializable
sealed interface FilterCenter {
    val latitude: Double
    val longitude: Double
    val displayName: String

    @kotlinx.serialization.Serializable
    data class GPS(
        override val latitude: Double,
        override val longitude: Double
    ) : FilterCenter {
        override val displayName: String = "Near Me"
    }

    @kotlinx.serialization.Serializable
    data class HomeCity(
        val cityName: String,
        override val latitude: Double,
        override val longitude: Double
    ) : FilterCenter {
        override val displayName: String = "Home ($cityName)"
    }

    @kotlinx.serialization.Serializable
    data class FocusCity(
        val cityName: String,
        override val latitude: Double,
        override val longitude: Double
    ) : FilterCenter {
        override val displayName: String = cityName
    }
}

object LocationUtils {
    /**
     * Calculates the distance in meters between two coordinates using the Haversine formula.
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Converts a raw value (0.0 to 1.0) on a slider to a logarithmic distance preset.
     * Presets: 500m, 1km, 2km, 5km, 10km, 20km, 50km
     */
    fun getLogarithmicRadiusMeters(sliderValue: Float): Double {
        return when {
            sliderValue < 0.15f -> 500.0
            sliderValue < 0.30f -> 1000.0
            sliderValue < 0.45f -> 2000.0
            sliderValue < 0.60f -> 5000.0
            sliderValue < 0.75f -> 10000.0
            sliderValue < 0.90f -> 20000.0
            else -> 50000.0
        }
    }

    /**
     * Returns a float value (0.0 to 1.0) corresponding to the radius preset.
     */
    fun getSliderValueForRadius(radiusMeters: Double): Float {
        return when {
            radiusMeters <= 500.0 -> 0.0f
            radiusMeters <= 1000.0 -> 0.2f
            radiusMeters <= 2000.0 -> 0.38f
            radiusMeters <= 5000.0 -> 0.53f
            radiusMeters <= 10000.0 -> 0.68f
            radiusMeters <= 20000.0 -> 0.83f
            else -> 1.0f
        }
    }

    fun getRadiusLabel(radiusMeters: Double): String {
        return if (radiusMeters < 1000.0) {
            "${radiusMeters.toInt()}m"
        } else {
            "${(radiusMeters / 1000.0).toInt()}km"
        }
    }
}
