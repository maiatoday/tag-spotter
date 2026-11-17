package net.maiatoday.tagspotter.core.a2ui

import androidx.a2ui.compose.ui.toJsonSchemaString
import kotlinx.coroutines.test.runTest
import net.maiatoday.tagspotter.core.a2ui.engine.JetpackA2UIEngine
import net.maiatoday.tagspotter.core.a2ui.engine.createJetpackHybridCatalog
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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

        engine.processMessage(payload)
        engine.clearSurfaces()
    }

    @Test
    fun engine_updateDataModel_invokedDirectly() {
        val engine = JetpackA2UIEngine()
        engine.updateDataModel("surface-1", "/artist", "Faith47")
    }
}
