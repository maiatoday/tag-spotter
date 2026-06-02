package net.maiatoday.tagspotter.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ImportSpotsTest {

    @Test
    fun testImportSpotsFilterDuplicatesAndInsertsCorrectly() = runTest {
        val repository = FakeSpotRepository()
        
        // 1. Initial state: load test data (3 spots)
        repository.loadTestData()
        
        // 2. Prepare imported spots:
        // - Spot A: identical coordinates and createdAt as mock spot 9001 (should be skipped)
        // - Spot B: unique coordinates and createdAt (should be imported)
        val now = System.currentTimeMillis()
        val mockSpot1Details = repository.getSpotById(9001L).first()
        assertNotNull(mockSpot1Details)
        
        val spotA = Spot(
            id = 9999L, // different ID, but coordinates and time match
            latitude = 45.4642,
            longitude = 9.1899,
            createdAt = mockSpot1Details!!.spot.createdAt,
            description = "Stunning street art stencil near the Duomo in Milan.",
            tags = listOf("milan"),
            category = "graffiti",
            status = "active"
        )
        val spotB = Spot(
            id = 8888L,
            latitude = 12.3456,
            longitude = 78.9012,
            createdAt = now + 5000L,
            description = "New unique imported spot",
            tags = listOf("imported"),
            category = "sculpture",
            status = "active"
        )
        
        val detailsA = SpotDetails(spotA, listOf(SpotImage(id = 1L, spotId = 9999L, imagePath = "imgA", timestamp = now)), emptyList())
        val detailsB = SpotDetails(spotB, listOf(SpotImage(id = 1L, spotId = 8888L, imagePath = "imgB", timestamp = now)), listOf(SpotNote(id = 1L, spotId = 8888L, noteText = "Note B", timestamp = now)))
        
        // Let's run import
        val importedCount = repository.importSpots(listOf(detailsA, detailsB))
        
        // Spot A should be skipped as duplicate, Spot B should be imported
        assertEquals(1, importedCount)
        
        // Verify Spot B exists in repo with a newly generated ID (not 8888L)
        val allSpots = repository.getAllSpots().first()
        assertEquals(4, allSpots.size) // 3 initial + 1 imported
        
        val importedSpotDetails = allSpots.find { it.spot.description == "New unique imported spot" }
        assertNotNull(importedSpotDetails)
        assertNotEquals(8888L, importedSpotDetails!!.spot.id)
        assertEquals("imgB", importedSpotDetails.images.first().imagePath)
        assertEquals(importedSpotDetails.spot.id, importedSpotDetails.images.first().spotId)
        assertEquals("Note B", importedSpotDetails.notes.first().noteText)
        assertEquals(importedSpotDetails.spot.id, importedSpotDetails.notes.first().spotId)
    }
}
