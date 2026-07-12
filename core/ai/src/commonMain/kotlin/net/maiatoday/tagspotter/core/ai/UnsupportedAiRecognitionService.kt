package net.maiatoday.tagspotter.core.ai

class UnsupportedAiRecognitionService : AiRecognitionService {
    override val isSupported: Boolean = false

    override suspend fun identifyArtist(
        imagePath: String,
        category: String,
        currentArtist: String?,
        currentTitle: String?,
        thumbnailPath: String?
    ): AiSuggestion? {
        throw UnsupportedOperationException("AI features are not supported on this platform.")
    }

    override suspend fun searchWikipediaForSpot(
        title: String,
        category: String,
        artists: List<String>
    ): String? {
        throw UnsupportedOperationException("AI features are not supported on this platform.")
    }
}
