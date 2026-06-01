package com.example.tagspotter

import androidx.room.Room
import com.example.tagspotter.data.LocalSpotRepository
import com.example.tagspotter.data.SpotDatabase

class TestTagSpotterApplication : TagSpotterApplication() {
    private val inMemoryDatabase by lazy {
        Room.inMemoryDatabaseBuilder(this, SpotDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }
    
    override val database: SpotDatabase
        get() = inMemoryDatabase

    override val repository: LocalSpotRepository by lazy {
        LocalSpotRepository(inMemoryDatabase.spotDao())
    }

    override fun onCreate() {
        super.onCreate()
        kotlinx.coroutines.runBlocking {
            repository.loadTestData()
        }
    }
}
