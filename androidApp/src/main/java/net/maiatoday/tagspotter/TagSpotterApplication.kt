package net.maiatoday.tagspotter

import android.app.Application
import net.maiatoday.tagspotter.core.database.LocalSpotRepository
import net.maiatoday.tagspotter.core.settings.SettingsRepository
import net.maiatoday.tagspotter.core.database.SpotDatabase
import net.maiatoday.tagspotter.core.database.getDatabaseBuilder
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import net.maiatoday.tagspotter.di.secretsModule
import net.maiatoday.tagspotter.di.androidAiModule
import com.google.firebase.Firebase
import com.google.firebase.initialize
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.adk.kt.runners.Runner
import net.maiatoday.tagspotter.core.agent.createAndroidTagAlongRunner
import net.maiatoday.tagspotter.core.location.LocationProvider
import net.maiatoday.tagspotter.feature.main.initKoin
import net.maiatoday.tagspotter.feature.tagalong.di.tagalongModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.dsl.module
import org.osmdroid.config.Configuration

import net.maiatoday.tagspotter.core.photo.AndroidPhotoProcessor

val androidAgentModule = module {
    single<Runner> {
        createAndroidTagAlongRunner(
            hybridSpotRepository = get(),
            locationProvider = getOrNull<LocationProvider>()
        )
    }
}

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
        
        // Initialize Firebase and App Check
        Firebase.initialize(this)
        val appCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            if (BuildConfig.APP_CHECK_DEBUG_TOKEN.isNotEmpty()) {
                System.setProperty("com.google.firebase.appcheck.debug.store.token", BuildConfig.APP_CHECK_DEBUG_TOKEN)
            }
            appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            appCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
        
        initKoin(listOf(secretsModule, androidAiModule, androidAgentModule, tagalongModule)).apply {
            androidContext(this@TagSpotterApplication)
        }
        
        // OSMDroid requires a custom user-agent value to load map tiles
        Configuration.getInstance().userAgentValue = packageName
    }
}
