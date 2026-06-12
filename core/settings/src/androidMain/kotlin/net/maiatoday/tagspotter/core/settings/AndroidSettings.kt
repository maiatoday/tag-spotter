package net.maiatoday.tagspotter.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

internal fun createDataStore(context: Context): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { context.filesDir.resolve("tag_spotter_settings.preferences_pb").absolutePath.toPath() }
    )

actual val platformSettingsModule: Module = module {
    includes(nonWebSettingsModule)
    single<SecureStorage> { AndroidSecureStorage(androidContext()) }
    single { createDataStore(androidContext()) }
}
