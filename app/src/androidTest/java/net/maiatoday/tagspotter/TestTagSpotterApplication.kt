package net.maiatoday.tagspotter

import androidx.room.Room
import net.maiatoday.tagspotter.core.database.LocalSpotRepository
import net.maiatoday.tagspotter.core.location.GeofenceService
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
        LocalSpotRepository(inMemoryDatabase.spotDao(), object : GeofenceService {
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
