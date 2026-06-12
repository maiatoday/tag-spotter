@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    com.russhwolf.settings.ExperimentalSettingsImplementation::class
)

package net.maiatoday.tagspotter.core.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.russhwolf.settings.KeychainSettings
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

internal fun createDataStore(): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = false,
                error = null
            )
            (requireNotNull(documentDirectory?.path) + "/tag_spotter_settings.preferences_pb").toPath()
        }
    )

actual val platformSettingsModule: Module = module {
    includes(nonWebSettingsModule)
    single<SecureStorage> { SettingsSecureStorage(KeychainSettings()) }
    single { createDataStore() }
}
