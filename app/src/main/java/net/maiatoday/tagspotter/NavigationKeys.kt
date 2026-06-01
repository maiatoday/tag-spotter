package net.maiatoday.tagspotter

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object Main : NavKey

@Serializable
data class DetailKey(val spotId: Long) : NavKey

@Serializable
data class TaggingKey(
    val imagePath: String,
    val thumbnailPath: String,
    val latitude: Double,
    val longitude: Double,
    val isFallback: Boolean,
    val defaultCategory: String = "graffiti",
    val captureTime: Long? = null
) : NavKey
