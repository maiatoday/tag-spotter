package net.maiatoday.tagspotter.core.database

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.maiatoday.tagspotter.core.model.LoadedPack
import net.maiatoday.tagspotter.core.model.Spot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FakeSpotRepositoryTest {

    @Test
    fun testFakeSpotRepositoryOperations() = runTest {
        val repo = FakeSpotRepository()

        // 1. Initially empty
        val initialSpots = repo.getAllSpots().first()
        assertTrue(initialSpots.isEmpty())

        // 2. Save a spot
        val spot = Spot(
            id = 0L,
            latitude = 45.4642,
            longitude = 9.1899,
            createdAt = 1000L,
            description = "Test Spot",
            tags = listOf("graffiti", "milan"),
            category = "graffiti",
            status = "active"
        )
        val savedId = repo.saveSpot(spot, "/img/path.png", "/thumb/path.png", rating = 4L, isMain = true)
        assertEquals(1L, savedId)

        // 3. Get spot by ID
        val loadedDetails = repo.getSpotById(savedId).first()
        assertNotNull(loadedDetails)
        assertEquals("Test Spot", loadedDetails.spot.description)
        assertEquals(1, loadedDetails.images.size)
        assertEquals(4L, loadedDetails.images.first().rating)

        // 4. Update spot details (status, category, artists, photographer, location, tags, description)
        repo.updateSpotStatus(savedId, "erased")
        repo.updateSpotCategory(savedId, "sculpture")
        repo.updateSpotArtists(savedId, listOf("Artist 1"))
        repo.updateSpotPhotographer(savedId, "Photographer 1")
        repo.updateSpotLocation(savedId, 50.0, 10.0)
        repo.updateSpotTags(savedId, listOf("newtag"))
        repo.updateSpotDescription(savedId, "Updated Description")
        repo.updateSpotArtworkDate(savedId, "2024-05")

        val updatedDetails = repo.getSpotById(savedId).first()
        assertNotNull(updatedDetails)
        assertEquals("erased", updatedDetails.spot.status)
        assertEquals("sculpture", updatedDetails.spot.category)
        assertEquals(listOf("Artist 1"), updatedDetails.spot.artists)
        assertEquals("Photographer 1", updatedDetails.spot.photographer)
        assertEquals(50.0, updatedDetails.spot.latitude)
        assertEquals(10.0, updatedDetails.spot.longitude)
        assertEquals(listOf("newtag"), updatedDetails.spot.tags)
        assertEquals("Updated Description", updatedDetails.spot.description)
        assertEquals("2024-05", updatedDetails.spot.artworkDate)

        // 5. Notes management
        val noteId = repo.addNoteToSpot(savedId, "First Note", 2000L)
        assertEquals(1L, noteId)
        val detailsWithNote = repo.getSpotById(savedId).first()
        assertEquals(1, detailsWithNote?.notes?.size)
        assertEquals("First Note", detailsWithNote?.notes?.first()?.noteText)

        repo.updateNote(noteId, "Updated Note")
        assertEquals("Updated Note", repo.getSpotById(savedId).first()?.notes?.first()?.noteText)

        repo.deleteNote(noteId)
        assertTrue(repo.getSpotById(savedId).first()?.notes?.isEmpty() == true)

        // 6. Starred spots
        repo.updateSpotStarred(savedId, true)
        assertEquals(1, repo.getStarredSpotsCount())
        assertEquals(1, repo.getStarredSpots().size)

        // 7. Syncing & Offline adoption
        val unsynced = repo.getUnsyncedSpots()
        assertEquals(1, unsynced.size)

        repo.adoptLocalSpots("user123", backup = true)
        val adoptedDetails = repo.getSpotById(savedId).first()
        assertEquals("user123", adoptedDetails?.spot?.ownerUid)

        // 8. Loaded packs management
        val pack = LoadedPack("pack1", "Title", "Author", "Desc", 1000L, 2000L)
        repo.saveLoadedPack(pack)
        assertEquals(1, repo.getAllLoadedPacks().first().size)

        repo.deleteLoadedPack("pack1")
        assertTrue(repo.getAllLoadedPacks().first().isEmpty())

        // 9. Test Data load & unload
        repo.loadTestData()
        assertTrue(repo.getAllSpots().first().size > 1)
        repo.unloadTestData()

        // 10. Delete spot
        repo.deleteSpot(updatedDetails)
        assertTrue(repo.getAllSpots().first().isEmpty())
    }
}
