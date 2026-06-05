package net.maiatoday.tagspotter.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpotDaoTest {

    private lateinit var db: SpotDatabase
    private lateinit var dao: SpotDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SpotDatabase::class.java).build()
        dao = db.spotDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndUpdateSpotArtists() = runBlocking {
        val spot = Spot(
            id = 1L,
            latitude = 12.34,
            longitude = 56.78,
            createdAt = System.currentTimeMillis(),
            description = "Test Spot",
            tags = listOf("tag1"),
            category = "graffiti",
            status = "active",
            artists = listOf("Initial Artist")
        )

        dao.insertSpot(spot)

        val updatedArtists = listOf("Artist A", "Artist B")
        dao.updateSpotArtists(1L, updatedArtists)

        val details = dao.getSpotDetails(1L).first()
        assertEquals(updatedArtists, details?.spot?.artists)
    }

    @Test
    fun cascadingDeleteOfImagesAndNotes() = runBlocking {
        val spot = Spot(
            id = 1L,
            latitude = 12.34,
            longitude = 56.78,
            createdAt = 1000L,
            description = "Spot 1",
            tags = listOf("tag1"),
            category = "graffiti",
            status = "active"
        )
        dao.insertSpot(spot)

        dao.insertImage(SpotImage(id = 10L, spotId = 1L, imagePath = "/path/1.png", timestamp = 1000L))
        dao.insertImage(SpotImage(id = 11L, spotId = 1L, imagePath = "/path/2.png", timestamp = 1100L))
        dao.insertNote(SpotNote(id = 20L, spotId = 1L, noteText = "Note 1", timestamp = 1000L))

        // Verify they are inserted and linked
        val details = dao.getSpotDetails(1L).first()
        assertNotNull(details)
        assertEquals(2, details?.images?.size)
        assertEquals(1, details?.notes?.size)

        // Delete the spot (should cascade delete images and notes)
        dao.deleteSpotById(1L)

        // Verify spot is deleted
        val deletedDetails = dao.getSpotDetails(1L).first()
        assertNull(deletedDetails)

        // Verify that inserting a new spot with the same ID doesn't pick up old notes/images
        val spotNew = Spot(
            id = 1L,
            latitude = 12.34,
            longitude = 56.78,
            createdAt = 2000L,
            description = "Spot 1 New",
            tags = emptyList(),
            category = "graffiti",
            status = "active"
        )
        dao.insertSpot(spotNew)
        val newDetails = dao.getSpotDetails(1L).first()
        assertNotNull(newDetails)
        assertTrue(newDetails?.images?.isEmpty() ?: false)
        assertTrue(newDetails?.notes?.isEmpty() ?: false)
    }

    @Test
    fun databaseQueryFlows() = runBlocking {
        val spot1 = Spot(
            id = 1L,
            latitude = 1.0,
            longitude = 1.0,
            createdAt = 1000L,
            description = "Spot 1",
            tags = listOf("tag1", "tag2"),
            category = "graffiti",
            status = "active"
        )
        val spot2 = Spot(
            id = 2L,
            latitude = 2.0,
            longitude = 2.0,
            createdAt = 2000L,
            description = "Spot 2",
            tags = listOf("tag2", "tag3"),
            category = "sculpture",
            status = "active"
        )
        val spot3 = Spot(
            id = 3L,
            latitude = 3.0,
            longitude = 3.0,
            createdAt = 3000L,
            description = "Spot 3",
            tags = emptyList(),
            category = "graffiti",
            status = "erased"
        )

        dao.insertSpot(spot1)
        dao.insertSpot(spot2)
        dao.insertSpot(spot3)

        // Test getAllSpotsDetails (should sort by createdAt desc)
        val allSpots = dao.getAllSpotsDetails().first()
        assertEquals(3, allSpots.size)
        assertEquals(3L, allSpots[0].spot.id)
        assertEquals(2L, allSpots[1].spot.id)
        assertEquals(1L, allSpots[2].spot.id)

        // Test getAllSpotsDetailsByCategory
        val graffitiSpots = dao.getAllSpotsDetailsByCategory("graffiti").first()
        assertEquals(2, graffitiSpots.size)
        assertEquals(3L, graffitiSpots[0].spot.id)
        assertEquals(1L, graffitiSpots[1].spot.id)

        // Test getAllUsedTags
        val allTags = dao.getAllUsedTags().first()
        val expected1 = Converters().fromStringList(listOf("tag1", "tag2"))
        val expected2 = Converters().fromStringList(listOf("tag2", "tag3"))
        assertTrue(allTags.contains(expected1))
        assertTrue(allTags.contains(expected2))
    }

    @Test
    fun setMainImageTransaction() = runBlocking {
        val spot = Spot(
            id = 1L,
            latitude = 12.34,
            longitude = 56.78,
            createdAt = 1000L,
            description = "Spot 1",
            tags = listOf("tag1"),
            category = "graffiti",
            status = "active"
        )
        dao.insertSpot(spot)

        dao.insertImage(SpotImage(id = 10L, spotId = 1L, imagePath = "/path/1.png", timestamp = 1000L, isMain = false))
        dao.insertImage(SpotImage(id = 11L, spotId = 1L, imagePath = "/path/2.png", timestamp = 1100L, isMain = false))

        // Set image 11 as main
        dao.setMainImage(1L, 11L)

        val details = dao.getSpotDetails(1L).first()
        assertNotNull(details)
        val image10 = details?.images?.find { it.id == 10L }
        val image11 = details?.images?.find { it.id == 11L }
        assertTrue(image11?.isMain == true)
        assertTrue(image10?.isMain == false)

        // Switch main image to 10
        dao.setMainImage(1L, 10L)
        val detailsNew = dao.getSpotDetails(1L).first()
        val image10New = detailsNew?.images?.find { it.id == 10L }
        val image11New = detailsNew?.images?.find { it.id == 11L }
        assertTrue(image10New?.isMain == true)
        assertTrue(image11New?.isMain == false)
    }
}
