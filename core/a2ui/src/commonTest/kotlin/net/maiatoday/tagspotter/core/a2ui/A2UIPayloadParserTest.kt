package net.maiatoday.tagspotter.core.a2ui

import net.maiatoday.tagspotter.core.a2ui.model.ArtistBioCardData
import net.maiatoday.tagspotter.core.a2ui.model.CommunityVotingData
import net.maiatoday.tagspotter.core.a2ui.model.FallbackComponentData
import net.maiatoday.tagspotter.core.a2ui.model.MapPinOverlayData
import net.maiatoday.tagspotter.core.a2ui.model.SpotCardData
import net.maiatoday.tagspotter.core.a2ui.model.StyleQuizData
import net.maiatoday.tagspotter.core.a2ui.model.TourCardData
import net.maiatoday.tagspotter.core.a2ui.parser.A2UIPayloadParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class A2UIPayloadParserTest {

    @Test
    fun parseSpotCard_withMarkdownFences() {
        val payload = """
            ```json
            {
              "type": "spot_card",
              "data": {
                "spotId": "spot-001",
                "title": "Neon Wings Mural",
                "artist": "AeroFlux",
                "imageUrl": "https://images.example.com/art1.jpg",
                "latitude": 52.5200,
                "longitude": 13.4050,
                "distanceMeters": 145.0,
                "tags": ["stencil", "glow", "berlin"],
                "isStarred": true
              }
            }
            ```
        """.trimIndent()

        val component = A2UIPayloadParser.parse(payload)
        assertIs<SpotCardData>(component)
        assertEquals("spot-001", component.spotId)
        assertEquals("Neon Wings Mural", component.title)
        assertEquals("AeroFlux", component.artist)
        assertEquals(52.5200, component.latitude)
        assertEquals(13.4050, component.longitude)
        assertEquals(145.0, component.distanceMeters)
        assertEquals(listOf("stencil", "glow", "berlin"), component.tags)
        assertTrue(component.isStarred)
    }

    @Test
    fun parseTourCard_flatStructure() {
        val payload = """
            {
              "type": "tour_card",
              "tourId": "tour-mitte-01",
              "tourTitle": "Mitte Cyber-Street Route",
              "description": "A 40-minute tour across Berlin Mitte hidden walls.",
              "totalDistanceKm": 1.85,
              "estimatedMinutes": 40,
              "waypoints": [
                {
                  "index": 1,
                  "title": "The Glitch Wall",
                  "address": "Rosenthaler Str. 39",
                  "latitude": 52.521,
                  "longitude": 13.401,
                  "completed": true
                },
                {
                  "index": 2,
                  "title": "Neon Falcon",
                  "address": "Linienstr. 12",
                  "latitude": 52.523,
                  "longitude": 13.404,
                  "completed": false
                }
              ]
            }
        """.trimIndent()

        val component = A2UIPayloadParser.parse(payload)
        assertIs<TourCardData>(component)
        assertEquals("tour-mitte-01", component.tourId)
        assertEquals("Mitte Cyber-Street Route", component.tourTitle)
        assertEquals(2, component.waypoints.size)
        assertEquals("The Glitch Wall", component.waypoints[0].title)
        assertTrue(component.waypoints[0].completed)
        assertEquals("Neon Falcon", component.waypoints[1].title)
        assertEquals(false, component.waypoints[1].completed)
    }

    @Test
    fun parseArtistBioCard() {
        val payload = """
            {
              "type": "artist_bio_card",
              "data": {
                "artistName": "PixelRebel",
                "avatarUrl": "https://images.example.com/avatar.jpg",
                "bio": "Berlin-based digital stencil pioneer active since 2017.",
                "activeYears": "2017 - Present",
                "styles": ["pixel-art", "paste-up", "augmented"],
                "artworks": [
                  {
                    "id": "spot-01",
                    "title": "8-Bit Dragon",
                    "imageUrl": "https://images.example.com/dragon.jpg"
                  }
                ]
              }
            }
        """.trimIndent()

        val component = A2UIPayloadParser.parse(payload)
        assertIs<ArtistBioCardData>(component)
        assertEquals("PixelRebel", component.artistName)
        assertEquals(1, component.artworks.size)
        assertEquals("8-Bit Dragon", component.artworks[0].title)
    }

    @Test
    fun parseStyleQuiz() {
        val payload = """
            {
              "type": "style_quiz",
              "quizId": "quiz-01",
              "question": "What movement inspired this neon gradient style?",
              "imageUrl": "https://images.example.com/quiz.jpg",
              "options": [
                { "id": "opt-1", "text": "Outrun / Synthwave", "isCorrect": true },
                { "id": "opt-2", "text": "Brutalism", "isCorrect": false }
              ],
              "explanation": "Outrun aesthetics from the 80s feature vibrant neon palettes."
            }
        """.trimIndent()

        val component = A2UIPayloadParser.parse(payload)
        assertIs<StyleQuizData>(component)
        assertEquals("quiz-01", component.quizId)
        assertEquals(2, component.options.size)
        assertTrue(component.options[0].isCorrect)
    }

    @Test
    fun parseCommunityVoting() {
        val payload = """
            {
              "type": "community_voting",
              "pollId": "poll-42",
              "title": "Should this wall remain protected as cultural street heritage?",
              "description": "Community vote for open wall preservation.",
              "options": [
                { "id": "yes", "label": "Yes, preserve it!", "votes": 128 },
                { "id": "no", "label": "No, let new artists paint over", "votes": 34 }
              ],
              "totalVotes": 162
            }
        """.trimIndent()

        val component = A2UIPayloadParser.parse(payload)
        assertIs<CommunityVotingData>(component)
        assertEquals("poll-42", component.pollId)
        assertEquals("Should this wall remain protected as cultural street heritage?", component.title)
        assertEquals(162, component.totalVotes)
    }

    @Test
    fun parseMapPinOverlay() {
        val payload = """
            {
              "type": "map_pin_overlay",
              "title": "Kreuzberg Murals",
              "centerLatitude": 52.499,
              "centerLongitude": 13.418,
              "zoomLevel": 14.0,
              "pins": [
                {
                  "id": "pin-1",
                  "title": "Astronaut Cosmonaut",
                  "latitude": 52.501,
                  "longitude": 13.419,
                  "category": "Mural"
                }
              ]
            }
        """.trimIndent()

        val component = A2UIPayloadParser.parse(payload)
        assertIs<MapPinOverlayData>(component)
        assertEquals("Kreuzberg Murals", component.title)
        assertEquals(1, component.pins.size)
        assertEquals("Astronaut Cosmonaut", component.pins[0].title)
    }

    @Test
    fun parseUnknownType_returnsFallback() {
        val payload = """
            {
              "type": "unsupported_widget",
              "data": { "foo": "bar" }
            }
        """.trimIndent()

        val component = A2UIPayloadParser.parse(payload)
        assertIs<FallbackComponentData>(component)
        assertTrue(component.title.contains("Unrecognized A2UI Component"))
    }

    @Test
    fun parseMalformedJson_returnsFallback() {
        val payload = "{ this is totally invalid json }"

        val component = A2UIPayloadParser.parse(payload)
        assertIs<FallbackComponentData>(component)
        assertTrue(component.title.contains("A2UI Parsing Error"))
    }
}
