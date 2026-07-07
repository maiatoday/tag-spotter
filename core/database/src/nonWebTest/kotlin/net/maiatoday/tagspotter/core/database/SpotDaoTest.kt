package net.maiatoday.tagspotter.core.database

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpotDaoTest : BaseDaoTest() {

    private lateinit var db: SpotDatabase
    private lateinit var dao: SpotDao

    @BeforeTest
    fun createDb() {
        db = getTestDatabaseBuilder().build()
        dao = db.spotDao()
    }

    @AfterTest
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndUpdateSpotArtists() = runTest {
        val spot = SpotEntity(
            id = 1L,
            latitude = 12.34,
            longitude = 56.78,
            createdAt = 123456789L,
            description = "Test Spot",
            tags = listOf("tag1"),
            category = "graffiti",
            status = "active",
            artists = listOf("Initial Artist")
        )

        dao.insertSpot(spot)

        val updatedArtists = listOf("Artist A", "Artist B")
        dao.updateSpotArtists(1L, updatedArtists, 987654321L)

        val details = dao.getSpotDetails(1L).first()
        assertEquals(updatedArtists, details?.spot?.artists)
        assertEquals(987654321L, details?.spot?.lastEditedAt)
        assertEquals(false, details?.spot?.isSynced)
    }

    @Test
    fun cascadingDeleteOfImagesAndNotes() = runTest {
        val spot = SpotEntity(
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

        dao.insertImage(
            SpotImageEntity(
                id = 10L,
                spotId = 1L,
                imagePath = "/path/1.png",
                timestamp = 1000L
            )
        )
        dao.insertImage(
            SpotImageEntity(
                id = 11L,
                spotId = 1L,
                imagePath = "/path/2.png",
                timestamp = 1100L
            )
        )
        dao.insertNote(SpotNoteEntity(id = 20L, spotId = 1L, noteText = "Note 1", timestamp = 1000L))

        // Verify they are inserted and linked
        val details = dao.getSpotDetails(1L).first()
        assertNotNull(details)
        assertEquals(2, details.images.size)
        assertEquals(1, details.notes.size)

        // Delete the spot (should cascade delete images and notes)
        dao.deleteSpotById(1L)

        // Verify spot is deleted
        val deletedDetails = dao.getSpotDetails(1L).first()
        assertNull(deletedDetails)

        // Verify that inserting a new spot with the same ID doesn't pick up old notes/images
        val spotNew = SpotEntity(
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
        assertTrue(newDetails.images.isEmpty())
        assertTrue(newDetails.notes.isEmpty())
    }

    @Test
    fun databaseQueryFlows() = runTest {
        val spot1 = SpotEntity(
            id = 1L,
            latitude = 1.0,
            longitude = 1.0,
            createdAt = 1000L,
            description = "Spot 1",
            tags = listOf("tag1", "tag2"),
            category = "graffiti",
            status = "active"
        )
        val spot2 = SpotEntity(
            id = 2L,
            latitude = 2.0,
            longitude = 2.0,
            createdAt = 2000L,
            description = "Spot 2",
            tags = listOf("tag2", "tag3"),
            category = "sculpture",
            status = "active"
        )
        val spot3 = SpotEntity(
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
        val allSpots = dao.getAllSpotsDetails(null).first()
        assertEquals(3, allSpots.size)
        assertEquals(3L, allSpots[0].spot.id)
        assertEquals(2L, allSpots[1].spot.id)
        assertEquals(1L, allSpots[2].spot.id)

        // Test getAllSpotsDetailsByCategory
        val graffitiSpots = dao.getAllSpotsDetailsByCategory("graffiti", null).first()
        assertEquals(2, graffitiSpots.size)
        assertEquals(3L, graffitiSpots[0].spot.id)
        assertEquals(1L, graffitiSpots[1].spot.id)

        // Test getAllUsedTags
        val allTags = dao.getAllUsedTags(null).first()
        val expected1 = Converters().fromStringList(listOf("tag1", "tag2"))
        val expected2 = Converters().fromStringList(listOf("tag2", "tag3"))
        assertTrue(allTags.contains(expected1))
        assertTrue(allTags.contains(expected2))
    }

    @Test
    fun setMainImageTransaction() = runTest {
        val spot = SpotEntity(
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

        dao.insertImage(
            SpotImageEntity(
                id = 10L,
                spotId = 1L,
                imagePath = "/path/1.png",
                timestamp = 1000L,
                isMain = false
            )
        )
        dao.insertImage(
            SpotImageEntity(
                id = 11L,
                spotId = 1L,
                imagePath = "/path/2.png",
                timestamp = 1100L,
                isMain = false
            )
        )

        // Set image 11 as main
        dao.setMainImage(1L, 11L)

        val details = dao.getSpotDetails(1L).first()
        assertNotNull(details)
        val image10 = details.images.find { it.id == 10L }
        val image11 = details.images.find { it.id == 11L }
        assertTrue(image11?.isMain == true)
        assertTrue(image10?.isMain == false)

        // Switch main image to 10
        dao.setMainImage(1L, 10L)
        val detailsNew = dao.getSpotDetails(1L).first()
        assertNotNull(detailsNew)
        val image10New = detailsNew.images.find { it.id == 10L }
        val image11New = detailsNew.images.find { it.id == 11L }
        assertTrue(image10New?.isMain == true)
        assertTrue(image11New?.isMain == false)
    }

    @Test
    fun getSpotIdForNoteAndImageLookups() = runTest {
        val spot = SpotEntity(
            id = 45L,
            latitude = 12.34,
            longitude = 56.78,
            createdAt = 1000L,
            description = "Spot 45",
            tags = emptyList(),
            category = "graffiti",
            status = "active"
        )
        dao.insertSpot(spot)

        dao.insertImage(
            SpotImageEntity(
                id = 101L,
                spotId = 45L,
                imagePath = "/path/101.png",
                timestamp = 1000L
            )
        )
        dao.insertNote(
            SpotNoteEntity(
                id = 201L,
                spotId = 45L,
                noteText = "A nice note",
                timestamp = 1000L
            )
        )

        assertEquals(45L, dao.getSpotIdForNote(201L))
        assertEquals(45L, dao.getSpotIdForImage(101L))
        assertNull(dao.getSpotIdForNote(999L))
        assertNull(dao.getSpotIdForImage(999L))
    }
}
