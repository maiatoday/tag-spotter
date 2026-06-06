package net.maiatoday.tagspotter

import android.app.Application
import net.maiatoday.tagspotter.data.LocalSpotRepository
import net.maiatoday.tagspotter.data.SpotDatabase
import net.maiatoday.tagspotter.data.SharedPreferencesSettingsRepository
import net.maiatoday.tagspotter.domain.LocationProvider
import net.maiatoday.tagspotter.domain.PhotoProcessor
import net.maiatoday.tagspotter.data.service.AndroidLocationProvider
import net.maiatoday.tagspotter.data.service.AndroidPhotoProcessor
import net.maiatoday.tagspotter.domain.GeofenceService
import net.maiatoday.tagspotter.data.service.AndroidGeofenceService
import org.osmdroid.config.Configuration
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import net.maiatoday.tagspotter.di.appModule

open class TagSpotterApplication : Application() {

    // Database and repository singletons for dependency injection
    open val database by lazy { SpotDatabase.getDatabase(this) }
    open val geofenceService: GeofenceService by lazy { AndroidGeofenceService(this) }
    open val repository by lazy { LocalSpotRepository(this, database.spotDao(), geofenceService) }
    open val settingsRepository by lazy { SharedPreferencesSettingsRepository(this) }
    
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidContext(this@TagSpotterApplication)
            modules(appModule)
        }
        
        // OSMDroid requires a custom user-agent value to load map tiles
        Configuration.getInstance().userAgentValue = packageName
    }
}
