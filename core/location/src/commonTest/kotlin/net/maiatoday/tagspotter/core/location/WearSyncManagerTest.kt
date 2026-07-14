package net.maiatoday.tagspotter.core.location

import net.maiatoday.tagspotter.core.model.Spot
import net.maiatoday.tagspotter.core.model.SpotDetails
import kotlin.test.Test
import kotlin.test.assertNotNull

class WearSyncManagerTest {

    @Test
    fun testNoOpWearSyncManagerExecutesSuccessfully() {
        val manager = NoOpWearSyncManager()
        val spot = Spot(
            id = 123L,
            latitude = 45.4642,
            longitude = 9.1900,
            createdAt = 1000L,
            description = "Test spot",
            tags = emptyList(),
            category = "all",
            status = "active",
            artists = emptyList(),
            photographer = "photographer"
        )
        val spotDetails = SpotDetails(spot, emptyList(), emptyList())

        // Ensure functions don't crash
        manager.shareSpotToWatch(spotDetails)
        manager.sendSpotPhoto(123L, "some_path")

        assertNotNull(manager)
    }
}
