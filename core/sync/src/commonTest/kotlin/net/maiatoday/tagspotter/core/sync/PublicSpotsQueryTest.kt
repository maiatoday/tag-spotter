package net.maiatoday.tagspotter.core.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import net.maiatoday.tagspotter.core.model.SharedPack
import net.maiatoday.tagspotter.core.model.Spot
import net.maiatoday.tagspotter.core.model.SpotDetails
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PublicSpotsQueryTest {

    private fun createSpot(uuid: String, lat: Double, lon: Double, isPublic: Boolean): SpotDetails {
        return SpotDetails(
            spot = Spot(
                uuid = uuid,
                latitude = lat,
                longitude = lon,
                createdAt = 1000L,
                description = "Spot $uuid",
                tags = listOf("tag"),
                category = "graffiti",
                status = "active",
                isPublic = isPublic
            ),
            images = emptyList(),
            notes = emptyList()
        )
    }

    private class FakePublicSyncManager(
        private val remoteSpots: List<SpotDetails>
    ) : SyncManager {
        override val isSyncing: StateFlow<Boolean> = MutableStateFlow(false)
        override suspend fun syncNow() {}
        override fun startRealtimeSync(userId: String) {}
        override fun stopRealtimeSync() {}
        override suspend fun deleteSpot(uuid: String) {}
        override suspend fun sharePack(title: String, description: String, authorName: String, spots: List<SpotDetails>): String = "ABC"
        override suspend fun importPackByCode(code: String): SharedPack = SharedPack(code, "", "", "", emptyList())
        override suspend fun saveImportedPack(sharedPack: SharedPack) {}

        override suspend fun fetchPublicSpots(
            minLat: Double,
            maxLat: Double,
            minLon: Double,
            maxLon: Double
        ): List<SpotDetails> {
            return remoteSpots.filter { detail ->
                detail.spot.isPublic &&
                detail.spot.latitude in minLat..maxLat &&
                detail.spot.longitude in minLon..maxLon
            }
        }
    }

    @Test
    fun testFetchPublicSpotsBoundingBoxFiltering() = runTest {
        val spotInBounds = createSpot("1", lat = 45.4642, lon = 9.1900, isPublic = true) // Milan
        val spotOutOfLat = createSpot("2", lat = 48.8566, lon = 2.3522, isPublic = true) // Paris
        val spotOutOfLon = createSpot("3", lat = 45.5000, lon = 15.0000, isPublic = true)
        val spotPrivateInBounds = createSpot("4", lat = 45.4642, lon = 9.1900, isPublic = false)

        val syncManager = FakePublicSyncManager(listOf(spotInBounds, spotOutOfLat, spotOutOfLon, spotPrivateInBounds))

        val results = syncManager.fetchPublicSpots(
            minLat = 45.0,
            maxLat = 46.0,
            minLon = 9.0,
            maxLon = 10.0
        )

        assertEquals(1, results.size)
        assertEquals("1", results.first().spot.uuid)
        assertTrue(results.first().spot.isPublic)
    }

    @Test
    fun testDefaultSyncManagerFetchPublicSpotsReturnsEmpty() = runTest {
        val defaultSyncManager = object : SyncManager {
            override val isSyncing: StateFlow<Boolean> = MutableStateFlow(false)
            override suspend fun syncNow() {}
            override fun startRealtimeSync(userId: String) {}
            override fun stopRealtimeSync() {}
            override suspend fun deleteSpot(uuid: String) {}
            override suspend fun sharePack(title: String, description: String, authorName: String, spots: List<SpotDetails>): String = ""
            override suspend fun importPackByCode(code: String): SharedPack = SharedPack("", "", "", "", emptyList())
            override suspend fun saveImportedPack(sharedPack: SharedPack) {}
        }

        val results = defaultSyncManager.fetchPublicSpots()
        assertTrue(results.isEmpty())
    }
}
