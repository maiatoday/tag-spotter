package net.maiatoday.tagspotter.core.sync

import kotlinx.serialization.Serializable
import net.maiatoday.tagspotter.core.model.Spot
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.model.SpotImage
import net.maiatoday.tagspotter.core.model.SpotNote

/**
 * Canonical Firestore document stored at `/spots/{uuid}`.
 * Top-level [ownerUid] and [isPublic] enable direct Firestore security rules and queries
 * (e.g. `where("isPublic", "==", true).where("ownerUid", "==", uid)`).
 */
@Serializable
data class FirestoreSpotDocument(
    val ownerUid: String,
    val isPublic: Boolean,
    val spot: Spot,
    val images: List<SpotImage> = emptyList(),
    val notes: List<SpotNote> = emptyList()
) {
    fun toSpotDetails(): SpotDetails = SpotDetails(
        spot = spot.copy(
            ownerUid = ownerUid,
            isPublic = isPublic
        ),
        images = images,
        notes = notes
    )

    companion object {
        fun fromSpotDetails(ownerUid: String, details: SpotDetails): FirestoreSpotDocument {
            val normalizedSpot = details.spot.copy(
                ownerUid = ownerUid,
                isPublic = details.spot.isPublic
            )
            return FirestoreSpotDocument(
                ownerUid = ownerUid,
                isPublic = normalizedSpot.isPublic,
                spot = normalizedSpot,
                images = details.images,
                notes = details.notes
            )
        }
    }
}

/**
 * Canonical Firebase Storage object path for a spot's thumbnail.
 */
fun spotThumbnailStoragePath(userId: String, spotUuid: String, imageUuid: String): String =
    "spots/$userId/$spotUuid/thumbnails/$imageUuid.jpg"
