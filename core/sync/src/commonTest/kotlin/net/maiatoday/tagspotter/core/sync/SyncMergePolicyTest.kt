package net.maiatoday.tagspotter.core.sync

import net.maiatoday.tagspotter.core.model.Spot
import net.maiatoday.tagspotter.core.model.SpotDetails
import net.maiatoday.tagspotter.core.model.SpotImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyncMergePolicyTest {

    private fun sampleSpot(isSynced: Boolean = true, lastEditedAt: Long = 1000L): Spot = Spot(
        id = 1L,
        latitude = 52.52,
        longitude = 13.405,
        createdAt = 1000L,
        description = "Mural",
        tags = listOf("streetart"),
        category = "graffiti",
        status = "active",
        uuid = "spot-1",
        lastEditedAt = lastEditedAt,
        isSynced = isSynced,
        ownerUid = "user-1"
    )

    private fun sampleImage(
        uuid: String = "img-1",
        thumbnailPath: String = "/local/thumb.jpg",
        remoteUrl: String = "https://example.com/1.jpg",
        isMain: Boolean = true
    ): SpotImage = SpotImage(
        id = 1L,
        spotId = 1L,
        imagePath = thumbnailPath,
        thumbnailPath = thumbnailPath,
        timestamp = 1000L,
        isMain = isMain,
        uuid = uuid,
        remoteThumbnailUrl = remoteUrl
    )

    @Test
    fun protectsUnsyncedLocalSpotFromRemoteOverwrite() {
        val local = SpotDetails(sampleSpot(isSynced = false, lastEditedAt = 1000L), listOf(sampleImage()), emptyList())
        val remote = SpotDetails(sampleSpot(isSynced = true, lastEditedAt = 2000L), listOf(sampleImage(remoteUrl = "https://example.com/new.jpg")), emptyList())

        assertFalse(shouldApplyRemoteUpdate(local, remote))
    }

    @Test
    fun appliesRemoteUpdateWhenImageUuidsDifferWithSameCountAndTimestamp() {
        val local = SpotDetails(sampleSpot(lastEditedAt = 1000L), listOf(sampleImage(uuid = "stale-local-uuid")), emptyList())
        val remote = SpotDetails(sampleSpot(lastEditedAt = 1000L), listOf(sampleImage(uuid = "canonical-remote-uuid")), emptyList())

        assertTrue(shouldApplyRemoteUpdate(local, remote))
    }

    @Test
    fun appliesRemoteUpdateWhenRemoteThumbnailUrlOrIsMainChanges() {
        val local = SpotDetails(sampleSpot(lastEditedAt = 1000L), listOf(sampleImage(remoteUrl = "https://example.com/v1.jpg", isMain = true)), emptyList())
        val remoteUrlChanged = SpotDetails(sampleSpot(lastEditedAt = 1000L), listOf(sampleImage(remoteUrl = "https://example.com/v2.jpg", isMain = true)), emptyList())
        val remoteMainChanged = SpotDetails(sampleSpot(lastEditedAt = 1000L), listOf(sampleImage(remoteUrl = "https://example.com/v1.jpg", isMain = false)), emptyList())
        val identical = SpotDetails(sampleSpot(lastEditedAt = 1000L), listOf(sampleImage(remoteUrl = "https://example.com/v1.jpg", isMain = true)), emptyList())

        assertTrue(shouldApplyRemoteUpdate(local, remoteUrlChanged))
        assertTrue(shouldApplyRemoteUpdate(local, remoteMainChanged))
        assertFalse(shouldApplyRemoteUpdate(local, identical))
    }

    @Test
    fun detectsUnresolvedRemoteThumbnailsForPublicSync() {
        val unresolved = SpotDetails(
            sampleSpot(),
            listOf(sampleImage(thumbnailPath = "/data/user/0/thumb.jpg", remoteUrl = "")),
            emptyList()
        )
        val resolvedByField = SpotDetails(
            sampleSpot(),
            listOf(sampleImage(thumbnailPath = "/data/user/0/thumb.jpg", remoteUrl = "https://example.com/1.jpg")),
            emptyList()
        )
        val resolvedByLegacyThumbPath = SpotDetails(
            sampleSpot(),
            listOf(sampleImage(thumbnailPath = "https://example.com/1.jpg", remoteUrl = "")),
            emptyList()
        )

        assertTrue(unresolved.hasUnresolvedRemoteThumbnails())
        assertFalse(resolvedByField.hasUnresolvedRemoteThumbnails())
        assertFalse(resolvedByLegacyThumbPath.hasUnresolvedRemoteThumbnails())
    }

    @Test
    fun rejectsOlderRemoteSnapshotEvenWhenImageUuidsDiffer() {
        val local = SpotDetails(sampleSpot(lastEditedAt = 2000L), listOf(sampleImage(uuid = "newer-local-uuid")), emptyList())
        val olderRemote = SpotDetails(sampleSpot(lastEditedAt = 1000L), listOf(sampleImage(uuid = "older-remote-uuid")), emptyList())

        assertFalse(shouldApplyRemoteUpdate(local, olderRemote))
    }

    @Test
    fun backupResultFormatsAccurateUserMessages() {
        assertEquals("Backup unavailable right now; please try again", BackupResult(attempted = false).toUserMessage())
        assertEquals("Uploaded 2 photo(s) to cloud", BackupResult(uploaded = 2).toUserMessage())
        assertEquals("Uploaded 1 photo(s); 2 failed", BackupResult(uploaded = 1, failed = 2).toUserMessage())
        assertEquals("Refreshed 3 cloud photo link(s)", BackupResult(refreshedUrls = 3).toUserMessage())
        assertEquals("Failed to upload 1 photo(s)", BackupResult(failed = 1).toUserMessage())
        assertEquals(
            "No local photo files found on this device to upload",
            BackupResult(skippedNoLocalFile = 1).toUserMessage()
        )
        assertEquals("Photos already backed up", BackupResult().toUserMessage())
    }
}
