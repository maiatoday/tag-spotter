package net.maiatoday.tagspotter.core.sync

import net.maiatoday.tagspotter.core.model.SpotDetails

/**
 * Determines whether an incoming remote [SpotDetails] snapshot should overwrite [localMatch].
 *
 * Protects pending unsynced local edits, and triggers an update when:
 * - remote `lastEditedAt` is newer,
 * - image or note UUID sets (or counts) differ (heals mismatched UUIDs across devices/imports),
 * - any matched image has a new `remoteThumbnailUrl` or changed `isMain` flag.
 */
internal fun shouldApplyRemoteUpdate(localMatch: SpotDetails, remoteDetail: SpotDetails): Boolean {
    if (!localMatch.spot.isSynced) return false
    if (remoteDetail.spot.lastEditedAt < localMatch.spot.lastEditedAt) return false
    if (remoteDetail.spot.lastEditedAt > localMatch.spot.lastEditedAt) return true

    val localImageUuids = localMatch.images.map { it.uuid }.toSet()
    val remoteImageUuids = remoteDetail.images.map { it.uuid }.toSet()
    if (localMatch.images.size != remoteDetail.images.size || localImageUuids != remoteImageUuids) {
        return true
    }

    val localNoteUuids = localMatch.notes.map { it.uuid }.toSet()
    val remoteNoteUuids = remoteDetail.notes.map { it.uuid }.toSet()
    if (localMatch.notes.size != remoteDetail.notes.size || localNoteUuids != remoteNoteUuids) {
        return true
    }

    return localMatch.images.any { localImg ->
        val remoteImg = remoteDetail.images.find { it.uuid == localImg.uuid }
        remoteImg != null && (
            (remoteImg.remoteThumbnailUrl.isNotBlank() && localImg.remoteThumbnailUrl != remoteImg.remoteThumbnailUrl) ||
                localImg.isMain != remoteImg.isMain
            )
    }
}

/**
 * Returns true if any image in this spot lacks a resolved HTTP(S) remote thumbnail URL.
 */
internal fun SpotDetails.hasUnresolvedRemoteThumbnails(): Boolean =
    images.any { image ->
        val resolved = image.remoteThumbnailUrl.ifBlank {
            if (image.thumbnailPath.startsWith("http")) image.thumbnailPath else ""
        }
        resolved.isBlank()
    }
