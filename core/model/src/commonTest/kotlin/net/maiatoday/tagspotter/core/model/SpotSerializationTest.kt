package net.maiatoday.tagspotter.core.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpotSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val lenientJson = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun defaultIsPublicIsFalse() {
        val spot = Spot(
            latitude = 52.52,
            longitude = 13.405,
            createdAt = 1000L,
            description = "Berlin Mural",
            tags = listOf("graffiti"),
            category = "graffiti",
            status = "active"
        )
        assertFalse(spot.isPublic)
    }

    @Test
    fun explicitIsPublicIsTrue() {
        val spot = Spot(
            latitude = 52.52,
            longitude = 13.405,
            createdAt = 1000L,
            description = "Berlin Mural",
            tags = listOf("graffiti"),
            category = "graffiti",
            status = "active",
            isPublic = true
        )
        assertTrue(spot.isPublic)
    }

    @Test
    fun copyPreservesAndTogglesIsPublic() {
        val privateSpot = Spot(
            latitude = 52.52,
            longitude = 13.405,
            createdAt = 1000L,
            description = "Berlin Mural",
            tags = listOf("graffiti"),
            category = "graffiti",
            status = "active",
            isPublic = false
        )
        val publicSpot = privateSpot.copy(isPublic = true)
        assertTrue(publicSpot.isPublic)

        val revertedSpot = publicSpot.copy(isPublic = false)
        assertFalse(revertedSpot.isPublic)
    }

    @Test
    fun legacyJsonWithoutIsPublicDeserializesToFalse() {
        // Legacy JSON schema without 'isPublic'
        val legacyJsonString = """
            {
                "id": 1,
                "latitude": 52.52,
                "longitude": 13.405,
                "createdAt": 1000,
                "description": "Legacy Spot",
                "tags": ["stencil"],
                "category": "graffiti",
                "status": "active",
                "artists": [],
                "photographer": "",
                "isImported": false,
                "isStarred": false,
                "artworkDate": "",
                "uuid": "test-uuid-1234",
                "photographerUuid": "",
                "lastEditedAt": 1000,
                "isSynced": false
            }
        """.trimIndent()

        val spot = lenientJson.decodeFromString<Spot>(legacyJsonString)
        assertEquals("Legacy Spot", spot.description)
        assertFalse(spot.isPublic)
    }

    @Test
    fun jsonRoundTripPreservesIsPublicTrueAndFalse() {
        val publicSpot = Spot(
            id = 42,
            latitude = 52.52,
            longitude = 13.405,
            createdAt = 2000L,
            description = "Public Street Art",
            tags = listOf("pasteup"),
            category = "graffiti",
            status = "active",
            uuid = "public-uuid-5678",
            isPublic = true
        )

        val encoded = json.encodeToString(publicSpot)
        val decoded = json.decodeFromString<Spot>(encoded)

        assertTrue(decoded.isPublic)
        assertEquals(publicSpot, decoded)

        val privateSpot = publicSpot.copy(isPublic = false)
        val encodedPrivate = json.encodeToString(privateSpot)
        val decodedPrivate = json.decodeFromString<Spot>(encodedPrivate)

        assertFalse(decodedPrivate.isPublic)
        assertEquals(privateSpot, decodedPrivate)
    }
}
