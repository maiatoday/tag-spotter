package net.maiatoday.tagspotter.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class SpotOrigin {
    OWN,
    IMPORTED,
    PUBLIC_CACHE
}

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
    val isPublic: Boolean = false,
    val origin: SpotOrigin = SpotOrigin.OWN
) {
    /**
     * Resolved provenance accounting for legacy backups where [isImported] or [parentPackId]
     * may be set without an explicit [origin].
     */
    val effectiveOrigin: SpotOrigin
        get() = when {
            origin == SpotOrigin.PUBLIC_CACHE -> SpotOrigin.PUBLIC_CACHE
            origin == SpotOrigin.IMPORTED || isImported || parentPackId != null -> SpotOrigin.IMPORTED
            else -> SpotOrigin.OWN
        }

    /**
     * Returns true if the user identified by [currentUserUid] is the author of this spot
     * and therefore allowed to edit it.
     *
     * - Only [SpotOrigin.OWN] spots can be edited; [SpotOrigin.IMPORTED] and
     *   [SpotOrigin.PUBLIC_CACHE] spots are never editable.
     * - Local spots without an author UID (`ownerUid.isNullOrBlank()`) or marked as
     *   [LOCAL_ONLY_UID] are editable on the device that created them.
     * - Spots with an explicit `ownerUid` (including public spots) are editable only when
     *   [currentUserUid] matches `ownerUid`.
     */
    fun isEditableBy(currentUserUid: String?): Boolean {
        if (effectiveOrigin != SpotOrigin.OWN) return false
        val owner = ownerUid
        if (owner.isNullOrBlank() || owner == LOCAL_ONLY_UID) return true
        return currentUserUid != null && owner == currentUserUid
    }

    /**
     * Returns true if this spot can be deleted locally by the user identified by [currentUserUid].
     *
     * Authors can delete their own spots, and users can also delete spots they imported from a pack
     * (local removal only). Other users' cached public spots cannot be deleted.
     */
    fun isDeletableBy(currentUserUid: String?): Boolean =
        isEditableBy(currentUserUid) || effectiveOrigin == SpotOrigin.IMPORTED

    /**
     * Returns true if this spot is a read-only public spot shared by another user
     * (distinct from the viewer's own spots and imported pack spots).
     */
    fun isOtherUsersPublic(currentUserUid: String?): Boolean =
        effectiveOrigin == SpotOrigin.PUBLIC_CACHE ||
            (effectiveOrigin == SpotOrigin.OWN && isPublic && !isEditableBy(currentUserUid))

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
    val ownerUid: String? = null,
    val remoteThumbnailUrl: String = ""
) {
    /**
     * Preferred thumbnail path for display: local/explicit thumbnailPath, falling back to [remoteThumbnailUrl].
     */
    val displayThumbnailPath: String
        get() = thumbnailPath.ifBlank { remoteThumbnailUrl }

    /**
     * Preferred full image path for display: local/explicit imagePath, falling back to [remoteThumbnailUrl].
     */
    val displayImagePath: String
        get() = imagePath.ifBlank { remoteThumbnailUrl }
}

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
