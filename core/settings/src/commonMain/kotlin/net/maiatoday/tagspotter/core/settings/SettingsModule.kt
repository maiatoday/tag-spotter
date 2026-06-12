package net.maiatoday.tagspotter.core.settings

import org.koin.core.module.Module
import org.koin.dsl.module

val coreSettingsModule = module {
    single { FilterManager() }
    includes(platformSettingsModule)
}

expect val platformSettingsModule: Module
