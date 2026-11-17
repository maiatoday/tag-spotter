package net.maiatoday.tagspotter.core.a2ui

import net.maiatoday.tagspotter.core.a2ui.engine.KmpA2UIEngine
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
        println("Surface components: ${surface.components}")
        println("Surface rootComponentId: ${surface.rootComponentId}")

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
}
