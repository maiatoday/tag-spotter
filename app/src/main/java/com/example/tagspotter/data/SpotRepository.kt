package com.example.tagspotter.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

interface SpotRepository {
    fun getAllSpots(): Flow<List<SpotDetails>>
    fun getSpotsByCategory(category: String): Flow<List<SpotDetails>>
    fun getSpotById(id: Long): Flow<SpotDetails?>
    suspend fun saveSpot(spot: Spot, imagePath: String): Long
    suspend fun addImageToSpot(spotId: Long, imagePath: String, timestamp: Long): Long
    suspend fun addNoteToSpot(spotId: Long, noteText: String, timestamp: Long): Long
    suspend fun updateSpotStatus(spotId: Long, status: String)
    suspend fun updateSpotArtists(spotId: Long, artists: List<String>)
    suspend fun updateSpotPhotographer(spotId: Long, photographer: String)
    suspend fun updateSpotLocation(spotId: Long, latitude: Double, longitude: Double)
    suspend fun updateSpotDescription(spotId: Long, description: String)
    suspend fun deleteSpot(spotDetails: SpotDetails)
    fun getRecentCustomTags(predefinedTags: Set<String>): Flow<List<String>>
}

class LocalSpotRepository(private val spotDao: SpotDao) : SpotRepository {

    override fun getAllSpots(): Flow<List<SpotDetails>> {
        return spotDao.getAllSpotsDetails()
    }

    override fun getSpotsByCategory(category: String): Flow<List<SpotDetails>> {
        if (category == "All") {
            return spotDao.getAllSpotsDetails()
        }
        return spotDao.getAllSpotsDetailsByCategory(category)
    }

    override fun getSpotById(id: Long): Flow<SpotDetails?> {
        return spotDao.getSpotDetails(id)
    }

    override suspend fun saveSpot(spot: Spot, imagePath: String): Long {
        val spotId = spotDao.insertSpot(spot)
        spotDao.insertImage(
            SpotImage(
                spotId = spotId,
                imagePath = imagePath,
                timestamp = spot.createdAt
            )
        )
        return spotId
    }

    override suspend fun addImageToSpot(spotId: Long, imagePath: String, timestamp: Long): Long {
        return spotDao.insertImage(
            SpotImage(
                spotId = spotId,
                imagePath = imagePath,
                timestamp = timestamp
            )
        )
    }

    override suspend fun addNoteToSpot(spotId: Long, noteText: String, timestamp: Long): Long {
        return spotDao.insertNote(
            SpotNote(
                spotId = spotId,
                noteText = noteText,
                timestamp = timestamp
            )
        )
    }

    override suspend fun updateSpotStatus(spotId: Long, status: String) {
        spotDao.updateSpotStatus(spotId, status)
    }

    override suspend fun updateSpotArtists(spotId: Long, artists: List<String>) {
        spotDao.updateSpotArtists(spotId, artists)
    }

    override suspend fun updateSpotPhotographer(spotId: Long, photographer: String) {
        spotDao.updateSpotPhotographer(spotId, photographer)
    }

    override suspend fun updateSpotLocation(spotId: Long, latitude: Double, longitude: Double) {
        spotDao.updateSpotLocation(spotId, latitude, longitude)
    }

    override suspend fun updateSpotDescription(spotId: Long, description: String) {
        spotDao.updateSpotDescription(spotId, description)
    }

    override suspend fun deleteSpot(spotDetails: SpotDetails) {
        // Delete all local image files
        spotDetails.images.forEach { image ->
            try {
                val file = File(image.imagePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // Room cascading delete will clean the images and notes in the database
        spotDao.deleteSpotById(spotDetails.spot.id)
    }

    override fun getRecentCustomTags(predefinedTags: Set<String>): Flow<List<String>> {
        return spotDao.getAllUsedTags().map { rawTagsList ->
            rawTagsList.flatMap { rawTags ->
                if (rawTags.isEmpty()) emptyList() else rawTags.split(",")
            }
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() && !predefinedTags.contains(it) }
                .distinct()
                .take(15) // Limit to top 15 suggestions
        }
    }
}
