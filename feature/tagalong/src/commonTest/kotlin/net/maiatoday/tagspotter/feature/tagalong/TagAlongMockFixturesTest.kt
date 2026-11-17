package net.maiatoday.tagspotter.feature.tagalong

import net.maiatoday.tagspotter.core.a2ui.engine.KmpA2UIEngine
import net.maiatoday.tagspotter.feature.tagalong.fixtures.TagAlongMockFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TagAlongMockFixturesTest {

    @Test
    fun testSpotDossierFixture() {
        val engine = KmpA2UIEngine()
        val surfaces = engine.processProtocolMessage(TagAlongMockFixtures.SPOT_DOSSIER_JSON)
        assertEquals(listOf("spot-surface-1"), surfaces)
        val surface = engine.stateManager.getSurface("spot-surface-1")
        assertNotNull(surface)
        assertEquals("root", surface.rootComponentId)
        val dataModel = engine.stateManager.getDataModel("spot-surface-1")
        assertNotNull(dataModel)
        assertEquals("Cosmic Chameleon", dataModel.getString("/spot/title"))
        assertEquals("AeroFlux", dataModel.getString("/spot/artist"))
    }

    @Test
    fun testTourSampleFixture() {
        val engine = KmpA2UIEngine()
        val surfaces = engine.processProtocolMessage(TagAlongMockFixtures.TOUR_SAMPLE_JSON)
        assertEquals(listOf("tour-surface-1"), surfaces)
        val surface = engine.stateManager.getSurface("tour-surface-1")
        assertNotNull(surface)
        assertEquals("root", surface.rootComponentId)
        val dataModel = engine.stateManager.getDataModel("tour-surface-1")
        assertNotNull(dataModel)
        assertEquals("Mitte Hidden Alleyways Tour", dataModel.getString("/tour/title"))
    }

    @Test
    fun testArtistBioFixture() {
        val engine = KmpA2UIEngine()
        val surfaces = engine.processProtocolMessage(TagAlongMockFixtures.ARTIST_BIO_JSON)
        assertEquals(listOf("artist-surface-1"), surfaces)
        val surface = engine.stateManager.getSurface("artist-surface-1")
        assertNotNull(surface)
        val dataModel = engine.stateManager.getDataModel("artist-surface-1")
        assertNotNull(dataModel)
        assertEquals("Kira 'Volt' Jensen", dataModel.getString("/artist/artistName"))
    }

    @Test
    fun testQuizSampleFixture() {
        val engine = KmpA2UIEngine()
        val surfaces = engine.processProtocolMessage(TagAlongMockFixtures.QUIZ_SAMPLE_JSON)
        assertEquals(listOf("quiz-surface-1"), surfaces)
        val surface = engine.stateManager.getSurface("quiz-surface-1")
        assertNotNull(surface)
        // Verify Basic Catalog composition (Card, Column, Text, ChoicePicker, Button)
        assertTrue(surface.components.containsKey("root"))
        assertTrue(surface.components.containsKey("col1"))
        assertTrue(surface.components.containsKey("picker"))
        assertTrue(surface.components.containsKey("submit_btn"))
    }

    @Test
    fun testVotingSampleFixture() {
        val engine = KmpA2UIEngine()
        val surfaces = engine.processProtocolMessage(TagAlongMockFixtures.VOTING_SAMPLE_JSON)
        assertEquals(listOf("voting-surface-1"), surfaces)
        val surface = engine.stateManager.getSurface("voting-surface-1")
        assertNotNull(surface)
        assertTrue(surface.components.containsKey("root"))
        assertTrue(surface.components.containsKey("v_picker"))
        assertTrue(surface.components.containsKey("v_btn"))
    }

    @Test
    fun testMapOverlayFixture() {
        val engine = KmpA2UIEngine()
        val surfaces = engine.processProtocolMessage(TagAlongMockFixtures.MAP_OVERLAY_JSON)
        assertEquals(listOf("map-surface-1"), surfaces)
        val surface = engine.stateManager.getSurface("map-surface-1")
        assertNotNull(surface)
        assertTrue(surface.components.containsKey("root"))
    }

    @Test
    fun testMalformedFixture() {
        val engine = KmpA2UIEngine()
        val surfaces = engine.processProtocolMessage(TagAlongMockFixtures.MALFORMED_JSON)
        assertEquals(listOf("malformed-surface-1"), surfaces)
        val surface = engine.stateManager.getSurface("malformed-surface-1")
        assertNotNull(surface)
    }
}
