package net.maiatoday.tagspotter.core.location

import net.maiatoday.tagspotter.core.model.Spot
import net.maiatoday.tagspotter.core.model.SpotDetails
import kotlin.test.Test
import kotlin.test.assertEquals

class NoOpWearSyncManagerTest {

    @Test
    fun testNoOpWearSyncManagerCallsDoNotThrow() {
        val manager = NoOpWearSyncManager()
        val spot = Spot(1L, 10.0, 20.0, 1000L, "desc", emptyList(), "graffiti", "active")
        val spotDetails = SpotDetails(spot, emptyList(), emptyList())

        // Ensure calls execute without exceptions
        manager.shareSpotToWatch(spotDetails)
        manager.sendSpotPhoto(1L, "/path/photo.jpg")
    }

    @Test
    fun testLocationDataProperties() {
        val loc = LocationData(45.0, 9.0, isFallback = true)
        assertEquals(45.0, loc.latitude)
        assertEquals(9.0, loc.longitude)
        assertEquals(true, loc.isFallback)
    }
}
