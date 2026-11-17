package net.maiatoday.tagspotter.core.a2ui

import androidx.a2ui.compose.ui.toJsonSchemaString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import net.maiatoday.tagspotter.core.a2ui.engine.JetpackA2UIEngine
import net.maiatoday.tagspotter.core.a2ui.engine.createJetpackHybridCatalog
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
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
        assertTrue(schemaJson.contains("TourCard"), "Schema should declare TourCard")
        assertTrue(schemaJson.contains("ArtistBioCard"), "Schema should declare ArtistBioCard")
        assertTrue(schemaJson.contains("MapPinOverlay"), "Schema should declare MapPinOverlay")
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
