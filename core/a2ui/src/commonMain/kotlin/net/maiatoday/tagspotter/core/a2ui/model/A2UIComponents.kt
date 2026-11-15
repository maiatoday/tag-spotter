package net.maiatoday.tagspotter.core.a2ui.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface A2UIComponent

@Serializable
@SerialName("SpotCard")
data class SpotCardData(
    val spotId: String,
    val title: String,
    val artist: String? = null,
    val imageUrl: String? = null,
    val tags: List<String> = emptyList(),
    val distanceMeters: Double? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isStarred: Boolean = false
) : A2UIComponent

@Serializable
@SerialName("TourWaypoint")
data class TourWaypointData(
    val index: Int,
    val title: String,
    val address: String? = null,
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String? = null,
    val completed: Boolean = false
)

@Serializable
@SerialName("TourCard")
data class TourCardData(
    val tourId: String = "",
    val tourTitle: String,
    val description: String = "",
    val estimatedMinutes: Int = 0,
    val totalDistanceKm: Double = 0.0,
    val waypoints: List<TourWaypointData> = emptyList()
) : A2UIComponent

@Serializable
@SerialName("ArtworkThumbnail")
data class ArtworkThumbnailData(
    val id: String,
    val title: String,
    val imageUrl: String
)

@Serializable
@SerialName("ArtistBioCard")
data class ArtistBioCardData(
    val artistName: String,
    val avatarUrl: String? = null,
    val bio: String,
    val activeYears: String? = null,
    val styles: List<String> = emptyList(),
    val artworks: List<ArtworkThumbnailData> = emptyList()
) : A2UIComponent

@Serializable
@SerialName("QuizOption")
data class QuizOptionData(
    val id: String,
    val text: String,
    val isCorrect: Boolean = false
)

@Serializable
@SerialName("StyleQuizWidget")
data class StyleQuizData(
    val quizId: String,
    val question: String,
    val imageUrl: String? = null,
    val options: List<QuizOptionData> = emptyList(),
    val explanation: String? = null
) : A2UIComponent

@Serializable
@SerialName("VoteOption")
data class VoteOptionData(
    val id: String,
    val label: String,
    val votes: Int = 0
)

@Serializable
@SerialName("CommunityVotingCard")
data class CommunityVotingData(
    val pollId: String,
    val title: String,
    val description: String = "",
    val imageUrl: String? = null,
    val options: List<VoteOptionData> = emptyList(),
    val totalVotes: Int = 0
) : A2UIComponent

@Serializable
@SerialName("MapPinItem")
data class MapPinItemData(
    val id: String,
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val category: String? = null
)

@Serializable
@SerialName("MapPinOverlayWidget")
data class MapPinOverlayData(
    val title: String,
    val centerLatitude: Double,
    val centerLongitude: Double,
    val zoomLevel: Float = 14f,
    val pins: List<MapPinItemData> = emptyList()
) : A2UIComponent

@Serializable
@SerialName("FallbackCard")
data class FallbackComponentData(
    val title: String = "Unable to render dynamic card",
    val description: String = "The response received from the agent could not be parsed as a known A2UI component.",
    val rawJson: String = ""
) : A2UIComponent
