package net.maiatoday.tagspotter.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Spot(
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val createdAt: Long,
    val description: String,
    val tags: List<String>,
    val category: String, // e.g. "graffiti", "sculpture", "nature", "architecture", "food"
    val status: String, // "active" or "erased"
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
    val isPublic: Boolean = false
) {
    /**
     * Returns true if the user identified by [currentUserUid] is the author of this spot
     * and therefore allowed to edit it.
     *
     * - Imported spots (`isImported == true`) and spots belonging to a loaded shared pack
     *   (`parentPackId != null`) are never editable.
     * - Local spots without an author UID (`ownerUid.isNullOrBlank()`) or marked as
     *   [LOCAL_ONLY_UID] are editable on the device that created them.
     * - Spots with an explicit `ownerUid` (including public spots) are editable only when
     *   [currentUserUid] matches `ownerUid`.
     */
    fun isEditableBy(currentUserUid: String?): Boolean {
        if (isImported || parentPackId != null) return false
        val owner = ownerUid
        if (owner.isNullOrBlank() || owner == LOCAL_ONLY_UID) return true
        return currentUserUid != null && owner == currentUserUid
    }

    companion object {
        const val LOCAL_ONLY_UID = "local_only"
        val CATEGORIES = listOf("graffiti", "sculpture", "nature", "architecture", "public_place", "food")
    }
}

@Serializable
data class SpotImage(
    val id: Long = 0,
    val spotId: Long,
    val imagePath: String,
    val timestamp: Long,
    val thumbnailPath: String = "",
    val isMain: Boolean = false,
    val rating: Long = 0L,
    val uuid: String = generateUuid(),
    val lastEditedAt: Long = timestamp,
    val ownerUid: String? = null
)

@Serializable
data class SpotNote(
    val id: Long = 0,
    val spotId: Long,
    val noteText: String,
    val timestamp: Long,
    val uuid: String = generateUuid(),
    val lastEditedAt: Long = timestamp,
    val ownerUid: String? = null
)

@Serializable
data class SpotDetails(
    val spot: Spot,
    val images: List<SpotImage>,
    val notes: List<SpotNote>
)

@Serializable
data class BackupWrapper(
    val backupVersion: Int = 2,
    val spots: List<SpotDetails>
)
