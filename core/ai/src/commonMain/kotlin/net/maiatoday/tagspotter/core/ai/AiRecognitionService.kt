package net.maiatoday.tagspotter.core.ai

import kotlinx.serialization.Serializable

@Serializable
data class AiSuggestion(
    val artist: String? = null,
    val title: String? = null,
    val tags: List<String> = emptyList()
)

interface AiRecognitionService {
    val isSupported: Boolean

    suspend fun identifyArtist(
        imagePath: String,
        category: String,
        currentArtist: String? = null,
        currentTitle: String? = null,
        thumbnailPath: String? = null
    ): AiSuggestion?

    suspend fun searchWikipediaForSpot(
        title: String,
        category: String,
        artists: List<String>
    ): String?
}
