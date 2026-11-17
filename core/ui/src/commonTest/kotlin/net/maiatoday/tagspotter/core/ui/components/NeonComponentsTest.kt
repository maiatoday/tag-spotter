package net.maiatoday.tagspotter.core.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NeonComponentsTest {

    @Test
    fun testNeonTourWaypointModel() {
        val waypoint = NeonTourWaypoint(
            index = 1,
            title = "East Side Gallery",
            address = "Mühlenstraße 73",
            latitude = 52.505,
            longitude = 13.439,
            imageUrl = "https://example.com/esg.jpg",
            completed = true
        )

        assertEquals(1, waypoint.index)
        assertEquals("East Side Gallery", waypoint.title)
        assertEquals("Mühlenstraße 73", waypoint.address)
        assertTrue(waypoint.completed)
    }

    @Test
    fun testNeonArtworkThumbnailModel() {
        val artwork = NeonArtworkThumbnail(
            id = "art_123",
            title = "The Wall",
            imageUrl = "https://example.com/art.jpg"
        )

        assertEquals("art_123", artwork.id)
        assertEquals("The Wall", artwork.title)
    }

    @Test
    fun testNeonMapPinModel() {
        val pin = NeonMapPin(
            id = "pin_1",
            title = "Urban Spree",
            latitude = 52.508,
            longitude = 13.453,
            category = "mural"
        )

        assertEquals("pin_1", pin.id)
        assertEquals("mural", pin.category)
    }

    @Test
    fun testDistanceFormattingLogic() {
        fun formatDistance(meters: Double?): String? {
            if (meters == null) return null
            return if (meters >= 1000) {
                "${(meters / 100.0).toInt() / 10.0} km"
            } else {
                "${meters.toInt()} m"
            }
        }

        assertEquals("450 m", formatDistance(450.0))
        assertEquals("1.2 km", formatDistance(1234.0))
        assertEquals(null, formatDistance(null))
    }
}
