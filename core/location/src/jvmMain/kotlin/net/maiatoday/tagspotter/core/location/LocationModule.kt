package net.maiatoday.tagspotter.core.location

import net.maiatoday.tagspotter.core.settings.SettingsRepository
import org.koin.core.module.Module
import org.koin.dsl.module

actual val locationModule: Module = module {
    single<LocationProvider> { JvmLocationProvider(getOrNull<SettingsRepository>()) }
    single<WearSyncManager> { NoOpWearSyncManager() }
}
