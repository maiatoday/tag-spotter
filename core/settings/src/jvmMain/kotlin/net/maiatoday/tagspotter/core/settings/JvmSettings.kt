package net.maiatoday.tagspotter.core.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.russhwolf.settings.PreferencesSettings
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.prefs.Preferences as JPreferences

internal fun createDataStore(): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            val userHome = System.getProperty("user.home")
            "$userHome/.tag_spotter_settings.preferences_pb".toPath()
        }
    )

actual val platformSettingsModule: Module = module {
    includes(nonWebSettingsModule)
    single<SecureStorage> { SettingsSecureStorage(PreferencesSettings(JPreferences.userRoot().node("net.maiatoday.tagspotter.core.settings"))) }
    single { createDataStore() }
}
