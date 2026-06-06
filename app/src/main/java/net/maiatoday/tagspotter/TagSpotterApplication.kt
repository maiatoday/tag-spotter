package net.maiatoday.tagspotter

import android.app.Application
import net.maiatoday.tagspotter.core.database.LocalSpotRepository
import net.maiatoday.tagspotter.core.settings.DataStoreSettingsRepository
import net.maiatoday.tagspotter.core.settings.SettingsRepository
import net.maiatoday.tagspotter.core.database.SpotDatabase
import net.maiatoday.tagspotter.core.location.AndroidGeofenceService
import net.maiatoday.tagspotter.di.appModule
import net.maiatoday.tagspotter.core.location.GeofenceService
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.osmdroid.config.Configuration

import net.maiatoday.tagspotter.core.photo.AndroidPhotoProcessor

open class TagSpotterApplication : Application() {

    // Database and repository singletons for dependency injection
    open val database by lazy { SpotDatabase.getDatabase(this) }
    open val geofenceService: GeofenceService by lazy { AndroidGeofenceService(this) }
    open val repository by lazy { LocalSpotRepository(database.spotDao(), geofenceService, AndroidPhotoProcessor(this)) }
    open val settingsRepository: SettingsRepository by lazy { DataStoreSettingsRepository(this) }

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
