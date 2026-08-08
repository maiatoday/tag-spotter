package net.maiatoday.tagspotter.core.ai

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AiRecognitionServiceTest {

    @Test
    fun testUnsupportedAiRecognitionServiceThrowsExceptions() = runTest {
        val service = UnsupportedAiRecognitionService()
        assertFalse(service.isSupported)

        assertFailsWith<UnsupportedOperationException> {
            service.identifyArtist("path", "category", null, null, null)
        }

        assertFailsWith<UnsupportedOperationException> {
            service.searchWikipediaForSpot("title", "category", emptyList())
        }
    }

    @Test
    fun testFakeAiRecognitionServiceSuccessfulMock() = runTest {
        val service = FakeAiRecognitionService()
        assertTrue(service.isSupported)

        val suggestion = AiSuggestion(
            artist = "Banksy",
            title = "Flower Thrower",
            tags = listOf("stencil", "mural")
        )
        service.identifyArtistResult = suggestion
        service.searchWikipediaResult = "Wikipedia content"

        val result = service.identifyArtist("path", "graffiti", "old-artist", "old-title", "thumb")
        assertEquals(suggestion, result)
        assertEquals("path", service.lastIdentifyImagePath)
        assertEquals("graffiti", service.lastIdentifyCategory)
        assertEquals("old-artist", service.lastIdentifyCurrentArtist)
        assertEquals("old-title", service.lastIdentifyCurrentTitle)
        assertEquals("thumb", service.lastIdentifyThumbnailPath)

        val wiki = service.searchWikipediaForSpot("title", "category", emptyList())
        assertEquals("Wikipedia content", wiki)
    }

    @Test
    fun testFakeAiRecognitionServiceThrowsException() = runTest {
        val service = FakeAiRecognitionService()
        service.identifyArtistException = RuntimeException("Model load error")
        service.searchWikipediaException = RuntimeException("Network down")

        assertFailsWith<RuntimeException> {
            service.identifyArtist("path", "graffiti", null, null, null)
        }

        assertFailsWith<RuntimeException> {
            service.searchWikipediaForSpot("title", "category", emptyList())
        }
    }
}
