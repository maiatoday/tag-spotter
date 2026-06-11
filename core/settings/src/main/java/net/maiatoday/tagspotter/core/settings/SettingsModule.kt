package net.maiatoday.tagspotter.core.settings

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreSettingsModule = module {
    single<SettingsRepository> { DataStoreSettingsRepository(androidContext()) }
    single { FilterManager() }
}
