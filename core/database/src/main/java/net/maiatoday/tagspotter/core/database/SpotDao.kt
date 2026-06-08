package net.maiatoday.tagspotter.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SpotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpot(spot: SpotEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: SpotImageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: SpotNoteEntity): Long

    @Update
    suspend fun updateSpot(spot: SpotEntity)

    @Delete
    suspend fun deleteSpot(spot: SpotEntity)

    @Query("DELETE FROM spots WHERE id = :spotId")
    suspend fun deleteSpotById(spotId: Long)

    @Query("UPDATE spots SET status = :status WHERE id = :spotId")
    suspend fun updateSpotStatus(spotId: Long, status: String)

    @Query("UPDATE spots SET category = :category WHERE id = :spotId")
    suspend fun updateSpotCategory(spotId: Long, category: String)

    @Query("UPDATE spots SET artists = :artists WHERE id = :spotId")
    suspend fun updateSpotArtists(spotId: Long, artists: List<String>)

    @Query("UPDATE spots SET photographer = :photographer WHERE id = :spotId")
    suspend fun updateSpotPhotographer(spotId: Long, photographer: String)

    @Query("UPDATE spots SET tags = :tags WHERE id = :spotId")
    suspend fun updateSpotTags(spotId: Long, tags: List<String>)

    @Query("UPDATE spots SET latitude = :latitude, longitude = :longitude WHERE id = :spotId")
    suspend fun updateSpotLocation(spotId: Long, latitude: Double, longitude: Double)

    @Query("UPDATE spots SET description = :description WHERE id = :spotId")
    suspend fun updateSpotDescription(spotId: Long, description: String)

    @Transaction
    @Query("SELECT * FROM spots WHERE id = :id")
    fun getSpotDetails(id: Long): Flow<SpotDetailsEntity?>

    @Transaction
    @Query("SELECT * FROM spots ORDER BY createdAt DESC")
    fun getAllSpotsDetails(): Flow<List<SpotDetailsEntity>>

    @Transaction
    @Query("SELECT * FROM spots WHERE category = :category ORDER BY createdAt DESC")
    fun getAllSpotsDetailsByCategory(category: String): Flow<List<SpotDetailsEntity>>

    @Query("SELECT DISTINCT tags FROM spots")
    fun getAllUsedTags(): Flow<List<String>>

    @Query("UPDATE spots SET isStarred = :isStarred WHERE id = :spotId")
    suspend fun updateSpotStarred(spotId: Long, isStarred: Boolean)

    @Query("SELECT * FROM spots WHERE isStarred = 1")
    suspend fun getStarredSpots(): List<SpotEntity>

    @Query("SELECT COUNT(*) FROM spots WHERE isStarred = 1")
    suspend fun getStarredSpotsCount(): Int

    @Query("UPDATE spot_images SET isMain = 0 WHERE spotId = :spotId")
    suspend fun clearMainImages(spotId: Long)

    @Query("UPDATE spot_images SET isMain = 1 WHERE id = :imageId")
    suspend fun setMainImageId(imageId: Long)

    @Transaction
    suspend fun setMainImage(spotId: Long, imageId: Long) {
        clearMainImages(spotId)
        setMainImageId(imageId)
    }

    @Query("DELETE FROM spot_images WHERE id = :imageId")
    suspend fun deleteImageById(imageId: Long)

    @Query("SELECT * FROM spot_images WHERE spotId = :spotId ORDER BY timestamp ASC")
    suspend fun getImagesForSpot(spotId: Long): List<SpotImageEntity>

    @Query("UPDATE spot_images SET rating = :rating WHERE id = :imageId")
    suspend fun updateImageRating(imageId: Long, rating: Int)

    @Query("UPDATE spots SET artworkDate = :artworkDate WHERE id = :spotId")
    suspend fun updateSpotArtworkDate(spotId: Long, artworkDate: String)

    @Query("DELETE FROM spot_notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Long)

    @Query("UPDATE spot_notes SET noteText = :noteText WHERE id = :noteId")
    suspend fun updateNoteText(noteId: Long, noteText: String)
}
