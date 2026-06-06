package net.maiatoday.tagspotter

import androidx.room.Room
import net.maiatoday.tagspotter.data.LocalSpotRepository
import net.maiatoday.tagspotter.domain.GeofenceService
import net.maiatoday.tagspotter.data.SpotDatabase

class TestTagSpotterApplication : TagSpotterApplication() {
    private val inMemoryDatabase by lazy {
        Room.inMemoryDatabaseBuilder(this, SpotDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }
    
    override val database: SpotDatabase
        get() = inMemoryDatabase

    override val repository: LocalSpotRepository by lazy {
        LocalSpotRepository(this, inMemoryDatabase.spotDao(), object : GeofenceService {
            override fun registerGeofence(id: Long, latitude: Double, longitude: Double, onResult: (Boolean) -> Unit) {
                onResult(true)
            }
            override fun unregisterGeofence(id: Long) {}
        })
    }

    override fun onCreate() {
        super.onCreate()
        kotlinx.coroutines.runBlocking {
            repository.loadTestData()
        }
    }
}
