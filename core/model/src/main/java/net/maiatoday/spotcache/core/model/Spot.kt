package net.maiatoday.spotcache.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Spot(
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val createdAt: Long,
    val description: String,
    val tags: List<String>,
    val category: String, // e.g. "graffiti", "sculpture", "nature", "architecture"
    val status: String, // "active" or "erased"
    val artists: List<String> = emptyList(),
    val photographer: String = "",
    val isImported: Boolean = false,
    val isStarred: Boolean = false,
    val artworkDate: String = ""
)

@Serializable
data class SpotImage(
    val id: Long = 0,
    val spotId: Long,
    val imagePath: String,
    val timestamp: Long,
    val thumbnailPath: String = "",
    val isMain: Boolean = false,
    val rating: Int = 0
)

@Serializable
data class SpotNote(
    val id: Long = 0,
    val spotId: Long,
    val noteText: String,
    val timestamp: Long
)

@Serializable
data class SpotDetails(
    val spot: Spot,
    val images: List<SpotImage>,
    val notes: List<SpotNote>
)
