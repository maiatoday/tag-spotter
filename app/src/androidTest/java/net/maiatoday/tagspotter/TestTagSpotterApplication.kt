package net.maiatoday.tagspotter

import androidx.room.Room
import net.maiatoday.tagspotter.core.database.LocalSpotRepository
import net.maiatoday.tagspotter.core.photo.AndroidPhotoProcessor
import net.maiatoday.tagspotter.core.database.SpotDatabase

class TestTagSpotterApplication : TagSpotterApplication() {
    private val inMemoryDatabase by lazy {
        Room.inMemoryDatabaseBuilder(this, SpotDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }
    
    override val database: SpotDatabase
        get() = inMemoryDatabase

    override val repository: LocalSpotRepository by lazy {
        LocalSpotRepository(inMemoryDatabase.spotDao(), AndroidPhotoProcessor(this))
    }

    override fun onCreate() {
        super.onCreate()
        kotlinx.coroutines.runBlocking {
            repository.loadTestData()
        }
    }
}
