package com.example.tagspotter

import android.app.Application
import com.example.tagspotter.data.LocalSpotRepository
import com.example.tagspotter.data.SpotDatabase
import com.example.tagspotter.data.SharedPreferencesSettingsRepository
import org.osmdroid.config.Configuration

open class TagSpotterApplication : Application() {

    // Database and repository singletons for dependency injection
    open val database by lazy { SpotDatabase.getDatabase(this) }
    open val repository by lazy { LocalSpotRepository(database.spotDao()) }
    open val settingsRepository by lazy { SharedPreferencesSettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        
        // OSMDroid requires a custom user-agent value to load map tiles
        Configuration.getInstance().userAgentValue = packageName
    }
}
