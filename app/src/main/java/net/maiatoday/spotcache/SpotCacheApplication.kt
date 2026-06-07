package net.maiatoday.spotcache

import android.app.Application
import net.maiatoday.spotcache.core.database.LocalSpotRepository
import net.maiatoday.spotcache.core.settings.DataStoreSettingsRepository
import net.maiatoday.spotcache.core.settings.SettingsRepository
import net.maiatoday.spotcache.core.database.SpotDatabase
import net.maiatoday.spotcache.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.osmdroid.config.Configuration

import net.maiatoday.spotcache.core.photo.AndroidPhotoProcessor

open class SpotCacheApplication : Application() {

    // Database and repository singletons for dependency injection
    open val database by lazy { SpotDatabase.getDatabase(this) }
    open val repository by lazy { LocalSpotRepository(database.spotDao(), AndroidPhotoProcessor(this)) }
    open val settingsRepository: SettingsRepository by lazy { DataStoreSettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidContext(this@SpotCacheApplication)
            modules(appModule)
        }
        
        // OSMDroid requires a custom user-agent value to load map tiles
        Configuration.getInstance().userAgentValue = packageName
    }
}
