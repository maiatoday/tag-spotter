package net.maiatoday.tagspotter

import android.app.Application
import net.maiatoday.tagspotter.core.database.SpotDatabase
import net.maiatoday.tagspotter.core.database.SpotRepository
import net.maiatoday.tagspotter.core.settings.SettingsRepository
import net.maiatoday.tagspotter.di.secretsModule
import net.maiatoday.tagspotter.di.androidAiModule
import com.google.firebase.Firebase
import com.google.firebase.initialize
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.adk.kt.runners.Runner
import net.maiatoday.tagspotter.core.a2ui.engine.getJetpackHybridCatalogSchemaJson
import net.maiatoday.tagspotter.core.agent.createAndroidTagAlongRunner
import net.maiatoday.tagspotter.core.location.LocationProvider
import net.maiatoday.tagspotter.feature.main.initKoin
import net.maiatoday.tagspotter.feature.tagalong.di.tagalongModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.dsl.module
import org.osmdroid.config.Configuration

val androidAgentModule = module {
    single<Runner> {
        createAndroidTagAlongRunner(
            hybridSpotRepository = get(),
            locationProvider = getOrNull<LocationProvider>(),
            catalogSchemaJson = getJetpackHybridCatalogSchemaJson(context = androidContext())
        )
    }
}

open class TagSpotterApplication : Application() {

    // Database and repository singletons delegated to Koin so ImportPackWorker,
    // Gallery, and TagAlong share the exact same SpotDatabase instance.
    open val database: SpotDatabase
        get() = GlobalContext.get().get()
    open val repository: SpotRepository
        get() = GlobalContext.get().get()
    open val settingsRepository: SettingsRepository
        get() = GlobalContext.get().get()

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
