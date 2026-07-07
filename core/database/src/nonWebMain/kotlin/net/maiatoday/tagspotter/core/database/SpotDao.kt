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

    @Query("UPDATE spots SET lastEditedAt = :lastEdited, isSynced = 0 WHERE id = :spotId")
    suspend fun touchSpot(spotId: Long, lastEdited: Long)

    @Query("UPDATE spots SET status = :status, lastEditedAt = :lastEditedAt, isSynced = 0 WHERE id = :spotId")
    suspend fun updateSpotStatus(spotId: Long, status: String, lastEditedAt: Long)

    @Query("UPDATE spots SET category = :category, lastEditedAt = :lastEditedAt, isSynced = 0 WHERE id = :spotId")
    suspend fun updateSpotCategory(spotId: Long, category: String, lastEditedAt: Long)

    @Query("UPDATE spots SET artists = :artists, lastEditedAt = :lastEditedAt, isSynced = 0 WHERE id = :spotId")
    suspend fun updateSpotArtists(spotId: Long, artists: List<String>, lastEditedAt: Long)

    @Query("UPDATE spots SET photographer = :photographer, lastEditedAt = :lastEditedAt, isSynced = 0 WHERE id = :spotId")
    suspend fun updateSpotPhotographer(spotId: Long, photographer: String, lastEditedAt: Long)

    @Query("UPDATE spots SET tags = :tags, lastEditedAt = :lastEditedAt, isSynced = 0 WHERE id = :spotId")
    suspend fun updateSpotTags(spotId: Long, tags: List<String>, lastEditedAt: Long)

    @Query("UPDATE spots SET latitude = :latitude, longitude = :longitude, lastEditedAt = :lastEditedAt, isSynced = 0 WHERE id = :spotId")
    suspend fun updateSpotLocation(spotId: Long, latitude: Double, longitude: Double, lastEditedAt: Long)

    @Query("UPDATE spots SET description = :description, lastEditedAt = :lastEditedAt, isSynced = 0 WHERE id = :spotId")
    suspend fun updateSpotDescription(spotId: Long, description: String, lastEditedAt: Long)

    @Transaction
    @Query("SELECT * FROM spots WHERE id = :id")
    fun getSpotDetails(id: Long): Flow<SpotDetailsEntity?>

    @Transaction
    @Query("SELECT * FROM spots WHERE ownerUid IS NULL OR ownerUid = 'local_only' OR (:activeUid IS NOT NULL AND ownerUid = :activeUid) ORDER BY createdAt DESC")
    fun getAllSpotsDetails(activeUid: String?): Flow<List<SpotDetailsEntity>>

    @Transaction
    @Query("SELECT * FROM spots WHERE category = :category AND (ownerUid IS NULL OR ownerUid = 'local_only' OR (:activeUid IS NOT NULL AND ownerUid = :activeUid)) ORDER BY createdAt DESC")
    fun getAllSpotsDetailsByCategory(category: String, activeUid: String?): Flow<List<SpotDetailsEntity>>

    @Query("SELECT DISTINCT tags FROM spots WHERE ownerUid IS NULL OR ownerUid = 'local_only' OR (:activeUid IS NOT NULL AND ownerUid = :activeUid)")
    fun getAllUsedTags(activeUid: String?): Flow<List<String>>

    @Query("UPDATE spots SET isStarred = :isStarred, lastEditedAt = :lastEditedAt, isSynced = 0 WHERE id = :spotId")
    suspend fun updateSpotStarred(spotId: Long, isStarred: Boolean, lastEditedAt: Long)

    @Query("SELECT * FROM spots WHERE isStarred = 1 AND (ownerUid IS NULL OR ownerUid = 'local_only' OR (:activeUid IS NOT NULL AND ownerUid = :activeUid))")
    suspend fun getStarredSpots(activeUid: String?): List<SpotEntity>

    @Query("SELECT COUNT(*) FROM spots WHERE isStarred = 1 AND (ownerUid IS NULL OR ownerUid = 'local_only' OR (:activeUid IS NOT NULL AND ownerUid = :activeUid))")
    suspend fun getStarredSpotsCount(activeUid: String?): Int

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

    @Query("UPDATE spot_images SET rating = :rating, lastEditedAt = :lastEditedAt WHERE id = :imageId")
    suspend fun updateImageRating(imageId: Long, rating: Long, lastEditedAt: Long)

    @Query("UPDATE spots SET artworkDate = :artworkDate, lastEditedAt = :lastEditedAt, isSynced = 0 WHERE id = :spotId")
    suspend fun updateSpotArtworkDate(spotId: Long, artworkDate: String, lastEditedAt: Long)

    @Query("DELETE FROM spot_notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Long)

    @Query("UPDATE spot_notes SET noteText = :noteText, lastEditedAt = :lastEditedAt WHERE id = :noteId")
    suspend fun updateNoteText(noteId: Long, noteText: String, lastEditedAt: Long)

    @Query("SELECT spotId FROM spot_notes WHERE id = :noteId")
    suspend fun getSpotIdForNote(noteId: Long): Long?

    @Query("SELECT spotId FROM spot_images WHERE id = :imageId")
    suspend fun getSpotIdForImage(imageId: Long): Long?

    @Transaction
    @Query("SELECT * FROM spots WHERE isSynced = 0 AND (ownerUid = :activeUid OR ownerUid IS NULL)")
    suspend fun getUnsyncedSpotsDetails(activeUid: String): List<SpotDetailsEntity>

    @Query("UPDATE spots SET isSynced = 1 WHERE uuid = :uuid")
    suspend fun markSpotAsSynced(uuid: String)

    @Query("SELECT * FROM spots WHERE uuid = :uuid")
    suspend fun getSpotByUuid(uuid: String): SpotEntity?

    @Query("SELECT * FROM spot_images WHERE uuid = :uuid")
    suspend fun getImageByUuid(uuid: String): SpotImageEntity?

    @Query("SELECT * FROM spot_notes WHERE uuid = :uuid")
    suspend fun getNoteByUuid(uuid: String): SpotNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoadedPack(pack: LoadedPackEntity)

    @Query("SELECT * FROM loaded_packs ORDER BY importedAt DESC")
    fun getAllLoadedPacks(): Flow<List<LoadedPackEntity>>

    @Query("DELETE FROM loaded_packs WHERE packId = :packId")
    suspend fun deleteLoadedPack(packId: String)

    @Query("DELETE FROM spots WHERE parentPackId = :packId")
    suspend fun deleteSpotsByPackId(packId: String)
}

