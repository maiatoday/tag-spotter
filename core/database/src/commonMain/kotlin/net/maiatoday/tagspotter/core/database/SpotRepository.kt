package net.maiatoday.tagspotter.core.database

import kotlinx.coroutines.flow.Flow
import net.maiatoday.tagspotter.core.model.Spot
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.model.SpotImage
import net.maiatoday.tagspotter.core.model.SpotNote

interface SpotRepository {
    fun getAllSpots(): Flow<List<SpotDetails>>
    fun getSpotsByCategory(category: String): Flow<List<SpotDetails>>
    fun getSpotById(id: Long): Flow<SpotDetails?>
    suspend fun saveSpot(spot: Spot, imagePath: String, thumbnailPath: String, rating: Int = 0, isMain: Boolean = false): Long
    suspend fun addImageToSpot(spotId: Long, imagePath: String, thumbnailPath: String, timestamp: Long, rating: Int = 0, isMain: Boolean = false): Long
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
    suspend fun updateNote(noteId: Long, noteText: String)
}
