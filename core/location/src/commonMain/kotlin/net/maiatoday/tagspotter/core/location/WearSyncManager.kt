package net.maiatoday.tagspotter.core.location

import net.maiatoday.tagspotter.core.model.SpotDetails

interface WearSyncManager {
    fun shareSpotToWatch(spotDetails: SpotDetails)
    fun sendSpotPhoto(spotId: Long, imagePath: String)
}

class NoOpWearSyncManager : WearSyncManager {
    override fun shareSpotToWatch(spotDetails: SpotDetails) {}
    override fun sendSpotPhoto(spotId: Long, imagePath: String) {}
}
