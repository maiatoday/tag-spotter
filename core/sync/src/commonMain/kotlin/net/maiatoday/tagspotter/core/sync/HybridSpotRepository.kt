package net.maiatoday.tagspotter.core.sync

import kotlinx.coroutines.flow.first
import net.maiatoday.tagspotter.core.database.SpotRepository
import net.maiatoday.tagspotter.core.model.Spot
import net.maiatoday.tagspotter.core.model.SpotDetails
import kotlin.math.*

class HybridSpotRepository(
    private val localRepository: SpotRepository,
    private val syncManager: SyncManager
) {

    /**
     * Fetches spots within a given radius in kilometers from the given coordinate,
     * merging local spots with remote public spots and deduplicating by UUID.
     * Prioritizes local image paths when a spot exists in both local and remote.
     */
    suspend fun getNearbySpots(
        latitude: Double,
        longitude: Double,
        radiusKm: Double = 5.0
    ): List<SpotDetails> {
        val deltaLat = radiusKm / 111.0
        val cosLat = max(0.01, cos(latitude.toRadians()))
        val deltaLon = radiusKm / (111.0 * cosLat)

        val minLat = latitude - deltaLat
        val maxLat = latitude + deltaLat
        val minLon = longitude - deltaLon
        val maxLon = longitude + deltaLon

        // 1. Fetch local spots
        val localSpots = localRepository.getAllSpots().first()

        // 2. Fetch remote public spots within bounding box
        val remoteSpots = try {
            syncManager.fetchPublicSpots(minLat, maxLat, minLon, maxLon)
        } catch (e: Exception) {
            emptyList()
        }

        // 3. Deduplicate and merge
        val mergedSpots = mergeSpots(localSpots, remoteSpots)

        // 4. Filter by radius and sort by distance
        return mergedSpots.mapNotNull { detail ->
            val dist = calculateDistanceKm(latitude, longitude, detail.spot.latitude, detail.spot.longitude)
            if (dist <= radiusKm) {
                detail to dist
            } else {
                null
            }
        }.sortedBy { it.second }.map { it.first }
    }

    /**
     * Returns all merged spots (local + public).
     */
    suspend fun getAllSpots(): List<SpotDetails> {
        val localSpots = localRepository.getAllSpots().first()
        val remoteSpots = try {
            syncManager.fetchPublicSpots()
        } catch (e: Exception) {
            emptyList()
        }
        return mergeSpots(localSpots, remoteSpots)
    }

    /**
     * Looks up a spot by its UUID, checking local database first, then remote public spots.
     */
    suspend fun getSpotByUuid(uuid: String): SpotDetails? {
        val local = localRepository.getAllSpots().first().find { it.spot.uuid == uuid }
        if (local != null) return local
        val remote = syncManager.fetchPublicSpots()
        return remote.find { it.spot.uuid == uuid }
    }

    /**
     * Saves a spot to the local repository with optional image and thumbnail paths.
     */
    suspend fun saveSpot(
        spot: Spot,
        imagePath: String = "",
        thumbnailPath: String = ""
    ): Long {
        return localRepository.saveSpot(spot, imagePath, thumbnailPath)
    }

    /**
     * Toggles the starred status of a spot by UUID.
     * Looks up the spot locally first. If not found locally, searches remote public spots and imports it.
     * Returns the new isStarred boolean value.
     */
    suspend fun toggleSpotStar(uuid: String): Boolean {
        val localSpots = localRepository.getAllSpots().first()
        val local = localSpots.find { it.spot.uuid == uuid || it.spot.id.toString() == uuid }
        if (local != null) {
            val newStar = !local.spot.isStarred
            localRepository.updateSpotStarred(local.spot.id, newStar)
            return newStar
        }

        val remote = try {
            syncManager.fetchPublicSpots().find { it.spot.uuid == uuid || it.spot.id.toString() == uuid }
        } catch (e: Exception) {
            null
        }

        if (remote != null) {
            localRepository.saveImportedSpot(remote.copy(spot = remote.spot.copy(isImported = true)))
            val imported = localRepository.getAllSpots().first().find { it.spot.uuid == uuid || it.spot.id.toString() == uuid }
            val newStar = !remote.spot.isStarred
            if (imported != null) {
                localRepository.updateSpotStarred(imported.spot.id, newStar)
            }
            return newStar
        }

        return false
    }

    private fun mergeSpots(
        localSpots: List<SpotDetails>,
        remoteSpots: List<SpotDetails>
    ): List<SpotDetails> {
        val localMap = localSpots.associateBy { it.spot.uuid }
        val merged = mutableListOf<SpotDetails>()

        // Add merged local spots, prioritizing local image paths
        localSpots.forEach { local ->
            val remote = remoteSpots.find { it.spot.uuid == local.spot.uuid }
            if (remote != null) {
                val mergedImages = local.images.map { localImg ->
                    val remoteImg = remote.images.find { it.uuid == localImg.uuid }
                    if (remoteImg != null && localImg.thumbnailPath.isEmpty() && remoteImg.thumbnailPath.isNotEmpty()) {
                        localImg.copy(thumbnailPath = remoteImg.thumbnailPath)
                    } else {
                        localImg
                    }
                }
                merged.add(local.copy(images = mergedImages))
            } else {
                merged.add(local)
            }
        }

        // Add remote spots that aren't in local
        remoteSpots.forEach { remote ->
            if (!localMap.containsKey(remote.spot.uuid)) {
                merged.add(remote)
            }
        }

        return merged
    }

    companion object {
        fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val earthRadiusKm = 6371.0
            val dLat = (lat2 - lat1).toRadians()
            val dLon = (lon2 - lon1).toRadians()
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(lat1.toRadians()) * cos(lat2.toRadians()) *
                    sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return earthRadiusKm * c
        }

        private fun Double.toRadians(): Double = this * PI / 180.0
    }
}
