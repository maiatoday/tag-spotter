package net.maiatoday.tagspotter.core.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import net.maiatoday.tagspotter.core.model.Spot
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.model.SpotImage
import net.maiatoday.tagspotter.core.model.SpotNote
import net.maiatoday.tagspotter.core.model.SpotOrigin
import net.maiatoday.tagspotter.core.model.generateUuid

@Entity(
    tableName = "spots",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["ownerUid"]),
        Index(value = ["origin"]),
        Index(value = ["origin", "ownerUid"])
    ]
)
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
    val artworkDate: String = "",
    val uuid: String = generateUuid(),
    val photographerUuid: String = "",
    val lastEditedAt: Long = createdAt,
    val isSynced: Boolean = false,
    val ownerUid: String? = null,
    val parentPackId: String? = null,
    val isPublic: Boolean = false,
    val origin: String = SpotOrigin.OWN.name
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
    indices = [
        Index(value = ["spotId"]),
        Index(value = ["uuid"]),
        Index(value = ["spotId", "uuid"])
    ]
)
data class SpotImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val spotId: Long,
    val imagePath: String,
    val timestamp: Long,
    val thumbnailPath: String = "",
    val isMain: Boolean = false,
    val rating: Long = 0L,
    val uuid: String = "",
    val lastEditedAt: Long = timestamp,
    val ownerUid: String? = null,
    val remoteThumbnailUrl: String = ""
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
    indices = [
        Index(value = ["spotId"]),
        Index(value = ["uuid"]),
        Index(value = ["spotId", "uuid"])
    ]
)
data class SpotNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val spotId: Long,
    val noteText: String,
    val timestamp: Long,
    val uuid: String = "",
    val lastEditedAt: Long = timestamp,
    val ownerUid: String? = null
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

private fun parseSpotOrigin(raw: String, isImported: Boolean, parentPackId: String?): SpotOrigin {
    val parsed = SpotOrigin.entries.firstOrNull { it.name == raw }
    if (parsed != null) return parsed
    return if (isImported || parentPackId != null) SpotOrigin.IMPORTED else SpotOrigin.OWN
}

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
    artworkDate = artworkDate,
    uuid = uuid,
    photographerUuid = photographerUuid,
    lastEditedAt = lastEditedAt,
    isSynced = isSynced,
    ownerUid = ownerUid,
    parentPackId = parentPackId,
    isPublic = isPublic,
    origin = parseSpotOrigin(origin, isImported, parentPackId)
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
    isImported = isImported || effectiveOrigin == SpotOrigin.IMPORTED,
    isStarred = isStarred,
    artworkDate = artworkDate,
    uuid = uuid,
    photographerUuid = photographerUuid,
    lastEditedAt = lastEditedAt,
    isSynced = isSynced,
    ownerUid = ownerUid,
    parentPackId = parentPackId,
    isPublic = isPublic,
    origin = effectiveOrigin.name
)

fun SpotImageEntity.toDomain(): SpotImage = SpotImage(
    id = id,
    spotId = spotId,
    imagePath = imagePath,
    timestamp = timestamp,
    thumbnailPath = thumbnailPath,
    isMain = isMain,
    rating = rating,
    uuid = uuid,
    lastEditedAt = lastEditedAt,
    ownerUid = ownerUid,
    remoteThumbnailUrl = remoteThumbnailUrl
)

fun SpotImage.toEntity(): SpotImageEntity = SpotImageEntity(
    id = id,
    spotId = spotId,
    imagePath = imagePath,
    timestamp = timestamp,
    thumbnailPath = thumbnailPath,
    isMain = isMain,
    rating = rating,
    uuid = uuid,
    lastEditedAt = lastEditedAt,
    ownerUid = ownerUid,
    remoteThumbnailUrl = remoteThumbnailUrl
)

fun SpotNoteEntity.toDomain(): SpotNote = SpotNote(
    id = id,
    spotId = spotId,
    noteText = noteText,
    timestamp = timestamp,
    uuid = uuid,
    lastEditedAt = lastEditedAt,
    ownerUid = ownerUid
)

fun SpotNote.toEntity(): SpotNoteEntity = SpotNoteEntity(
    id = id,
    spotId = spotId,
    noteText = noteText,
    timestamp = timestamp,
    uuid = uuid,
    lastEditedAt = lastEditedAt,
    ownerUid = ownerUid
)

fun SpotDetailsEntity.toDomain(): SpotDetails = SpotDetails(
    spot = spot.toDomain(),
    images = images.map { it.toDomain() },
    notes = notes.map { it.toDomain() }
)

@Entity(tableName = "loaded_packs")
data class LoadedPackEntity(
    @PrimaryKey val packId: String,
    val title: String,
    val authorName: String,
    val description: String,
    val importedAt: Long,
    val lastRefreshedAt: Long
)

fun LoadedPackEntity.toDomain(): net.maiatoday.tagspotter.core.model.LoadedPack = net.maiatoday.tagspotter.core.model.LoadedPack(
    packId = packId,
    title = title,
    authorName = authorName,
    description = description,
    importedAt = importedAt,
    lastRefreshedAt = lastRefreshedAt
)

fun net.maiatoday.tagspotter.core.model.LoadedPack.toEntity(): LoadedPackEntity = LoadedPackEntity(
    packId = packId,
    title = title,
    authorName = authorName,
    description = description,
    importedAt = importedAt,
    lastRefreshedAt = lastRefreshedAt
)
