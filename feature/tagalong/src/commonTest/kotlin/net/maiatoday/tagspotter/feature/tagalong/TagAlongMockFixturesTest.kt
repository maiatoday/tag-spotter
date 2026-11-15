package net.maiatoday.tagspotter.feature.tagalong

import net.maiatoday.tagspotter.core.a2ui.model.ArtistBioCardData
import net.maiatoday.tagspotter.core.a2ui.model.CommunityVotingData
import net.maiatoday.tagspotter.core.a2ui.model.FallbackComponentData
import net.maiatoday.tagspotter.core.a2ui.model.MapPinOverlayData
import net.maiatoday.tagspotter.core.a2ui.model.SpotCardData
import net.maiatoday.tagspotter.core.a2ui.model.StyleQuizData
import net.maiatoday.tagspotter.core.a2ui.model.TourCardData
import net.maiatoday.tagspotter.core.a2ui.parser.A2UIPayloadParser
import net.maiatoday.tagspotter.feature.tagalong.fixtures.TagAlongMockFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TagAlongMockFixturesTest {

    @Test
    fun testSpotDossierFixture() {
        val component = A2UIPayloadParser.parse(TagAlongMockFixtures.SPOT_DOSSIER_JSON)
        assertIs<SpotCardData>(component)
        assertEquals("spot-berlin-teufelsberg-01", component.spotId)
        assertEquals("Cosmic Chameleon", component.title)
        assertEquals("AeroFlux", component.artist)
    }

    @Test
    fun testTourSampleFixture() {
        val component = A2UIPayloadParser.parse(TagAlongMockFixtures.TOUR_SAMPLE_JSON)
        assertIs<TourCardData>(component)
        assertEquals("tour-mitte-hidden-gems", component.tourId)
        assertEquals(4, component.waypoints.size)
    }

    @Test
    fun testArtistBioFixture() {
        val component = A2UIPayloadParser.parse(TagAlongMockFixtures.ARTIST_BIO_JSON)
        assertIs<ArtistBioCardData>(component)
        assertEquals("Kira 'Volt' Jensen", component.artistName)
        assertEquals(3, component.artworks.size)
    }

    @Test
    fun testQuizSampleFixture() {
        val component = A2UIPayloadParser.parse(TagAlongMockFixtures.QUIZ_SAMPLE_JSON)
        assertIs<StyleQuizData>(component)
        assertEquals("quiz-berlin-styles-01", component.quizId)
        assertEquals(3, component.options.size)
    }

    @Test
    fun testVotingSampleFixture() {
        val component = A2UIPayloadParser.parse(TagAlongMockFixtures.VOTING_SAMPLE_JSON)
        assertIs<CommunityVotingData>(component)
        assertEquals("poll-wall-curation-01", component.pollId)
        assertEquals(3, component.options.size)
        assertEquals(307, component.totalVotes)
    }

    @Test
    fun testMapOverlayFixture() {
        val component = A2UIPayloadParser.parse(TagAlongMockFixtures.MAP_OVERLAY_JSON)
        assertIs<MapPinOverlayData>(component)
        assertEquals("Kreuzberg Neon Cluster", component.title)
        assertEquals(3, component.pins.size)
    }

    @Test
    fun testMalformedFixture() {
        val component = A2UIPayloadParser.parse(TagAlongMockFixtures.MALFORMED_JSON)
        assertIs<FallbackComponentData>(component)
    }
}
