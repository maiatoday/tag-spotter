package net.maiatoday.tagspotter.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.Serializable

@Entity(tableName = "spots")
@Serializable
data class Spot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val createdAt: Long,
    val description: String,
    val tags: List<String>,
    val category: String, // e.g. "graffiti", "sculpture", "nature", "architecture"
    val status: String, // "active" or "erased"
    val artists: List<String> = emptyList(),
    val photographer: String = "",
    val isImported: Boolean = false,
    val isStarred: Boolean = false
)

@Entity(
    tableName = "spot_images",
    foreignKeys = [
        ForeignKey(
            entity = Spot::class,
            parentColumns = ["id"],
            childColumns = ["spotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["spotId"])]
)
@Serializable
data class SpotImage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val spotId: Long,
    val imagePath: String,
    val timestamp: Long,
    val thumbnailPath: String = ""
)

@Entity(
    tableName = "spot_notes",
    foreignKeys = [
        ForeignKey(
            entity = Spot::class,
            parentColumns = ["id"],
            childColumns = ["spotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["spotId"])]
)
@Serializable
data class SpotNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val spotId: Long,
    val noteText: String,
    val timestamp: Long
)

@Serializable
data class SpotDetails(
    @Embedded val spot: Spot,
    @Relation(
        parentColumn = "id",
        entityColumn = "spotId"
    )
    val images: List<SpotImage>,
    @Relation(
        parentColumn = "id",
        entityColumn = "spotId"
    )
    val notes: List<SpotNote>
)
