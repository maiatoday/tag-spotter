package net.maiatoday.tagspotter.core.model

import kotlinx.serialization.Serializable

@Serializable
data class LoadedPack(
    val packId: String,
    val title: String,
    val authorName: String,
    val description: String,
    val importedAt: Long,
    val lastRefreshedAt: Long
)

@Serializable
data class SharedPack(
    val packId: String,
    val title: String,
    val authorName: String,
    val description: String,
    val spots: List<SpotDetails>
)
