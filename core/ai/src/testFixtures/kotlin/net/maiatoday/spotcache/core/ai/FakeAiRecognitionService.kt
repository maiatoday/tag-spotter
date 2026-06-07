package net.maiatoday.spotcache.core.ai

class FakeAiRecognitionService : AiRecognitionService {
    var identifyArtistResult: AiSuggestion? = null
    var identifyArtistException: Exception? = null
    var searchWikipediaResult: String? = null
    var searchWikipediaException: Exception? = null

    override suspend fun identifyArtist(imagePath: String, apiKey: String, category: String): AiSuggestion? {
        identifyArtistException?.let { throw it }
        return identifyArtistResult
    }

    override suspend fun searchWikipediaForSpot(title: String, apiKey: String): String? {
        searchWikipediaException?.let { throw it }
        return searchWikipediaResult
    }
}
