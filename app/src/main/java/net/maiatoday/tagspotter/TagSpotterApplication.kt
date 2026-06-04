package net.maiatoday.tagspotter

import android.app.Application
import net.maiatoday.tagspotter.data.LocalSpotRepository
import net.maiatoday.tagspotter.data.SpotDatabase
import net.maiatoday.tagspotter.data.SharedPreferencesSettingsRepository
import org.osmdroid.config.Configuration

open class TagSpotterApplication : Application() {

    // Database and repository singletons for dependency injection
    open val database by lazy { SpotDatabase.getDatabase(this) }
    open val repository by lazy { LocalSpotRepository(this, database.spotDao()) }
    open val settingsRepository by lazy { SharedPreferencesSettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        
        // OSMDroid requires a custom user-agent value to load map tiles
        Configuration.getInstance().userAgentValue = packageName
    }
}
