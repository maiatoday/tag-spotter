package net.maiatoday.tagspotter

import android.app.Application
import net.maiatoday.tagspotter.core.database.LocalSpotRepository
import net.maiatoday.tagspotter.core.settings.SettingsRepository
import net.maiatoday.tagspotter.core.database.SpotDatabase
import net.maiatoday.tagspotter.core.database.getDatabaseBuilder
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import net.maiatoday.tagspotter.di.secretsModule
import net.maiatoday.tagspotter.feature.main.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.osmdroid.config.Configuration

import net.maiatoday.tagspotter.core.photo.AndroidPhotoProcessor

open class TagSpotterApplication : Application() {

    // Database and repository singletons for dependency injection
    open val database: SpotDatabase by lazy { 
        getDatabaseBuilder(this)
            .setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigration(true)
            .build() 
    }
    open val repository: LocalSpotRepository by lazy { LocalSpotRepository(database.spotDao(), AndroidPhotoProcessor(this)) }
    open val settingsRepository: SettingsRepository by lazy { 
        GlobalContext.get().get()
    }

    override fun onCreate() {
        super.onCreate()
        
        initKoin(listOf(secretsModule)).apply {
            androidContext(this@TagSpotterApplication)
        }
        
        // OSMDroid requires a custom user-agent value to load map tiles
        Configuration.getInstance().userAgentValue = packageName
    }
}
