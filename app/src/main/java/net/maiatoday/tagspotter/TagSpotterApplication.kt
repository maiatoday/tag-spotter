package net.maiatoday.tagspotter

import android.app.Application
import net.maiatoday.tagspotter.data.LocalSpotRepository
import net.maiatoday.tagspotter.data.SpotDatabase
import net.maiatoday.tagspotter.data.SharedPreferencesSettingsRepository
import net.maiatoday.tagspotter.domain.LocationProvider
import net.maiatoday.tagspotter.domain.PhotoProcessor
import net.maiatoday.tagspotter.data.service.AndroidLocationProvider
import net.maiatoday.tagspotter.data.service.AndroidPhotoProcessor
import org.osmdroid.config.Configuration

open class TagSpotterApplication : Application() {

    // Database and repository singletons for dependency injection
    open val database by lazy { SpotDatabase.getDatabase(this) }
    open val repository by lazy { LocalSpotRepository(this, database.spotDao()) }
    open val settingsRepository by lazy { SharedPreferencesSettingsRepository(this) }
    
    // Services
    open val locationProvider: LocationProvider by lazy { AndroidLocationProvider(this) }
    open val photoProcessor: PhotoProcessor by lazy { AndroidPhotoProcessor(this) }

    override fun onCreate() {
        super.onCreate()
        
        // OSMDroid requires a custom user-agent value to load map tiles
        Configuration.getInstance().userAgentValue = packageName
    }
}
