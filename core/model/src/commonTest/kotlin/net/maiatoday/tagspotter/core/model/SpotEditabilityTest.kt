package net.maiatoday.tagspotter.core.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpotEditabilityTest {

    private fun baseSpot(
        ownerUid: String? = null,
        isPublic: Boolean = false,
        isImported: Boolean = false,
        parentPackId: String? = null
    ) = Spot(
        id = 1L,
        latitude = 45.4642,
        longitude = 9.1900,
        createdAt = 1000L,
        description = "Test Spot",
        tags = listOf("mural"),
        category = "graffiti",
        status = "active",
        ownerUid = ownerUid,
        isPublic = isPublic,
        isImported = isImported,
        parentPackId = parentPackId
    )

    @Test
    fun ownSignedInPrivateSpot_isEditableByAuthor() {
        val spot = baseSpot(ownerUid = "user-1", isPublic = false)
        assertTrue(spot.isEditableBy("user-1"))
    }

    @Test
    fun ownSignedInPublicSpot_isEditableByAuthor() {
        val spot = baseSpot(ownerUid = "user-1", isPublic = true)
        assertTrue(spot.isEditableBy("user-1"))
    }

    @Test
    fun publicSpotByOtherAuthor_isNotEditableWhenSignedIn() {
        val spot = baseSpot(ownerUid = "other-user", isPublic = true)
        assertFalse(spot.isEditableBy("user-1"))
    }

    @Test
    fun publicSpotByOtherAuthor_isNotEditableWhenSignedOut() {
        val spot = baseSpot(ownerUid = "other-user", isPublic = true)
        assertFalse(spot.isEditableBy(null))
    }

    @Test
    fun privateSpotWithOwnerUid_isNotEditableWhenSignedOut() {
        val spot = baseSpot(ownerUid = "user-1", isPublic = false)
        assertFalse(spot.isEditableBy(null))
    }

    @Test
    fun importedPackSpotByOtherAuthor_isNotEditable() {
        val spot = baseSpot(ownerUid = "other-user", isImported = true, parentPackId = "PACK01")
        assertFalse(spot.isEditableBy("user-1"))
        assertFalse(spot.isEditableBy(null))
    }

    @Test
    fun importedPackSpotWithNullOwner_isNotEditable() {
        val spot = baseSpot(ownerUid = null, isImported = true, parentPackId = "PACK01")
        assertFalse(spot.isEditableBy("user-1"))
        assertFalse(spot.isEditableBy(null))
    }

    @Test
    fun spotWithParentPackIdEvenIfNotFlaggedImported_isNotEditable() {
        val spot = baseSpot(ownerUid = "user-1", isImported = false, parentPackId = "PACK01")
        assertFalse(spot.isEditableBy("user-1"))
    }

    @Test
    fun importedZipSpotByOtherAuthor_isNotEditableEvenIfOwnerUidMatches() {
        val spot = baseSpot(ownerUid = "user-1", isImported = true, parentPackId = null)
        assertFalse(spot.isEditableBy("user-1"))
    }

    @Test
    fun localSpotCreatedWhileSignedOut_isEditable() {
        val spot = baseSpot(ownerUid = null, isImported = false, parentPackId = null)
        assertTrue(spot.isEditableBy(null))
        assertTrue(spot.isEditableBy("user-1"))
    }

    @Test
    fun localOnlySpot_isEditable() {
        val spot = baseSpot(ownerUid = Spot.LOCAL_ONLY_UID, isImported = false, parentPackId = null)
        assertTrue(spot.isEditableBy(null))
        assertTrue(spot.isEditableBy("user-1"))
    }
}
