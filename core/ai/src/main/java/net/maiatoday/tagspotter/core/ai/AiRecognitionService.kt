package net.maiatoday.tagspotter.core.ai

import kotlinx.serialization.Serializable

@Serializable
data class AiSuggestion(
    val artist: String? = null,
    val title: String? = null,
    val tags: List<String> = emptyList()
)

interface AiRecognitionService {
    suspend fun identifyArtist(imagePath: String, apiKey: String, category: String): AiSuggestion?
    suspend fun searchWikipediaForSpot(title: String, apiKey: String): String?
}
