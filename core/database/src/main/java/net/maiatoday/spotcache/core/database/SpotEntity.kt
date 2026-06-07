package net.maiatoday.spotcache.core.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import net.maiatoday.spotcache.core.model.Spot
import net.maiatoday.spotcache.core.model.SpotDetails
import net.maiatoday.spotcache.core.model.SpotImage
import net.maiatoday.spotcache.core.model.SpotNote

@Entity(tableName = "spots")
data class SpotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val createdAt: Long,
    val description: String,
    val tags: List<String>,
    val category: String,
    val status: String,
    val artists: List<String> = emptyList(),
    val photographer: String = "",
    val isImported: Boolean = false,
    val isStarred: Boolean = false,
    val artworkDate: String = ""
)

@Entity(
    tableName = "spot_images",
    foreignKeys = [
        ForeignKey(
            entity = SpotEntity::class,
            parentColumns = ["id"],
            childColumns = ["spotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["spotId"])]
)
data class SpotImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val spotId: Long,
    val imagePath: String,
    val timestamp: Long,
    val thumbnailPath: String = "",
    val isMain: Boolean = false,
    val rating: Int = 0
)

@Entity(
    tableName = "spot_notes",
    foreignKeys = [
        ForeignKey(
            entity = SpotEntity::class,
            parentColumns = ["id"],
            childColumns = ["spotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["spotId"])]
)
data class SpotNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val spotId: Long,
    val noteText: String,
    val timestamp: Long
)

data class SpotDetailsEntity(
    @Embedded val spot: SpotEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "spotId"
    )
    val images: List<SpotImageEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "spotId"
    )
    val notes: List<SpotNoteEntity>
)

// Mapping extensions to map Entities to Domain Models

fun SpotEntity.toDomain(): Spot = Spot(
    id = id,
    latitude = latitude,
    longitude = longitude,
    createdAt = createdAt,
    description = description,
    tags = tags,
    category = category,
    status = status,
    artists = artists,
    photographer = photographer,
    isImported = isImported,
    isStarred = isStarred,
    artworkDate = artworkDate
)

fun Spot.toEntity(): SpotEntity = SpotEntity(
    id = id,
    latitude = latitude,
    longitude = longitude,
    createdAt = createdAt,
    description = description,
    tags = tags,
    category = category,
    status = status,
    artists = artists,
    photographer = photographer,
    isImported = isImported,
    isStarred = isStarred,
    artworkDate = artworkDate
)

fun SpotImageEntity.toDomain(): SpotImage = SpotImage(
    id = id,
    spotId = spotId,
    imagePath = imagePath,
    timestamp = timestamp,
    thumbnailPath = thumbnailPath,
    isMain = isMain,
    rating = rating
)

fun SpotImage.toEntity(): SpotImageEntity = SpotImageEntity(
    id = id,
    spotId = spotId,
    imagePath = imagePath,
    timestamp = timestamp,
    thumbnailPath = thumbnailPath,
    isMain = isMain,
    rating = rating
)

fun SpotNoteEntity.toDomain(): SpotNote = SpotNote(
    id = id,
    spotId = spotId,
    noteText = noteText,
    timestamp = timestamp
)

fun SpotNote.toEntity(): SpotNoteEntity = SpotNoteEntity(
    id = id,
    spotId = spotId,
    noteText = noteText,
    timestamp = timestamp
)

fun SpotDetailsEntity.toDomain(): SpotDetails = SpotDetails(
    spot = spot.toDomain(),
    images = images.map { it.toDomain() },
    notes = notes.map { it.toDomain() }
)
