package net.maiatoday.tagspotter.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpotOriginModelTest {

    private fun baseSpot(
        ownerUid: String? = null,
        isImported: Boolean = false,
        parentPackId: String? = null,
        isPublic: Boolean = false,
        origin: SpotOrigin = SpotOrigin.OWN
    ) = Spot(
        latitude = 45.46,
        longitude = 9.19,
        createdAt = 1000L,
        description = "Test",
        tags = emptyList(),
        category = "graffiti",
        status = "active",
        ownerUid = ownerUid,
        isImported = isImported,
        parentPackId = parentPackId,
        isPublic = isPublic,
        origin = origin
    )

    @Test
    fun ownSpotEditAndDeletePermissions() {
        val own = baseSpot(ownerUid = "alice", origin = SpotOrigin.OWN)
        assertTrue(own.isEditableBy("alice"))
        assertTrue(own.isDeletableBy("alice"))
        assertFalse(own.isEditableBy("bob"))
        assertFalse(own.isDeletableBy("bob"))
        assertFalse(own.isEditableBy(null))
        assertFalse(own.isDeletableBy(null))
    }

    @Test
    fun importedSpotIsNeverEditableButDeletableLocally() {
        val imported = baseSpot(ownerUid = "alice", isImported = true, parentPackId = "PACK1", origin = SpotOrigin.IMPORTED)
        assertEquals(SpotOrigin.IMPORTED, imported.effectiveOrigin)
        assertFalse(imported.isEditableBy("alice"))
        assertFalse(imported.isEditableBy("bob"))
        assertTrue(imported.isDeletableBy("alice"))
        assertTrue(imported.isDeletableBy("bob"))
        assertTrue(imported.isDeletableBy(null))
        assertFalse(imported.isOtherUsersPublic("bob"))
    }

    @Test
    fun publicCacheSpotIsReadOnlyAndNonDeletable() {
        val publicCache = baseSpot(ownerUid = "alice", isPublic = true, origin = SpotOrigin.PUBLIC_CACHE)
        assertEquals(SpotOrigin.PUBLIC_CACHE, publicCache.effectiveOrigin)
        assertFalse(publicCache.isEditableBy("alice"))
        assertFalse(publicCache.isEditableBy("bob"))
        assertFalse(publicCache.isDeletableBy("bob"))
        assertFalse(publicCache.isDeletableBy(null))
        assertTrue(publicCache.isOtherUsersPublic("bob"))
        assertTrue(publicCache.isOtherUsersPublic(null))
    }
}
