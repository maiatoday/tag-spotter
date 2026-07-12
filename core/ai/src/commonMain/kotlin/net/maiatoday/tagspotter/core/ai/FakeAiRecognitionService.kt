package net.maiatoday.tagspotter.core.ai

class FakeAiRecognitionService : AiRecognitionService {
    override var isSupported: Boolean = true

    var identifyArtistResult: AiSuggestion? = null
    var identifyArtistException: Exception? = null
    var searchWikipediaResult: String? = null
    var searchWikipediaException: Exception? = null

    var lastIdentifyImagePath: String? = null
    var lastIdentifyCategory: String? = null
    var lastIdentifyCurrentArtist: String? = null
    var lastIdentifyCurrentTitle: String? = null
    var lastIdentifyThumbnailPath: String? = null

    override suspend fun identifyArtist(
        imagePath: String,
        category: String,
        currentArtist: String?,
        currentTitle: String?,
        thumbnailPath: String?
    ): AiSuggestion? {
        lastIdentifyImagePath = imagePath
        lastIdentifyCategory = category
        lastIdentifyCurrentArtist = currentArtist
        lastIdentifyCurrentTitle = currentTitle
        lastIdentifyThumbnailPath = thumbnailPath

        identifyArtistException?.let { throw it }
        return identifyArtistResult
    }

    override suspend fun searchWikipediaForSpot(
        title: String,
        category: String,
        artists: List<String>
    ): String? {
        searchWikipediaException?.let { throw it }
        return searchWikipediaResult
    }
}
