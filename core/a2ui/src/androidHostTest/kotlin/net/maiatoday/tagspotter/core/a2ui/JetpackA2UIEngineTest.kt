package net.maiatoday.tagspotter.core.a2ui

import androidx.a2ui.compose.ui.toJsonSchemaString
import androidx.a2ui.model.protocol.A2uiExecutionContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import net.maiatoday.tagspotter.core.a2ui.engine.JETPACK_HYBRID_CATALOG_URI
import net.maiatoday.tagspotter.core.a2ui.engine.JetpackA2UIEngine
import net.maiatoday.tagspotter.core.a2ui.engine.createJetpackHybridCatalog
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class JetpackA2UIEngineTest {

    @Test
    fun catalog_generatesValidJsonSchemaWithDomainComponents() {
        val catalog = createJetpackHybridCatalog()
        val schemaJson: String = catalog.toJsonSchemaString()

        assertNotNull(schemaJson)
        assertTrue(schemaJson.contains("SpotCard"), "Schema should declare SpotCard")
        assertTrue(schemaJson.contains("isSaved"), "SpotCard schema should declare isSaved")
        assertTrue(schemaJson.contains("TourCard"), "Schema should declare TourCard")
        assertTrue(schemaJson.contains("ArtistBioCard"), "Schema should declare ArtistBioCard")
        assertTrue(schemaJson.contains("MapPinOverlay"), "Schema should declare MapPinOverlay")
        assertTrue(schemaJson.contains("focusMap"), "Schema should declare focusMap function")
        assertTrue(schemaJson.contains("openSpotDetail"), "Schema should declare openSpotDetail function")
        assertTrue(schemaJson.contains("ChoicePicker"), "Schema should declare Basic Catalog ChoicePicker")
        assertTrue(schemaJson.contains("Button"), "Schema should declare Basic Catalog Button")
        assertTrue(schemaJson.contains("TextField"), "Schema should declare Basic Catalog TextField")
    }

    @Test
    fun engine_localFunctionCallbacksEmitActions() = runTest {
        withContext(Dispatchers.Default) {
            val engine = JetpackA2UIEngine()
            val dummyContext = Proxy.newProxyInstance(
                A2uiExecutionContext::class.java.classLoader,
                arrayOf(A2uiExecutionContext::class.java)
            ) { _, _, _ -> null } as A2uiExecutionContext

            val focusMapFn = engine.catalog.functions.find { it.definition.name == "focusMap" }
            assertNotNull(focusMapFn, "focusMap function should be registered")

            val focusDeferred = async {
                withTimeout(5000) {
                    engine.outboundEvents.first { it is A2UIAction.ViewOnMap }
                }
            }
            delay(50)
            focusMapFn.execute(
                mapOf("latitude" to 52.4972, "longitude" to 13.2415, "title" to "Teufelsberg"),
                dummyContext
            )
            val focusAction = focusDeferred.await() as A2UIAction.ViewOnMap
            assertEquals(52.4972, focusAction.latitude)
            assertEquals(13.2415, focusAction.longitude)
            assertEquals("Teufelsberg", focusAction.title)

            val openDetailFn = engine.catalog.functions.find { it.definition.name == "openSpotDetail" }
            assertNotNull(openDetailFn, "openSpotDetail function should be registered")

            val detailDeferred = async {
                withTimeout(5000) {
                    engine.outboundEvents.first { it is A2UIAction.OpenSpotDetail }
                }
            }
            delay(50)
            openDetailFn.execute(mapOf("spotId" to "spot-berlin-01"), dummyContext)
            val detailAction = detailDeferred.await() as A2UIAction.OpenSpotDetail
            assertEquals("spot-berlin-01", detailAction.spotId)
        }
    }

    @Test
    fun engine_processesAllSixValidFixturesAndAliasWithoutErrors() = runTest {
        withContext(Dispatchers.Default) {
            val engine = JetpackA2UIEngine()
            val errors = mutableListOf<A2UIAction.ClientError>()
            val errorCollector = launch {
                engine.outboundEvents.collect { action ->
                    if (action is A2UIAction.ClientError) {
                        errors.add(action)
                    }
                }
            }

            val fixtures = listOf(
                "spot-surface-1" to """
                    [
                      {"version":"v0.9","createSurface":{"surfaceId":"spot-surface-1","catalogId":"net.maiatoday.tagspotter.hybrid"}},
                      {"version":"v0.9","updateDataModel":{"surfaceId":"spot-surface-1","path":"/spot","value":{"title":"Cosmic Chameleon","artist":"AeroFlux","imageUrl":"https://picsum.photos/seed/teufelsberg/800/500","isStarred":false,"isSaved":false}}},
                      {"version":"v0.9","updateComponents":{"surfaceId":"spot-surface-1","components":[
                        {"id":"root","type":"SpotCard","properties":{"spotId":"spot-berlin-teufelsberg-01","title":{"path":"/spot/title"},"artist":{"path":"/spot/artist"},"imageUrl":{"path":"/spot/imageUrl"},"tags":["stencil","glow"],"distanceMeters":180.0,"latitude":52.4972,"longitude":13.2415,"isStarred":{"path":"/spot/isStarred"},"isSaved":{"path":"/spot/isSaved"}}}
                      ]}}
                    ]
                """.trimIndent(),
                "tour-surface-1" to """
                    [
                      {"version":"v0.9","createSurface":{"surfaceId":"tour-surface-1","catalogId":"$JETPACK_HYBRID_CATALOG_URI"}},
                      {"version":"v0.9","updateDataModel":{"surfaceId":"tour-surface-1","path":"/tour","value":{"title":"Mitte Hidden Alleyways Tour","description":"Discover 4 murals.","estimatedMinutes":45,"totalDistanceKm":2.1}}},
                      {"version":"v0.9","updateComponents":{"surfaceId":"tour-surface-1","components":[
                        {"id":"root","type":"TourCard","properties":{"tourTitle":{"path":"/tour/title"},"description":{"path":"/tour/description"},"estimatedMinutes":{"path":"/tour/estimatedMinutes"},"totalDistanceKm":{"path":"/tour/totalDistanceKm"},"waypoints":[{"index":1,"title":"The Cyber Samurai","address":"Rosenthaler Str. 39","latitude":52.5251,"longitude":13.4021,"completed":true}]}}
                      ]}}
                    ]
                """.trimIndent(),
                "artist-surface-1" to """
                    [
                      {"version":"v0.9","createSurface":{"surfaceId":"artist-surface-1","catalogId":"net.maiatoday.tagspotter.hybrid"}},
                      {"version":"v0.9","updateDataModel":{"surfaceId":"artist-surface-1","path":"/artist","value":{"artistName":"Kira 'Volt' Jensen","avatarUrl":"https://picsum.photos/seed/volt/200/200","bio":"Pioneering Berlin street artist.","activeYears":"2018 - Present"}}},
                      {"version":"v0.9","updateComponents":{"surfaceId":"artist-surface-1","components":[
                        {"id":"root","type":"ArtistBioCard","properties":{"artistName":{"path":"/artist/artistName"},"avatarUrl":{"path":"/artist/avatarUrl"},"bio":{"path":"/artist/bio"},"activeYears":{"path":"/artist/activeYears"},"styles":["UV-Reactive","Stencil"],"artworks":[{"id":"spot-volt-01","title":"Subway Sentinel","imageUrl":"https://picsum.photos/seed/volt1/300/300"}]}}
                      ]}}
                    ]
                """.trimIndent(),
                "quiz-surface-1" to """
                    [
                      {"version":"v0.9","createSurface":{"surfaceId":"quiz-surface-1","catalogId":"net.maiatoday.tagspotter.hybrid"}},
                      {"version":"v0.9","updateComponents":{"surfaceId":"quiz-surface-1","components":[
                        {"id":"root","type":"Card","properties":{"child":"col1"}},
                        {"id":"col1","type":"Column","properties":{"children":["q_title","q_text","picker","submit_btn"]}},
                        {"id":"q_title","type":"Text","properties":{"text":"Style Quiz: Street Art Techniques","variant":"h3"}},
                        {"id":"q_text","type":"Text","properties":{"text":"Which visual hallmark identifies a Wheatpaste?","variant":"body"}},
                        {"id":"picker","type":"ChoicePicker","properties":{"options":[{"value":"opt-1","label":"Paper wrinkling"},{"value":"opt-2","label":"Overspray halo"}],"value":{"path":"/selectedOption"}}},
                        {"id":"submit_btn","type":"Button","properties":{"child":"btn_label","action":{"event":{"name":"answer_quiz","context":{"quizId":"quiz-berlin-styles-01","selectedOption":{"path":"/selectedOption"}}}}}},
                        {"id":"btn_label","type":"Text","properties":{"text":"Submit Answer"}}
                      ]}}
                    ]
                """.trimIndent(),
                "voting-surface-1" to """
                    [
                      {"version":"v0.9","createSurface":{"surfaceId":"voting-surface-1","catalogId":"net.maiatoday.tagspotter.hybrid"}},
                      {"version":"v0.9","updateComponents":{"surfaceId":"voting-surface-1","components":[
                        {"id":"root","type":"Card","properties":{"child":"col1"}},
                        {"id":"col1","type":"Column","properties":{"children":["v_title","v_desc","v_picker","v_btn"]}},
                        {"id":"v_title","type":"Text","properties":{"text":"Community Wall Preservation","variant":"H3"}},
                        {"id":"v_desc","type":"Text","properties":{"text":"Should the Phoenix Wall be sealed?","variant":"Body"}},
                        {"id":"v_picker","type":"ChoicePicker","properties":{"options":[{"id":"preserve","label":"Seal it"},{"id":"keep-open","label":"Keep open"}],"value":{"path":"/selectedOption"}}},
                        {"id":"v_btn","type":"Button","properties":{"child":"v_btn_label","action":{"functionCall":{"call":"clientEvent","args":{"name":"vote_option","pollId":"poll-wall-curation-01","selectedOption":{"path":"/selectedOption"}}}}}},
                        {"id":"v_btn_label","type":"Text","properties":{"text":"Vote Now"}}
                      ]}}
                    ]
                """.trimIndent(),
                "map-surface-1" to """
                    [
                      {"version":"v0.9","createSurface":{"surfaceId":"map-surface-1","catalogId":"net.maiatoday.tagspotter.hybrid"}},
                      {"version":"v0.9","updateComponents":{"surfaceId":"map-surface-1","components":[
                        {"id":"root","type":"MapPinOverlay","properties":{"title":"Kreuzberg Neon Cluster","centerLatitude":52.4990,"centerLongitude":13.4180,"zoomLevel":15.0,"pins":[{"id":"pin-kbg-01","title":"Astronaut Cosmonaut","latitude":52.5015,"longitude":13.4190,"category":"Mural"}]}}
                      ]}}
                    ]
                """.trimIndent(),
                "map-alias-surface-1" to """
                    [
                      {"version":"v0.9","createSurface":{"surfaceId":"map-alias-surface-1","catalogId":"net.maiatoday.tagspotter.hybrid"}},
                      {"version":"v0.9","updateComponents":{"surfaceId":"map-alias-surface-1","components":[
                        {"id":"root","type":"MapPinOverlayWidget","properties":{"title":"Alias Cluster","centerLatitude":52.4990,"centerLongitude":13.4180,"zoomLevel":15.0,"pins":[{"id":"pin-1","title":"Pin 1","latitude":52.5015,"longitude":13.4190,"category":"Mural"}]}}
                      ]}}
                    ]
                """.trimIndent()
            )

            for ((surfaceId, payload) in fixtures) {
                val affected = engine.processProtocolMessage(payload)
                assertTrue(affected.contains(surfaceId), "Expected affected to contain $surfaceId")
                val surfaces = withTimeout(5000) {
                    engine.processor.activeSurfaces.first { list -> list.any { it.id == surfaceId } }
                }
                val surface = surfaces.find { it.id == surfaceId }
                assertNotNull(surface, "Surface $surfaceId should be active")
            }

            delay(150)
            errorCollector.cancel()
            assertTrue(errors.isEmpty(), "Expected zero ClientErrors across all 6 valid fixtures + alias, got: $errors")
        }
    }

    @Test
    fun engine_processesStandardA2UIEnvelope() = runTest {
        withContext(Dispatchers.Default) {
            val engine = JetpackA2UIEngine()
            val payload = """
                [
                  {
                    "createSurface": {
                      "surfaceId": "surface-android-1"
                    }
                  },
                  {
                    "updateDataModel": {
                      "surfaceId": "surface-android-1",
                      "data": {
                        "artist": "Banksy",
                        "count": 42
                      }
                    }
                  },
                  {
                    "updateComponents": {
                      "surfaceId": "surface-android-1",
                      "components": [
                        {
                          "id": "root",
                          "type": "SpotCard",
                          "properties": {
                            "title": "Girl with Balloon",
                            "latitude": -33.9,
                            "longitude": 18.4
                          }
                        }
                      ]
                    }
                  }
                ]
            """.trimIndent()

            val affected = engine.processProtocolMessage(payload)
            assertTrue(affected.contains("surface-android-1"))
            val surfaces = withTimeout(5000) {
                engine.processor.activeSurfaces.first { list -> list.any { it.id == "surface-android-1" } }
            }
            val surface = surfaces.find { it.id == "surface-android-1" }
            assertNotNull(surface)
            engine.clearSurfaces()
            withTimeout(5000) {
                engine.processor.activeSurfaces.first { it.isEmpty() }
            }
        }
    }

    @Test
    fun engine_updateDataModel_invokedDirectly() {
        val engine = JetpackA2UIEngine()
        engine.updateDataModel("surface-1", "/artist", "Faith47")
    }

    @Test
    fun engine_processesSpotDossierPayloadWithNestedProperties() = runTest {
        withContext(Dispatchers.Default) {
            val engine = JetpackA2UIEngine()
            val payload = """
                [
                  {
                    "version": "v0.9",
                    "createSurface": {
                      "surfaceId": "spot-surface-1",
                      "catalogId": "net.maiatoday.tagspotter.hybrid"
                    }
                  },
                  {
                    "version": "v0.9",
                    "updateDataModel": {
                      "surfaceId": "spot-surface-1",
                      "path": "/spot",
                      "value": {
                        "title": "Cosmic Chameleon",
                        "artist": "AeroFlux",
                        "imageUrl": "https://picsum.photos/seed/teufelsberg/800/500",
                        "isStarred": false
                      }
                    }
                  },
                  {
                    "version": "v0.9",
                    "updateComponents": {
                      "surfaceId": "spot-surface-1",
                      "components": [
                        {
                          "id": "root",
                          "type": "SpotCard",
                          "properties": {
                            "spotId": "spot-berlin-teufelsberg-01",
                            "title": {"path": "/spot/title"},
                            "artist": {"path": "/spot/artist"},
                            "imageUrl": {"path": "/spot/imageUrl"},
                            "tags": ["stencil", "glow"],
                            "distanceMeters": 180.0,
                            "latitude": 52.4972,
                            "longitude": 13.2415,
                            "isStarred": {"path": "/spot/isStarred"}
                          }
                        }
                      ]
                    }
                  }
                ]
            """.trimIndent()

            val affected = engine.processProtocolMessage(payload)
            assertTrue(affected.contains("spot-surface-1"))
            val surfaces = withTimeout(5000) {
                engine.processor.activeSurfaces.first { list -> list.any { it.id == "spot-surface-1" } }
            }
            val surface = surfaces.find { it.id == "spot-surface-1" }
            assertNotNull(surface)
        }
    }
}
