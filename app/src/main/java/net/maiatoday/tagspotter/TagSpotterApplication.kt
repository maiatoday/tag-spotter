package net.maiatoday.tagspotter

import android.app.Application
import net.maiatoday.tagspotter.data.LocalSpotRepository
import net.maiatoday.tagspotter.data.SharedPreferencesSettingsRepository
import net.maiatoday.tagspotter.data.SpotDatabase
import net.maiatoday.tagspotter.data.service.AndroidGeofenceService
import net.maiatoday.tagspotter.di.appModule
import net.maiatoday.tagspotter.domain.GeofenceService
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.osmdroid.config.Configuration

open class TagSpotterApplication : Application() {

    // Database and repository singletons for dependency injection
    open val database by lazy { SpotDatabase.getDatabase(this) }
    open val geofenceService: GeofenceService by lazy { AndroidGeofenceService(this) }
    open val repository by lazy { LocalSpotRepository(database.spotDao(), geofenceService) }
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
