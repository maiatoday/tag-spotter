package net.maiatoday.tagspotter.data

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
    suspend fun insertSpot(spot: Spot): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: SpotImage): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: SpotNote): Long

    @Update
    suspend fun updateSpot(spot: Spot)

    @Delete
    suspend fun deleteSpot(spot: Spot)

    @Query("DELETE FROM spots WHERE id = :spotId")
    suspend fun deleteSpotById(spotId: Long)

    @Query("UPDATE spots SET status = :status WHERE id = :spotId")
    suspend fun updateSpotStatus(spotId: Long, status: String)

    @Query("UPDATE spots SET artists = :artists WHERE id = :spotId")
    suspend fun updateSpotArtists(spotId: Long, artists: List<String>)

    @Query("UPDATE spots SET photographer = :photographer WHERE id = :spotId")
    suspend fun updateSpotPhotographer(spotId: Long, photographer: String)

    @Query("UPDATE spots SET latitude = :latitude, longitude = :longitude WHERE id = :spotId")
    suspend fun updateSpotLocation(spotId: Long, latitude: Double, longitude: Double)

    @Query("UPDATE spots SET description = :description WHERE id = :spotId")
    suspend fun updateSpotDescription(spotId: Long, description: String)

    @Transaction
    @Query("SELECT * FROM spots WHERE id = :id")
    fun getSpotDetails(id: Long): Flow<SpotDetails?>

    @Transaction
    @Query("SELECT * FROM spots ORDER BY createdAt DESC")
    fun getAllSpotsDetails(): Flow<List<SpotDetails>>

    @Transaction
    @Query("SELECT * FROM spots WHERE category = :category ORDER BY createdAt DESC")
    fun getAllSpotsDetailsByCategory(category: String): Flow<List<SpotDetails>>

    @Query("SELECT DISTINCT tags FROM spots")
    fun getAllUsedTags(): Flow<List<String>>
}
