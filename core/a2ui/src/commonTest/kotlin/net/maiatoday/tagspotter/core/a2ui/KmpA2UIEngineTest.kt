package net.maiatoday.tagspotter.core.a2ui

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import net.maiatoday.tagspotter.core.a2ui.engine.KmpA2UIEngine
import net.maiatoday.tagspotter.core.a2ui.engine.toNativeValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KmpA2UIEngineTest {

    @Test
    fun testProcessProtocolMessage() {
        val engine = KmpA2UIEngine()
        val json = """
            [
              {
                "version": "v0.9",
                "createSurface": {
                  "surfaceId": "test-surf-1",
                  "catalogId": "net.maiatoday.tagspotter.hybrid"
                }
              },
              {
                "version": "v0.9",
                "updateDataModel": {
                  "surfaceId": "test-surf-1",
                  "path": "/title",
                  "value": "Hello A2UI"
                }
              },
              {
                "version": "v0.9",
                "updateComponents": {
                  "surfaceId": "test-surf-1",
                  "components": [
                    {
                      "id": "root",
                      "type": "Text",
                      "properties": {
                        "text": {"path": "/title"}
                      }
                    }
                  ]
                }
              }
            ]
        """.trimIndent()

        val surfaces = engine.processProtocolMessage(json)
        assertEquals(listOf("test-surf-1"), surfaces)

        val surface = engine.stateManager.getSurface("test-surf-1")
        assertNotNull(surface)

        val dataModel = engine.stateManager.getDataModel("test-surf-1")
        assertNotNull(dataModel)
        assertEquals("Hello A2UI", dataModel.getString("/title"))

        // Test updateDataModel
        engine.updateDataModel("test-surf-1", "/title", "Updated Title")
        assertEquals("Updated Title", dataModel.getString("/title"))

        // Test clearSurfaces
        engine.clearSurfaces()
        assertTrue(engine.stateManager.getSurface("test-surf-1") == null)
    }

    @Test
    fun testSchemaGeneration() {
        val engine = KmpA2UIEngine()
        val schema = engine.getCatalogSchemaJson()
        assertNotNull(schema)
        assertTrue(schema.isNotBlank())
        assertTrue(schema.contains("type") || schema.contains("catalogId"))
    }

    @Test
    fun testAllSixFixtureTypesHydrateAndNormalize() {
        val engine = KmpA2UIEngine()

        // 1. SpotCard with isSaved binding
        val spotJson = """
            [
              {"version":"v0.9","createSurface":{"surfaceId":"spot-1","catalogId":"net.maiatoday.tagspotter.hybrid"}},
              {"version":"v0.9","updateDataModel":{"surfaceId":"spot-1","path":"/spot","value":{"title":"Cosmic Chameleon","artist":"AeroFlux","isStarred":false,"isSaved":false}}},
              {"version":"v0.9","updateComponents":{"surfaceId":"spot-1","components":[
                {"id":"root","type":"SpotCard","properties":{"spotId":"spot-01","title":{"path":"/spot/title"},"artist":{"path":"/spot/artist"},"tags":["stencil","mural"],"latitude":52.49,"longitude":13.24,"isStarred":{"path":"/spot/isStarred"},"isSaved":{"path":"/spot/isSaved"}}}
              ]}}
            ]
        """.trimIndent()
        assertEquals(listOf("spot-1"), engine.processProtocolMessage(spotJson))
        assertNotNull(engine.stateManager.getSurface("spot-1"))
        val spotData = engine.stateManager.getDataModel("spot-1")
        assertNotNull(spotData)
        assertEquals(false, spotData.getBoolean("/spot/isSaved"))
        engine.updateDataModel("spot-1", "/spot/isSaved", true)
        assertEquals(true, spotData.getBoolean("/spot/isSaved"))

        // 2. TourCard
        val tourJson = """
            [
              {"version":"v0.9","createSurface":{"surfaceId":"tour-1","catalogId":"net.maiatoday.tagspotter.hybrid"}},
              {"version":"v0.9","updateDataModel":{"surfaceId":"tour-1","path":"/tour","value":{"title":"Mitte Tour","description":"4 murals","estimatedMinutes":45,"totalDistanceKm":2.1}}},
              {"version":"v0.9","updateComponents":{"surfaceId":"tour-1","components":[
                {"id":"root","type":"TourCard","properties":{"tourTitle":{"path":"/tour/title"},"description":{"path":"/tour/description"},"estimatedMinutes":{"path":"/tour/estimatedMinutes"},"totalDistanceKm":{"path":"/tour/totalDistanceKm"},"waypoints":[{"index":1,"title":"Stop 1","latitude":52.52,"longitude":13.40,"completed":false}]}}
              ]}}
            ]
        """.trimIndent()
        assertEquals(listOf("tour-1"), engine.processProtocolMessage(tourJson))
        assertNotNull(engine.stateManager.getSurface("tour-1"))

        // 3. ArtistBioCard
        val artistJson = """
            [
              {"version":"v0.9","createSurface":{"surfaceId":"artist-1","catalogId":"net.maiatoday.tagspotter.hybrid"}},
              {"version":"v0.9","updateDataModel":{"surfaceId":"artist-1","path":"/artist","value":{"artistName":"Kira Volt","bio":"UV stencils"}}},
              {"version":"v0.9","updateComponents":{"surfaceId":"artist-1","components":[
                {"id":"root","type":"ArtistBioCard","properties":{"artistName":{"path":"/artist/artistName"},"bio":{"path":"/artist/bio"},"styles":["Stencil","Mural"],"artworks":[{"id":"a1","title":"Aurora","imageUrl":"https://example.com/a1.jpg"}]}}
              ]}}
            ]
        """.trimIndent()
        assertEquals(listOf("artist-1"), engine.processProtocolMessage(artistJson))
        assertNotNull(engine.stateManager.getSurface("artist-1"))

        // 4. Quiz (Basic Catalog + canonical event action + ChoicePicker + Text variant)
        val quizJson = """
            [
              {"version":"v0.9","createSurface":{"surfaceId":"quiz-1","catalogId":"net.maiatoday.tagspotter.hybrid"}},
              {"version":"v0.9","updateComponents":{"surfaceId":"quiz-1","components":[
                {"id":"root","type":"Card","properties":{"child":"col1"}},
                {"id":"col1","type":"Column","properties":{"children":["q_title","picker","submit_btn"]}},
                {"id":"q_title","type":"Text","properties":{"text":"Style Quiz","variant":"H3"}},
                {"id":"picker","type":"ChoicePicker","properties":{"options":[{"id":"opt-1","label":"Wheatpaste"},{"id":"opt-2","label":"Stencil"}],"value":{"path":"/selectedOption"}}},
                {"id":"submit_btn","type":"Button","properties":{"child":"btn_label","action":{"event":{"name":"answer_quiz","context":{"quizId":"q1","selectedOption":{"path":"/selectedOption"}}}}}},
                {"id":"btn_label","type":"Text","properties":{"text":"Submit"}}
              ]}}
            ]
        """.trimIndent()
        assertEquals(listOf("quiz-1"), engine.processProtocolMessage(quizJson))
        val quizSurface = engine.stateManager.getSurface("quiz-1")
        assertNotNull(quizSurface)
        assertEquals("h3", quizSurface.components["q_title"]?.widgetData?.get("variant")?.jsonPrimitive?.contentOrNull)
        val normalizedOptions = quizSurface.components["picker"]?.widgetData?.get("options")?.jsonArray
        assertNotNull(normalizedOptions)
        assertEquals("opt-1", normalizedOptions[0].jsonObject["value"]?.jsonPrimitive?.contentOrNull)

        // 5. Voting (Basic Catalog + legacy clientEvent normalization)
        val votingJson = """
            [
              {"version":"v0.9","createSurface":{"surfaceId":"vote-1","catalogId":"net.maiatoday.tagspotter.hybrid"}},
              {"version":"v0.9","updateComponents":{"surfaceId":"vote-1","components":[
                {"id":"root","type":"Card","properties":{"child":"col1"}},
                {"id":"col1","type":"Column","properties":{"children":["v_title","v_picker","v_btn"]}},
                {"id":"v_title","type":"Text","properties":{"text":"Preserve Wall","variant":"Body"}},
                {"id":"v_picker","type":"ChoicePicker","properties":{"options":[{"value":"preserve","label":"Seal it"}],"value":{"path":"/selectedOption"}}},
                {"id":"v_btn","type":"Button","properties":{"child":"v_label","action":{"functionCall":{"call":"clientEvent","args":{"name":"vote_option","pollId":"p1","selectedOption":{"path":"/selectedOption"}}}}}},
                {"id":"v_label","type":"Text","properties":{"text":"Vote"}}
              ]}}
            ]
        """.trimIndent()
        assertEquals(listOf("vote-1"), engine.processProtocolMessage(votingJson))
        val voteSurface = engine.stateManager.getSurface("vote-1")
        assertNotNull(voteSurface)
        assertEquals("body", voteSurface.components["v_title"]?.widgetData?.get("variant")?.jsonPrimitive?.contentOrNull)
        val voteAction = voteSurface.components["v_btn"]?.widgetData?.get("action")?.jsonObject
        assertEquals("vote_option", voteAction?.get("event")?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull)

        // 6. MapPinOverlay + MapPinOverlayWidget alias normalization
        val mapAliasJson = """
            [
              {"version":"v0.9","createSurface":{"surfaceId":"map-1","catalogId":"net.maiatoday.tagspotter.hybrid"}},
              {"version":"v0.9","updateComponents":{"surfaceId":"map-1","components":[
                {"id":"root","type":"MapPinOverlayWidget","properties":{"title":"Kreuzberg Cluster","centerLatitude":52.499,"centerLongitude":13.418,"zoomLevel":15.0,"pins":[{"id":"p1","title":"Astronaut","latitude":52.5015,"longitude":13.419,"category":"Mural"}]}}
              ]}}
            ]
        """.trimIndent()
        assertEquals(listOf("map-1"), engine.processProtocolMessage(mapAliasJson))
        val mapSurface = engine.stateManager.getSurface("map-1")
        assertNotNull(mapSurface)
        assertEquals("MapPinOverlay", mapSurface.components["root"]?.widgetType)
    }

    @Test
    fun testJsonElementToNativeValuePreservesListsAndNumbers() {
        val ctx = buildJsonObject {
            put("spotId", "spot-1")
            put("latitude", 52.4972)
            put("isSaved", true)
            put("tags", JsonArray(listOf(JsonPrimitive("stencil"), JsonPrimitive("mural"))))
        }
        val nativeMap = ctx.toNativeValue() as Map<*, *>
        assertEquals("spot-1", nativeMap["spotId"])
        assertEquals(52.4972, nativeMap["latitude"])
        assertEquals(true, nativeMap["isSaved"])
        assertEquals(listOf("stencil", "mural"), nativeMap["tags"])
    }
}
