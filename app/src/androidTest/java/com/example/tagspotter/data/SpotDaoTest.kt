package com.example.tagspotter.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
}
