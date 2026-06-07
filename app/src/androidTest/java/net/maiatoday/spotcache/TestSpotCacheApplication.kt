package net.maiatoday.spotcache

import androidx.room.Room
import net.maiatoday.spotcache.core.database.LocalSpotRepository
import net.maiatoday.spotcache.core.photo.AndroidPhotoProcessor
import net.maiatoday.spotcache.core.database.SpotDatabase

class TestSpotCacheApplication : SpotCacheApplication() {
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
