package com.example.tagspotter

import android.app.Application
import com.example.tagspotter.data.LocalSpotRepository
import com.example.tagspotter.data.SpotDatabase
import org.osmdroid.config.Configuration

class TagSpotterApplication : Application() {

    // Database and repository singletons for dependency injection
    val database by lazy { SpotDatabase.getDatabase(this) }
    val repository by lazy { LocalSpotRepository(database.spotDao()) }

    override fun onCreate() {
        super.onCreate()
        
        // OSMDroid requires a custom user-agent value to load map tiles
        Configuration.getInstance().userAgentValue = packageName
    }
}
