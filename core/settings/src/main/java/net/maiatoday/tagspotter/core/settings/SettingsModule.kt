package net.maiatoday.tagspotter.core.settings

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreSettingsModule = module {
    single<SecureStorage> { AndroidSecureStorage(androidContext()) }
    single<SettingsRepository> { DataStoreSettingsRepository(androidContext(), get()) }
    single { FilterManager() }
}
