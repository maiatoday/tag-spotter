package net.maiatoday.tagspotter.core.settings

import org.koin.core.module.Module
import org.koin.dsl.module

val nonWebSettingsModule: Module = module {
    single<SettingsRepository> { DataStoreSettingsRepository(get(), get()) }
}
