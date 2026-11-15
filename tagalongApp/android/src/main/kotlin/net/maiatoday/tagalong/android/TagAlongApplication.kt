package net.maiatoday.tagalong.android

import android.app.Application
import com.google.adk.kt.runners.Runner
import com.google.firebase.Firebase
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize
import net.maiatoday.tagspotter.core.agent.createAndroidTagAlongRunner
import net.maiatoday.tagspotter.core.database.databaseModule
import net.maiatoday.tagspotter.core.location.LocationProvider
import net.maiatoday.tagspotter.core.location.locationModule
import net.maiatoday.tagspotter.core.photo.photoModule
import net.maiatoday.tagspotter.core.settings.coreSettingsModule
import net.maiatoday.tagspotter.core.sync.syncModule
import net.maiatoday.tagspotter.feature.tagalong.di.tagalongModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class TagAlongApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        var isFirebaseReady = false
        try {
            Firebase.initialize(this)
            val appCheck = FirebaseAppCheck.getInstance()
            appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            isFirebaseReady = true
        } catch (e: Exception) {
            println("TagAlongApplication: Firebase AppCheck initialization warning: ${e.message}")
        }

        val androidAgentModule = module {
            if (isFirebaseReady) {
                single<Runner> {
                    createAndroidTagAlongRunner(
                        hybridSpotRepository = get(),
                        locationProvider = getOrNull<LocationProvider>()
                    )
                }
            }
        }

        startKoin {
            androidContext(this@TagAlongApplication)
            modules(
                listOf(
                    databaseModule,
                    locationModule,
                    photoModule,
                    coreSettingsModule,
                    syncModule,
                    androidAgentModule,
                    tagalongModule
                )
            )
        }
    }
}
