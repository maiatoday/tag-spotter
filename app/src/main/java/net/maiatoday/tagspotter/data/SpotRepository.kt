package net.maiatoday.tagspotter.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.io.File
import android.content.Context
import net.maiatoday.tagspotter.domain.GeofenceService

interface SpotRepository {
    fun getAllSpots(): Flow<List<SpotDetails>>
    fun getSpotsByCategory(category: String): Flow<List<SpotDetails>>
    fun getSpotById(id: Long): Flow<SpotDetails?>
    suspend fun saveSpot(spot: Spot, imagePath: String, thumbnailPath: String, rating: Int = 0): Long
    suspend fun addImageToSpot(spotId: Long, imagePath: String, thumbnailPath: String, timestamp: Long, rating: Int = 0): Long
    suspend fun addNoteToSpot(spotId: Long, noteText: String, timestamp: Long): Long
    suspend fun updateSpotStatus(spotId: Long, status: String)
    suspend fun updateSpotCategory(spotId: Long, category: String)
    suspend fun updateSpotArtists(spotId: Long, artists: List<String>)
    suspend fun updateSpotPhotographer(spotId: Long, photographer: String)
    suspend fun updateSpotTags(spotId: Long, tags: List<String>)
    suspend fun updateSpotLocation(spotId: Long, latitude: Double, longitude: Double)
    suspend fun updateSpotDescription(spotId: Long, description: String)
    suspend fun deleteSpot(spotDetails: SpotDetails)
    fun getRecentCustomTags(predefinedTags: Set<String>): Flow<List<String>>
    suspend fun loadTestData()
    suspend fun unloadTestData()
    suspend fun importSpots(spots: List<SpotDetails>): Int
    suspend fun updateSpotStarred(spotId: Long, isStarred: Boolean)
    suspend fun getStarredSpots(): List<Spot>
    suspend fun getStarredSpotsCount(): Int
    suspend fun setMainImage(spotId: Long, imageId: Long)
    suspend fun deleteImage(image: SpotImage)
    suspend fun updateImageRating(imageId: Long, rating: Int)
    suspend fun updateSpotArtworkDate(spotId: Long, artworkDate: String)
    suspend fun deleteNote(noteId: Long)
}

class LocalSpotRepository(
    private val context: Context,
    private val spotDao: SpotDao,
    private val geofenceService: GeofenceService
) : SpotRepository {



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

    override suspend fun saveSpot(spot: Spot, imagePath: String, thumbnailPath: String, rating: Int): Long {
        val spotId = spotDao.insertSpot(spot)
        if (imagePath.isNotEmpty()) {
            spotDao.insertImage(
                SpotImage(
                    spotId = spotId,
                    imagePath = imagePath,
                    thumbnailPath = thumbnailPath,
                    timestamp = spot.createdAt,
                    rating = rating
                )
            )
        }
        return spotId
    }

    override suspend fun addImageToSpot(spotId: Long, imagePath: String, thumbnailPath: String, timestamp: Long, rating: Int): Long {
        return spotDao.insertImage(
            SpotImage(
                spotId = spotId,
                imagePath = imagePath,
                thumbnailPath = thumbnailPath,
                timestamp = timestamp,
                rating = rating
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

    override suspend fun updateSpotCategory(spotId: Long, category: String) {
        spotDao.updateSpotCategory(spotId, category)
    }

    override suspend fun updateSpotArtists(spotId: Long, artists: List<String>) {
        spotDao.updateSpotArtists(spotId, artists)
    }

    override suspend fun updateSpotPhotographer(spotId: Long, photographer: String) {
        spotDao.updateSpotPhotographer(spotId, photographer)
    }

    override suspend fun updateSpotTags(spotId: Long, tags: List<String>) {
        spotDao.updateSpotTags(spotId, tags)
    }

    override suspend fun updateSpotLocation(spotId: Long, latitude: Double, longitude: Double) {
        spotDao.updateSpotLocation(spotId, latitude, longitude)
    }

    override suspend fun updateSpotDescription(spotId: Long, description: String) {
        spotDao.updateSpotDescription(spotId, description)
    }

    override suspend fun deleteSpot(spotDetails: SpotDetails) {
        // If starred, unregister geofence
        if (spotDetails.spot.isStarred) {
            geofenceService.unregisterGeofence(spotDetails.spot.id)
        }
        // Delete all local thumbnail and image files (original public gallery photos are NOT deleted)
        spotDetails.images.forEach { image ->
            try {
                if (image.thumbnailPath.isNotEmpty()) {
                    val file = File(image.thumbnailPath)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                if (image.imagePath.isNotEmpty()) {
                    val file = File(image.imagePath)
                    // Only delete local private files, not content:// URIs or resource paths
                    if (file.exists() &&
                        !image.imagePath.startsWith("content://") &&
                        !image.imagePath.startsWith("android.resource://") &&
                        !image.imagePath.startsWith("http")
                    ) {
                        file.delete()
                    }
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
            rawTagsList.asSequence().flatMap { rawTags ->
                Converters().toStringList(rawTags)
            }
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() && !predefinedTags.contains(it) }
                .distinct()
                .take(15).toList() // Limit to top 15 suggestions
        }
    }

    override suspend fun loadTestData() {
        val now = System.currentTimeMillis()
        val spot1 = Spot(
            id = 9001L,
            latitude = 45.4642,
            longitude = 9.1899,
            createdAt = now - 86400000 * 2, // 2 days ago
            description = "Stunning street art stencil near the Duomo in Milan.",
            tags = listOf("milan", "stencil", "duomo"),
            category = "graffiti",
            status = "active",
            artists = listOf("Mr. Brainwash"),
            photographer = "Mock Photographer"
        )
        val image1 = SpotImage(
            id = 9001L,
            spotId = 9001L,
            imagePath = "android.resource://net.maiatoday.tagspotter/drawable/ic_launcher_foreground",
            thumbnailPath = "android.resource://net.maiatoday.tagspotter/drawable/ic_launcher_foreground",
            timestamp = now - 86400000 * 2
        )

        val spot2 = Spot(
            id = 9002L,
            latitude = 51.5074,
            longitude = -0.1278,
            createdAt = now - 86400000 * 1, // 1 day ago
            description = "Statue of a famous historical figure in central London.",
            tags = listOf("london", "statue", "history"),
            category = "sculpture",
            status = "active",
            artists = listOf("Famous Sculptor"),
            photographer = "Mock Photographer"
        )
        val image2 = SpotImage(
            id = 9002L,
            spotId = 9002L,
            imagePath = "android.resource://net.maiatoday.tagspotter/drawable/ic_launcher_foreground",
            thumbnailPath = "android.resource://net.maiatoday.tagspotter/drawable/ic_launcher_foreground",
            timestamp = now - 86400000 * 1
        )

        val spot3 = Spot(
            id = 9003L,
            latitude = 40.7128,
            longitude = -74.0060,
            createdAt = now,
            description = "Modern architectural masterpiece in New York City.",
            tags = listOf("nyc", "modern", "design"),
            category = "architecture",
            status = "active",
            artists = listOf("Star Architect"),
            photographer = "Mock Photographer"
        )
        val image3 = SpotImage(
            id = 9003L,
            spotId = 9003L,
            imagePath = "android.resource://net.maiatoday.tagspotter/drawable/ic_launcher_foreground",
            thumbnailPath = "android.resource://net.maiatoday.tagspotter/drawable/ic_launcher_foreground",
            timestamp = now
        )

        spotDao.insertSpot(spot1)
        spotDao.insertImage(image1)
        spotDao.insertSpot(spot2)
        spotDao.insertImage(image2)
        spotDao.insertSpot(spot3)
        spotDao.insertImage(image3)
    }

    override suspend fun unloadTestData() {
        MOCK_SPOT_IDS.forEach { id ->
            spotDao.deleteSpotById(id)
        }
    }

    override suspend fun importSpots(spots: List<SpotDetails>): Int {
        val existingSpots = spotDao.getAllSpotsDetails().first()
        var importedCount = 0
        spots.forEach { importedDetail ->
            val importedSpot = importedDetail.spot
            val isDuplicate = existingSpots.any { existingDetail ->
                val e = existingDetail.spot
                e.createdAt == importedSpot.createdAt &&
                        e.latitude == importedSpot.latitude &&
                        e.longitude == importedSpot.longitude
            }
            if (!isDuplicate) {
                val newSpotId = spotDao.insertSpot(importedSpot.copy(id = 0L, isImported = true))
                importedDetail.images.forEach { image ->
                    spotDao.insertImage(image.copy(id = 0L, spotId = newSpotId))
                }
                importedDetail.notes.forEach { note ->
                    spotDao.insertNote(note.copy(id = 0L, spotId = newSpotId))
                }
                importedCount++
            }
        }
        return importedCount
    }

    override suspend fun updateSpotStarred(spotId: Long, isStarred: Boolean) {
        spotDao.updateSpotStarred(spotId, isStarred)
        if (isStarred) {
            val details = spotDao.getSpotDetails(spotId).first()
            if (details != null) {
                geofenceService.registerGeofence(
                    id = details.spot.id,
                    latitude = details.spot.latitude,
                    longitude = details.spot.longitude
                )
            }
        } else {
            geofenceService.unregisterGeofence(spotId)
        }
    }

    override suspend fun getStarredSpots(): List<Spot> {
        return spotDao.getStarredSpots()
    }

    override suspend fun getStarredSpotsCount(): Int {
        return spotDao.getStarredSpotsCount()
    }

    override suspend fun setMainImage(spotId: Long, imageId: Long) {
        spotDao.setMainImage(spotId, imageId)
    }

    override suspend fun deleteImage(image: SpotImage) {
        try {
            if (image.thumbnailPath.isNotEmpty()) {
                val file = File(image.thumbnailPath)
                if (file.exists()) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            if (image.imagePath.isNotEmpty()) {
                val file = File(image.imagePath)
                if (file.exists() &&
                    !image.imagePath.startsWith("content://") &&
                    !image.imagePath.startsWith("android.resource://") &&
                    !image.imagePath.startsWith("http")
                ) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        spotDao.deleteImageById(image.id)

        if (image.isMain) {
            val remainingImages = spotDao.getImagesForSpot(image.spotId)
            val nextMain = remainingImages.firstOrNull()
            if (nextMain != null) {
                spotDao.setMainImage(image.spotId, nextMain.id)
            }
        }
    }

    override suspend fun updateImageRating(imageId: Long, rating: Int) {
        spotDao.updateImageRating(imageId, rating)
    }

    override suspend fun updateSpotArtworkDate(spotId: Long, artworkDate: String) {
        spotDao.updateSpotArtworkDate(spotId, artworkDate)
    }

    override suspend fun deleteNote(noteId: Long) {
        spotDao.deleteNoteById(noteId)
    }

    companion object {
        val MOCK_SPOT_IDS = listOf(9001L, 9002L, 9003L)
    }
}
